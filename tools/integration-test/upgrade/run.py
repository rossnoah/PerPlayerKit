#!/usr/bin/env python3
"""Exercise an old plugin -> v3 -> restart/save -> rollback with real stored inventories.

Uses a dedicated throwaway server and an EMPTY test database. Never point this at
production storage. The probe refuses to seed a database that already has kit rows.
"""
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

import yaml

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def compile_probe(destination):
    reports = sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))
    require(reports, 'Run mvn test first to prepare the probe classpath')
    properties = ET.parse(reports[0]).getroot().find('properties')
    classpath = next(prop.get('value') for prop in properties if prop.get('name') == 'java.class.path')
    classes = destination / 'probe-classes'
    classes.mkdir()
    subprocess.run(['javac', '--release', '17', '-cp', classpath, '-d', str(classes), str(HERE / 'UpgradeProbe.java')], check=True)
    jar = destination / 'UpgradeProbe.jar'
    with zipfile.ZipFile(jar, 'w') as out:
        for file in classes.rglob('*.class'):
            out.write(file, file.relative_to(classes))
        out.writestr('plugin.yml', 'name: UpgradeProbe\nversion: 1\nmain: ppk.upgradeprobe.UpgradeProbe\napi-version: 1.19\ndepend: [PerPlayerKit]\n')
    return jar


def java_for(major):
    if Path('/usr/libexec/java_home').exists():
        home = subprocess.check_output(['/usr/libexec/java_home', '-v', str(major)], text=True).strip()
        return str(Path(home) / 'bin/java')
    home = os.environ.get('JAVA_HOME')
    return str(Path(home) / 'bin/java') if home else 'java'


def boot(server, server_jar, plugin_jar, phase, java):
    for file in (server / 'plugins').glob('PerPlayerKit-*.jar'):
        file.unlink()
    shutil.copy2(plugin_jar, server / 'plugins/PerPlayerKit-under-test.jar')
    (server / 'upgrade-phase.txt').write_text(phase)
    log_path = server / (phase + '.log')
    with log_path.open('w') as log:
        process = subprocess.Popen([java, '-Xms512M', '-Xmx1536M', '-jar', str(server_jar), 'nogui'], cwd=server,
                                   stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            deadline = time.monotonic() + 180
            while time.monotonic() < deadline:
                output = log_path.read_text(errors='replace')
                require('PPK_UPGRADE_FAIL' not in output, 'Probe failed; see ' + str(log_path))
                require(process.poll() is None, 'Server exited; see ' + str(log_path))
                if 'Done (' in output and 'PPK_UPGRADE_PASS ' + phase in output:
                    break
                time.sleep(.25)
            else:
                raise RuntimeError('Server/probe did not finish; see ' + str(log_path))
            process.stdin.write('stop\n')
            process.stdin.flush()
            process.wait(timeout=120)
            require(process.returncode == 0, 'Unclean shutdown; see ' + str(log_path))
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
    print('PASS', phase, flush=True)


def configuration_snapshot(folder):
    files = {Path('config.yml'): (folder / 'config.yml').read_bytes()}
    if (folder / 'lang').exists():
        files.update({path.relative_to(folder): path.read_bytes() for path in (folder / 'lang').rglob('*') if path.is_file()})
    return files


def validate_migrated(folder, original_version, expected_backend):
    data = yaml.safe_load((folder / 'config.yml').read_text())
    require(data['config-version'] == 3, 'Config did not upgrade to v3')
    require(data['storage']['type'] == expected_backend, 'Upgrade selected a different backend')
    require(data['kits']['max-slots'] == 13, 'Kit slot count changed')
    require(data['rekit']['kill']['enabled'] is True, 'Enabled kill setting changed')
    require(data['rekit']['respawn']['enabled'] is False, 'Disabled respawn setting changed')
    require(data['broadcasts']['actions']['player-loaded-public-kit']['enabled'] is False, 'Disabled kit broadcasts changed')
    require(data['broadcasts']['actions']['player-repaired']['permission'] == 'fixture.notify', 'Custom recipient permission changed')
    require(set(data['publickits']) == {'custom', 'unassigned'}, 'Custom public kit definitions changed')
    require(data['locations']['global'] == {'mode': 'deny', 'entries': ['lobby', 'events']}, 'Disabled world choices changed')
    require(data['locations']['rekit-kill'] == {'mode': 'allow', 'entries': ['world']}, 'Whitelist precedence changed')
    require(data['storage']['backup']['enabled'] is False, 'Disabled backups changed')
    language = yaml.safe_load((folder / 'lang/en.yml').read_text())
    require(language['prefix'] == '', 'Empty prefix restored a default')
    require(language['motd']['message'] == [], 'Empty MOTD restored defaults')
    require(language['scheduled-broadcast']['messages'] == [], 'Empty announcements restored defaults')
    if original_version == 1:
        require(language['broadcast-messages']['player-loaded-public-kit'] == '<gold>{player} used {kitname} (%player_name%)</gold>', 'Legacy placeholders changed incorrectly')
    else:
        require(language['success']['kit-loaded'] == 'Personal translation {slot}', 'Custom language entry changed')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--old-jar', type=Path, required=True)
    version = ET.parse(ROOT / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
    parser.add_argument('--new-jar', type=Path, default=ROOT / 'target' / f'PerPlayerKit-{version}.jar')
    parser.add_argument('--old-config-version', type=int, choices=[1, 2], required=True)
    parser.add_argument('--backend', choices=['sqlite', 'yaml', 'mysql', 'postgresql', 'redis'], required=True)
    parser.add_argument('--legacy-storage-type', help='Optional legacy spelling, such as MySQL, whose effective backend was SQLite')
    parser.add_argument('--connection', type=Path, help='JSON connection settings for a dedicated empty external test database')
    parser.add_argument('--server-jar', type=Path, required=True)
    parser.add_argument('--paper-cache', type=Path, help='Reuse the matching Paper vanilla-jar cache')
    parser.add_argument('--port', type=int, default=26100)
    parser.add_argument('--java-major', type=int, default=21)
    args = parser.parse_args()
    for path in [args.old_jar, args.new_jar, args.server_jar]:
        require(path.is_file(), 'Missing file: ' + str(path))
    require(args.backend not in ['mysql', 'postgresql', 'redis'] or args.connection, 'External backends require --connection')
    require(not (args.old_config_version == 1 and args.backend == 'postgresql'), 'The v1 baseline predates PostgreSQL support')
    base = ROOT / 'target/upgrade-test'
    base.mkdir(exist_ok=True)
    server = Path(tempfile.mkdtemp(prefix=f'v{args.old_config_version}-{args.backend}-', dir=base))
    print('Fixture:', server, flush=True)
    # Keep each run pinned even if another build replaces the caller's jar while tests run.
    old_jar = server / 'baseline.jar'
    new_jar = server / 'candidate.jar'
    shutil.copy2(args.old_jar, old_jar)
    shutil.copy2(args.new_jar, new_jar)
    (server / 'plugins/PerPlayerKit').mkdir(parents=True)
    folder = server / 'plugins/PerPlayerKit'
    shutil.copy2(compile_probe(server), server / 'plugins/UpgradeProbe.jar')
    config = yaml.safe_load((HERE / f'fixtures/v{args.old_config_version}.yml').read_text())
    config['config-version'] = args.old_config_version
    config['storage']['type'] = args.legacy_storage_type or args.backend
    # A selector regression must not contact a database outside the supplied fixture.
    for backend in ['mysql', 'postgresql']:
        config[backend] = {'host': '127.0.0.1', 'port': '1', 'dbname': 'unused_fixture',
                           'username': 'unused_fixture', 'password': 'unused_fixture', 'useSSL': False}
    config['redis'] = {'host': '127.0.0.1', 'port': 1, 'password': 'unused_fixture'}
    if args.connection:
        config[args.backend] = json.loads(args.connection.read_text())
    config['max-kits'] = 13
    config['backup'] = {'enabled': False}
    config['disabled-command-worlds'] = ['lobby', 'events']
    config['publickits'] = {'custom': {'name': 'Owner custom kit', 'icon': 'DIAMOND_SWORD'},
                            'unassigned': {'name': 'Keep this empty', 'icon': 'CHEST'}}
    # The baseline's merger corrupts the boolean form; the unit suite covers that migration separately.
    config['feature']['rekit-on-kill'] = {'enabled': True, 'world-whitelist': ['world'], 'world-blacklist': ['world', 'lobby']}
    config['feature']['rekit-on-respawn'] = False
    config['feature']['broadcast-kit-messages'] = False
    config['messages']['player-repaired'] = {'enabled': False, 'permission': 'fixture.notify'}
    if args.old_config_version == 1:
        config['prefix'] = ''
        config['motd']['message'] = []
        config['scheduled-broadcast']['messages'] = []
        config['messages']['player-loaded-public-kit']['message'] = '<gold>%player% used %kitname% (%player_name%)</gold>'
    else:
        (folder / 'lang').mkdir()
        (folder / 'lang/en.yml').write_text("# Owner language file\nprefix: ''\nmotd:\n  message: []\nscheduled-broadcast:\n  messages: []\nsuccess:\n  kit-loaded: 'Personal translation {slot}'\n")
        (folder / 'lang/custom.yml').write_text("# Inactive custom language: retain exactly\nprefix: '<gold>Custom</gold>'\n")
    (folder / 'config.yml').write_text(yaml.safe_dump(config, sort_keys=False))
    (server / 'eula.txt').write_text('eula=true\n')
    (server / 'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=false\nenforce-secure-profile=false\nlevel-type=flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\n')
    if args.paper_cache:
        (server / 'cache').symlink_to(args.paper_cache.resolve(), target_is_directory=True)
    java = java_for(args.java_major)
    boot(server, args.server_jar.resolve(), old_jar, 'seed', java)
    snapshot = configuration_snapshot(folder)
    boot(server, args.server_jar.resolve(), new_jar, 'verify', java)
    validate_migrated(folder, args.old_config_version, args.backend)
    if args.old_config_version == 2:
        for file, contents in snapshot.items():
            if file.parts[0] == 'lang':
                require((folder / file).read_bytes() == contents, 'V2 language file rewritten: ' + str(file))
    backups = list(folder.glob('config.yml.backup-*.yml'))
    require(len(backups) == 1 and backups[0].read_bytes() == snapshot[Path('config.yml')], 'Config backup does not match the original')
    migrated = (folder / 'config.yml').read_bytes()
    boot(server, args.server_jar.resolve(), new_jar, 'write', java)
    require((folder / 'config.yml').read_bytes() == migrated, 'Repeated startup changed the migrated config')
    # Roll back configuration and language files, keeping the database after the new API wrote to it.
    if (folder / 'lang').exists():
        shutil.rmtree(folder / 'lang')
    for relative, contents in snapshot.items():
        target = folder / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if relative == Path('config.yml'):
            shutil.copy2(backups[0], target)
        else:
            target.write_bytes(contents)
    boot(server, args.server_jar.resolve(), old_jar, 'rollback', java)
    require((folder / 'config.yml').read_bytes() == snapshot[Path('config.yml')], 'Old config changed during rollback startup')
    result = {'old_config_version': args.old_config_version, 'backend': args.backend, 'legacy_storage_type': args.legacy_storage_type,
              'old_jar_sha256': hashlib.sha256(old_jar.read_bytes()).hexdigest(),
              'new_jar_sha256': hashlib.sha256(new_jar.read_bytes()).hexdigest(), 'phases': ['seed', 'verify', 'write', 'rollback'], 'passed': True}
    (server / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print('PASS populated upgrade, restart, and rollback:', server, flush=True)


if __name__ == '__main__':
    main()
