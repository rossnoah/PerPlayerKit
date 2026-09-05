# Item filter and kit room checks

Build with `mvn verify` and install the existing chat probe dependencies, then run:

```bash
python3 tools/integration-test/features/run.py \
  --server-jar /path/to/server.jar --version 1.21.8 --java /path/to/java
```

The probe checks real enchantments, stored book enchantments, unbreakable items, shulkers, bundles, base potions, and custom potion effects. It checks that filtering copies leaves source metadata intact. A connected protocol client then opens the kit room, navigates the new arrow buttons, saves page 8, and verifies permission revocation blocks raw editor access.

Use `--paper-cache` for an existing vanilla jar cache and `--port` to override the localhost port (27200). Logs, client messages, and input hashes are retained in the temporary server under `target/`.
