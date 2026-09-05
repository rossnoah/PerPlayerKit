#!/usr/bin/env python3
"""Check real item metadata and kit room paging with a connected test client."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-jar', type=Path, required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--paper-cache', type=Path)
    parser.add_argument('--java', default='java')
    parser.add_argument('--port', type=int, default=27200)
    args = parser.parse_args()
    version = ET.parse(ROOT / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
    candidate = ROOT / 'target' / f'PerPlayerKit-{version}.jar'
    properties = ET.parse(sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))[0]).getroot().find('properties')
    dependencies = next(p.get('value') for p in properties if p.get('name') == 'java.class.path')
    classpath = str(candidate) + os.pathsep + dependencies
    folder = Path(tempfile.mkdtemp(prefix='features-', dir=ROOT / 'target'))
    print('Fixture:', folder, flush=True)
    classes = folder / 'classes'; classes.mkdir()
    subprocess.run(['javac', '--release', '17', '-cp', classpath, '-d', str(classes), str(HERE / 'FeatureProbe.java')], check=True)
    plugins = folder / 'plugins'; data = plugins / 'PerPlayerKit'; data.mkdir(parents=True)
    with zipfile.ZipFile(plugins / 'FeatureProbe.jar', 'w') as jar:
        for file in classes.rglob('*.class'):
            jar.write(file, file.relative_to(classes))
        jar.writestr('plugin.yml', 'name: FeatureProbe\nversion: 1\nmain: ppk.featureprobe.FeatureProbe\napi-version: 1.19\ndepend: [PerPlayerKit]\n')
    shutil.copy2(candidate, plugins / candidate.name)
    (folder / 'inputs.json').write_text(json.dumps({str(p): hashlib.sha256(p.read_bytes()).hexdigest()
                                                for p in [candidate, args.server_jar]}, indent=2))
    (data / 'config.yml').write_text('config-version: 3\nkitroom:\n  pages: 8\nmotd:\n  enabled: false\nupdates:\n  notify-admins-on-join: false\nbroadcasts:\n  scheduled:\n    enabled: false\n')
    if args.paper_cache:
        (folder / 'cache').symlink_to(args.paper_cache.resolve(), target_is_directory=True)
    (folder / 'eula.txt').write_text('eula=true\n')
    (folder / 'server.properties').write_text(f'online-mode=false\nenforce-secure-profile=false\nserver-ip=127.0.0.1\nserver-port={args.port}\nview-distance=2\nsimulation-distance=2\ngenerate-structures=false\n')
    log_path = folder / 'server.log'
    with log_path.open('w') as log:
        process = subprocess.Popen([args.java, '-Xms512M', '-Xmx1536M', '-jar', str(args.server_jar.resolve()), 'nogui'],
                                   cwd=folder, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            deadline = time.monotonic() + 180
            while time.monotonic() < deadline:
                text = log_path.read_text(errors='replace')
                if 'PPK_FEATURE_FAIL' in text or process.poll() is not None:
                    raise RuntimeError('Probe failed: ' + str(log_path))
                if 'PPK_FEATURE_READY' in text and 'Done (' in text:
                    break
                time.sleep(.25)
            else:
                raise RuntimeError('Server/probe timeout: ' + str(log_path))
            with (folder / 'client-errors.log').open('w') as errors:
                client = subprocess.run(['node', str(HERE.parent / 'chat-probe.js'), '--port', str(args.port), '--version', args.version,
                                         '--username', 'FeatureProbe', '--await', 'PPK_FEATURE_CLIENT_PASS', '--seconds', '40'],
                                        stdout=subprocess.PIPE, stderr=errors, text=True, timeout=60)
            (folder / 'client.json').write_text(client.stdout)
            report = json.loads(client.stdout)
            if client.returncode != 0 or not any('PPK_FEATURE_CLIENT_PASS' in message['text'] for message in report.get('messages', [])):
                raise RuntimeError('Client/menu checks failed: ' + str(folder))
            if 'PPK_FEATURE_FAIL' in log_path.read_text():
                raise RuntimeError('Probe failed: ' + str(log_path))
            process.stdin.write('stop\n'); process.stdin.flush()
            if process.wait(timeout=120) != 0:
                raise RuntimeError('Unclean shutdown: ' + str(log_path))
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill(); process.wait()
    print('PASS item filters and kit room paging:', folder, flush=True)


if __name__ == '__main__':
    main()
