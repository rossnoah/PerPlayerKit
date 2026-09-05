# Documentation screenshots

One command regenerates every screenshot in `docs/images/`:

```bash
cd tools/screenshots
./auto.sh
```

Nothing else to do. No Minecraft client, no manual setup, no clicking.

## What it does

1. Builds the plugin.
2. Picks the newest Paper version the bot library can speak, and downloads it.
3. Pulls the vanilla item textures out of the Minecraft client jar.
4. Boots a throwaway Paper server on port 25599 in offline mode.
5. Connects a headless bot named `Notch`, ops it, and has it **stock the kit room,
   save nine public kits, and build four player kits**, making the same clicks a person would.
6. Opens each GUI and dumps the real slot contents to JSON.
7. Renders each capture to a PNG in `docs/images/`.

Everything is deterministic, so re-running it produces the same images.

## Requirements

`java`, `mvn`, `tmux`, `node`, `curl`, `python3`, Pillow, and PyYAML:

```bash
python3 -m pip install pillow pyyaml
```

## What gets produced

| File | GUI |
| ---- | --- |
| `MainMenu.png` | `/kit` |
| `KitEditor.png` | `/kit` → kit slot 1 |
| `KitRoomEditor.png` | `/kit` → nether star |
| `PublicKits.png` | `/publickit` |
| `EnderchestMenu.png` | `/kit` → enderchest slot 1 |

## The parts

| File | Does |
| ---- | ---- |
| `auto.sh` | Runs everything below in order |
| `kits.json` | The demo kit room, public kits and player kits. **Edit this to change what is in the shots.** |
| `capture.js` | The bot: seeds the server from `kits.json`, dumps each GUI to `captures/*.json` |
| `patch_config.py` | Declares the public kits in the server's `config.yml` and silences chat |
| `assets.py` | Extracts textures, models, the GUI sheet and the font from the client jar |
| `mcicon.py` | Renders one item icon by following its real Minecraft model |
| `render.py` | Composites a capture into a PNG on the vanilla container panel |

`.server/`, `.cache/`, `captures/` and `node_modules/` are all gitignored. The
cached client jar and its textures are Mojang's, so they are never committed. `assets.py` re-downloads them on a clean checkout.

## How the icons are drawn

Vanilla decides an item's inventory icon from its **model**, not from a single
texture. `mcicon.py` does the same:

- **Flat items** (swords, food, potions) composite `layer0..layerN` from the item
  model, applying the model's tints. Which is why a splash potion comes out the
  right colour.
- **Blocks** are drawn as an isometric cube from their `up`, `north` and `east`
  faces, with vanilla's per-face shading, so obsidian looks like a block rather
  than a flat square.
- **Chests and shields** have no item texture at all; they are entity-rendered in
  game. Both are rebuilt here from their entity textures using the UVs from the
  vanilla model.

The container panel is the real `generic_54.png` GUI sheet and the title uses the
vanilla `ascii.png` bitmap font, so the result matches the game rather than
approximating it.

## These are rendered, not photographed

The images are drawn from the real slot contents of a real running server, so
they always match what the plugin actually shows.

That makes them crisp, consistent, and reproducible on any machine. It also
means they do not include the world behind the menu, the player's hotbar, or
the mouse cursor.

What it cannot produce is anything that is not a container GUI. An in-world
shot of a placed regear shulker, or a chat message such as a share code. Take
those by hand if you want them.

## Changing what is in the shots

Everything the bot builds is defined in `kits.json`: the five kit room pages, the
public kits (id, display name, menu icon and contents) and which of them become
saved player kits. `capture.js` and `patch_config.py` both read that one file, so
the in-game contents and the `config.yml` entries cannot drift apart.

Edit `kits.json` and re-run `./auto.sh`.

To add a GUI, add one `capture(...)` call at the bottom of `capture.js`. The
slot numbers it clicks come from `GUI.java`. Main menu kit buttons are `27+col`,
the nether star is slot 37, and the kit room save barrier is slot 53.

## If it breaks

The bot pins itself to the Paper version in `.cache/paper-version.txt`, chosen
by intersecting Paper's builds with `minecraft-protocol`'s supported versions.
When Paper ships a version the library does not support yet, `auto.sh` falls
back to the newest one that works. If no version matches it stops with an error
rather than guessing.

Server output is in `.server/console.log`.

Use `PORT=25990 ./auto.sh` if the default test port is occupied. `BUILD=0` reuses the current plugin jar.
