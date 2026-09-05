# Remembered selection checks

Build with `mvn verify`, then run:

```bash
python3 tools/integration-test/selections/run.py --server-jar /path/to/server.jar --java /path/to/java
```

The probe saves personal/public kit selections and enderchest slots through normal loads, then starts a separate server process to restore them. It checks regear and respawn restoration, verifies preferences stay out of item exports, and migrates SQLite to YAML to check both inventories and selections move together. It also checks a fresh YAML backend across a restart.

All data lives in dedicated temporary servers under `target/`. Input hashes and logs are retained. Supply `--paper-cache /path/to/cache` to reuse Paper's vanilla jar cache. The default port is 27100, bound to localhost.
