#!/usr/bin/env python3
"""Verify real WorldGuard queries, then boot without WorldGuard, in isolated temporary servers."""
import argparse
import json
import hashlib
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
    parser.add_argument('--worldguard', type=Path, required=True)
    parser.add_argument('--worldedit', type=Path, required=True)
    parser.add_argument('--paper-cache', type=Path)
    parser.add_argument('--java', default='java')
    parser.add_argument('--port', type=int, default=26300)
    args = parser.parse_args()
    version = ET.parse(ROOT / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
    candidate = ROOT / 'target' / f'PerPlayerKit-{version}.jar'
    reports = sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))
    properties = ET.parse(reports[0]).getroot().find('properties')
    classpath = next(prop.get('value') for prop in properties if prop.get('name') == 'java.class.path')
    base = Path(tempfile.mkdtemp(prefix='regions-', dir=ROOT / 'target'))
    print('Fixture:', base, flush=True)
    classes = base / 'classes'
    classes.mkdir()
    subprocess.run(['javac', '--release', '17', '-cp', classpath, '-d', str(classes), str(HERE / 'RegionProbe.java')], check=True)
    probe = base / 'RegionProbe.jar'
    with zipfile.ZipFile(probe, 'w') as out:
        for file in classes.rglob('*.class'):
            out.write(file, file.relative_to(classes))
        out.writestr('plugin.yml', 'name: RegionProbe\nversion: 1\nmain: ppk.regionprobe.RegionProbe\napi-version: 1.19\ndepend: [PerPlayerKit]\nsoftdepend: [WorldGuard]\n')
    inputs = [args.server_jar, args.worldguard, args.worldedit, candidate]
    (base / 'inputs.json').write_text(json.dumps({str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}, indent=2))
    for enabled in [True, False]:
        server = base / ('with-worldguard' if enabled else 'without-worldguard')
        (server / 'plugins').mkdir(parents=True)
        for file in [candidate, probe] + ([args.worldguard, args.worldedit] if enabled else []):
            shutil.copy2(file, server / 'plugins' / file.name)
        if args.paper_cache:
            (server / 'cache').symlink_to(args.paper_cache.resolve(), target_is_directory=True)
        (server / 'eula.txt').write_text('eula=true\n')
        (server / 'expect-worldguard.txt').write_text(str(enabled).lower())
        (server / 'server.properties').write_text(f'online-mode=false\nserver-ip=127.0.0.1\nserver-port={args.port}\nview-distance=2\nsimulation-distance=2\ngenerate-structures=false\n')
        log_path = server / 'probe.log'
        with log_path.open('w') as log:
            process = subprocess.Popen([args.java, '-Xms512M', '-Xmx1536M', '-jar', str(args.server_jar.resolve()), 'nogui'],
                                       cwd=server, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
            try:
                deadline = time.monotonic() + 180
                while time.monotonic() < deadline:
                    output = log_path.read_text(errors='replace')
                    if 'PPK_REGIONS_FAIL' in output or process.poll() is not None:
                        raise RuntimeError('Probe failed; see ' + str(log_path))
                    if 'PPK_REGIONS_PASS' in output and 'Done (' in output:
                        break
                    time.sleep(.25)
                else:
                    raise RuntimeError('Probe timed out; see ' + str(log_path))
                process.stdin.write('stop\n'); process.stdin.flush(); process.wait(timeout=120)
                if process.returncode != 0:
                    raise RuntimeError('Unclean shutdown; see ' + str(log_path))
            finally:
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        process.kill(); process.wait()
        print('PASS', server.name, flush=True)


if __name__ == '__main__':
    main()
