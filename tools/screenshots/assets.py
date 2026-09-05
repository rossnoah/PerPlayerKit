#!/usr/bin/env python3
"""Extract the vanilla assets needed to render inventory screenshots.

Pulls textures, item/block models, the GUI sheet and the bitmap font out of the
Minecraft client jar Mojang publishes. Everything lands in .cache/ and is
gitignored: these are Mojang's assets, not ours to redistribute.
"""
import json, os, subprocess, sys, zipfile

CACHE = ".cache"
ASSETS = os.path.join(CACHE, "assets")
MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"

WANT = (
    "assets/minecraft/textures/item/",
    "assets/minecraft/textures/block/",
    "assets/minecraft/textures/entity/",
    "assets/minecraft/textures/gui/",
    "assets/minecraft/textures/font/",
    "assets/minecraft/models/item/",
    "assets/minecraft/models/block/",
    "assets/minecraft/items/",
)


def fetch_json(url):
    return json.loads(subprocess.run(["curl", "-fsSL", url], check=True,
                                     capture_output=True, text=True).stdout)


def client_jar(version):
    jar = os.path.join(CACHE, f"client-{version}.jar")
    if os.path.exists(jar):
        return jar
    entry = next((v for v in fetch_json(MANIFEST)["versions"] if v["id"] == version), None)
    if entry is None:
        raise SystemExit(f"Minecraft {version} not in the version manifest")
    url = fetch_json(entry["url"])["downloads"]["client"]["url"]
    print(f"downloading client jar for {version}")
    subprocess.run(["curl", "-fsSL", "-o", jar, url], check=True)
    return jar


def sync(version):
    jar = client_jar(version)
    n = 0
    with zipfile.ZipFile(jar) as z:
        for info in z.infolist():
            if info.is_dir() or not info.filename.startswith(WANT):
                continue
            if not info.filename.endswith((".png", ".json")):
                continue
            rel = info.filename[len("assets/minecraft/"):]
            out = os.path.join(ASSETS, rel)
            if not os.path.exists(out):
                os.makedirs(os.path.dirname(out), exist_ok=True)
                with z.open(info) as src, open(out, "wb") as dst:
                    dst.write(src.read())
            n += 1
    print(f"{n} assets available in {ASSETS}")


if __name__ == "__main__":
    sync(sys.argv[1] if len(sys.argv) > 1
         else open(f"{CACHE}/paper-version.txt").read().strip())
