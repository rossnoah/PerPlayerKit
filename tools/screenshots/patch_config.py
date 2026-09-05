#!/usr/bin/env python3
"""Prepare the throwaway server's config.yml for screenshots.

Declares the public kits the capture bot fills in, and silences join and
broadcast messages so nothing lands in chat mid-capture.
"""
import json, os, sys
try:
    import yaml
except ImportError:
    sys.exit("PyYAML is required:  python3 -m pip install pyyaml")

KITS = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "kits.json")))
PUBLIC_KITS = {k: {"name": v["name"], "icon": v["icon"]}
               for k, v in KITS["publicKits"].items()}

path = sys.argv[1]
cfg = yaml.safe_load(open(path))
cfg["publickits"] = PUBLIC_KITS
cfg["motd"]["enabled"] = False
cfg["broadcasts"]["scheduled"]["enabled"] = False
cfg["broadcasts"]["enabled"] = False
cfg["updates"]["notify-admins-on-join"] = False
yaml.safe_dump(cfg, open(path, "w"), sort_keys=False, allow_unicode=True)
print(f"config.yml prepared: {len(PUBLIC_KITS)} public kits declared, chat silenced")
