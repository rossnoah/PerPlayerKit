#!/usr/bin/env python3
"""Verify remembered selections across server restarts and SQLite-to-YAML migration."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET
import zipfile
import yaml

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def boot(folder, server_jar, java, phase, label=None):
    (folder / 'selection-phase.txt').write_text(phase)
    log_path = folder / ((label or phase) + '.log')
    with log_path.open('w') as log:
        process = subprocess.Popen([java, '-Xms512M', '-Xmx1536M', '-jar', str(server_jar), 'nogui'],
                                   cwd=folder, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            deadline = time.monotonic() + 180
            while time.monotonic() < deadline:
                text = log_path.read_text(errors='replace')
                if process.poll() is not None or 'PPK_SELECTION_FAIL' in text:
                    raise RuntimeError('Probe failed: ' + str(log_path))
                if 'PPK_SELECTION_PASS ' + phase in text and 'Done (' in text:
                    break
                time.sleep(.25)
            else:
                raise RuntimeError('Probe timed out: ' + str(log_path))
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
    print('PASS', folder.name, label or phase, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-jar', type=Path, required=True)
    parser.add_argument('--paper-cache', type=Path)
    parser.add_argument('--java', default='java')
    parser.add_argument('--port', type=int, default=27100)
    args = parser.parse_args()
    version = ET.parse(ROOT / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
    candidate = ROOT / 'target' / f'PerPlayerKit-{version}.jar'
    reports = sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))
    properties = ET.parse(reports[0]).getroot().find('properties')
    classpath = next(prop.get('value') for prop in properties if prop.get('name') == 'java.class.path')
    base = Path(tempfile.mkdtemp(prefix='selections-', dir=ROOT / 'target'))
    print('Fixture:', base, flush=True)
    classes = base / 'classes'; classes.mkdir()
    subprocess.run(['javac', '--release', '17', '-cp', classpath, '-d', str(classes), str(HERE / 'SelectionProbe.java')], check=True)
    probe = base / 'SelectionProbe.jar'
    with zipfile.ZipFile(probe, 'w') as jar:
        for file in classes.rglob('*.class'):
            jar.write(file, file.relative_to(classes))
        jar.writestr('plugin.yml', 'name: SelectionProbe\nversion: 1\nmain: ppk.selectionprobe.SelectionProbe\napi-version: 1.19\ndepend: [PerPlayerKit]\n')
    (base / 'inputs.json').write_text(json.dumps({str(p): hashlib.sha256(p.read_bytes()).hexdigest()
                                              for p in [candidate, args.server_jar]}, indent=2))
    for backend in ['sqlite', 'yaml']:
        folder = base / backend; data = folder / 'plugins/PerPlayerKit'; data.mkdir(parents=True)
        for file in [candidate, probe]:
            shutil.copy2(file, folder / 'plugins' / file.name)
        if args.paper_cache:
            (folder / 'cache').symlink_to(args.paper_cache.resolve(), target_is_directory=True)
        config = {'config-version': 3, 'storage': {'type': backend, 'backup': {'enabled': False}},
                  'publickits': {'persisted': {'name': 'Persisted kit', 'icon': 'STONE'}},
                  'regear': {'invert-whitelist': True, 'whitelist': []}, 'motd': {'enabled': False}}
        (data / 'config.yml').write_text(yaml.safe_dump(config, sort_keys=False))
        (folder / 'eula.txt').write_text('eula=true\n')
        (folder / 'server.properties').write_text(f'online-mode=false\nserver-ip=127.0.0.1\nserver-port={args.port}\nview-distance=2\nsimulation-distance=2\ngenerate-structures=false\n')
        boot(folder, args.server_jar.resolve(), args.java, 'seed')
        boot(folder, args.server_jar.resolve(), args.java, 'verify')
        if backend == 'sqlite':
            # The seed copied the full database to YAML through the real storage migrator.
            config['storage']['type'] = 'yaml'
            (data / 'config.yml').write_text(yaml.safe_dump(config, sort_keys=False))
            boot(folder, args.server_jar.resolve(), args.java, 'verify', 'verify-migrated-yaml')


if __name__ == '__main__':
    main()
