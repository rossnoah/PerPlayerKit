# Populated upgrade and rollback test

Runs four real server boots: the old plugin seeds inventories, the new plugin
loads them, the new plugin restarts and saves a change, then the old plugin
loads that change after configuration rollback.

The fixture includes a named enchanted sword, armor/off-hand items, a shulker
with nested metadata, a high-numbered player slot, a custom public kit, an
unassigned definition, and a deliberately empty saved kit room page. It checks
exact stored data, inventory decoding, notification permissions, custom settings,
empty announcements, backups, and repeated migration.

## Run

Needs Java, Maven, Python 3 with PyYAML, and a cached Paper server jar. Build the
new plugin with `mvn package` first; the probe also uses its test classpath.

```bash
python3 tools/integration-test/upgrade/run.py \
  --old-jar /path/to/old/PerPlayerKit.jar \
  --old-config-version 2 \
  --backend sqlite \
  --server-jar tools/integration-test/.cache/paper-1.21.8.jar \
  --paper-cache tools/integration-test/.cache/paperclip/1.21.8
```

`--backend yaml` covers the other file backend. External backends take
`--connection /path/to/connection.json` with the old connection fields (`host`,
`port`, `dbname`, `username`, `password`, `useSSL` as applicable).

Use a **dedicated empty test database**. The probe refuses to seed an existing
kit database. The fixture server binds to localhost; `--port` changes its port.
All server files and phase logs remain under `target/upgrade-test/`.

`--legacy-storage-type MySQL --backend sqlite` verifies that the old
case-sensitive selector's SQLite fallback survives migration.

## Historical baselines

The checked-in configs come from these source revisions:

| Config | Revision | Supported storage backends |
| --- | --- | --- |
| v1 | `df1040e` | SQLite, YAML, MySQL, Redis |
| v2 | `12c959b` | SQLite, YAML, MySQL, Redis, PostgreSQL |

Build the matching old jar in a separate directory with `git archive` and Maven.
The older builds' own default merger corrupts the old boolean rekit-on-kill
form, so live fixtures use the supported section form. Unit tests separately
verify migration of the boolean form without first running that old merger.

Rollback restores the config backup produced by the migrator and restores
the original language files. It deliberately keeps the database after 1.8
saved a kit, proving that the old plugin can read the new save. Minecraft
stays on the same version throughout; this does not test Minecraft downgrades.

The probe is compiled into a temporary companion plugin and never packaged
into PerPlayerKit's production jar. `result.json` records both jar hashes and
the phases that passed.
