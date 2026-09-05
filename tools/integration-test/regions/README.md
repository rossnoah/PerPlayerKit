# WorldGuard integration checks

Build the candidate with `mvn verify`, then supply a server jar and compatible WorldGuard and WorldEdit jars:

```bash
python3 tools/integration-test/regions/run.py \
  --server-jar /path/to/paper.jar \
  --worldguard /path/to/worldguard.jar \
  --worldedit /path/to/worldedit.jar \
  --java /path/to/java
```

The script creates two isolated servers under `target/`. It checks real region containment, height, inherited parents, priority, tied priorities, global/feature rules, both rekit mappings, unavailable region data, and WorldGuard being disabled. The second server verifies startup and world-only behavior with no WorldGuard classes installed. Input hashes and logs are retained.

For Paper, `--paper-cache /path/to/cache` reuses a matching vanilla-jar cache. The default port is 26300 and binds to localhost; override it with `--port`.
