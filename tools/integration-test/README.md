# Integration test

Boots a real Paper or Spigot server for each supported Minecraft version, drives
PerPlayerKit through the console, and checks what actually happened.

```bash
./tools/integration-test/run.sh                  # everything
./tools/integration-test/run.sh paper            # one platform
./tools/integration-test/run.sh paper 1.21.8     # one target
```

`KEEP_WORK=1` leaves the server directories in `target/integration-test/` so you
can read the logs or poke at the database. `BUILD=0` skips rebuilding the plugin.

## Why this exists

The plugin compiles against the 1.19 API and has to keep working on servers years
newer, across two platforms whose internals differ. The unit tests never load a
server, so they cannot see the failures that actually happen in the wild:

- **Class loading and shading.** The bundled Adventure is relocated, and the
  relocation has to leave the class names in `AudienceCompat` alone while
  renaming the `META-INF/services` files. Get it wrong and nothing throws - chat
  just stops.
- **API drift.** A material, enchantment or potion that disappears in a later
  version, or a method that changes shape.
- **Platform differences.** Paper implements Adventure natively; Spigot does not,
  so the two take different delivery paths.

## What it checks

Per target: the plugin enables, prints its startup guide and permission list,
notices the empty kit room, lists its subcommands, fills five kit room pages and
the supported bundled public kits with `autosetup`, refuses to redo the work on a second run, and
leaves five page rows plus eight or nine public kit rows in storage. It restarts the server and verifies autosetup still finds every saved entry. It also fails on any exception, `LinkageError`,
`NoClassDefFoundError` or `NoSuchMethodError` attributed to the plugin.

Those console lines go through the same `Lang` path that players do, so if
components stop rendering on a version, the expectations stop matching.

## The chat probe

`chat-probe.js` joins each server as a real client and records what arrives in
chat. This is the branch the console cannot reach: `Lang.deliver` treats a
`Player` differently from the console, and that player branch is the one that
broke on Paper 26 - silently, without throwing anything.

It talks the protocol directly rather than going through a bot framework.
Nothing here needs pathfinding or world state, and the lower layer supports
newer Minecraft versions than the bots built on top of it - which matters,
because the newest versions are where this sort of thing breaks. Chat arrives as
a JSON string on older versions and as an NBT component from 1.20.3 on; both are
normalised to one shape before the assertions run.

It asserts the intro arrives, the permission list is in it, and **the autosetup
offer is still clickable**. That last one is the real check: components reach a
client as JSON, and a fallback to a legacy string would still show the text while
quietly dropping the click event.

Install it once:

```bash
npm install --prefix tools/integration-test
```

Without it the chat checks are skipped, not failed.

### Versions the client library does not know yet

Packet definitions lag new releases, but the version index lists them with their
protocol numbers well before the definitions land. A release that only bumps the
protocol still speaks the older packets for everything here, so the probe borrows
the newest definitions available and announces the real protocol number - which
has to come from the index, because getting it wrong means the server rejects the
client as outdated.

That is how 26.2 is covered: packets from 26.1.2, announced as protocol 776. The
run says so when it happens. Some packets do not line up byte for byte and the
library complains on stderr; chat is not one of them. If a future release does
reshape chat, the assertions fail rather than passing on garbage.

## What it does not check

**Anything visual.** Whether a colour reads well or a menu lays out correctly is
not something this can see.

## Versions

`TARGETS` at the top of `run.sh` is the list, as `platform:version:java-major`.
Add a row when a new Minecraft ships, and add the matching entry to
`.github/workflows/integration-test.yml`.

Each Minecraft version needs a specific Java. Locally the script finds it with
`java_home`; in CI it reads the `JAVA_HOME_<major>_<arch>` variables that
`actions/setup-java` exports. A version whose Java is missing is skipped and
reported, not failed.

Server jars are cached in `.cache/`. Paper downloads in seconds. Spigot is not
redistributable so BuildTools compiles it, which takes several minutes the first
time for each version. Paper also fetches a vanilla jar of its own on first
start, kept in `.cache/paperclip/<version>/` so that happens once rather than
once per run.

## When it fails on a loaded machine

Servers are heavy and these run back to back, so the harness is written to
survive a busy box rather than assume an idle one: the client stops as soon as
the message it is waiting for arrives instead of sitting out a fixed window,
ports are waited on between targets, and a server that ignores `stop` is killed
by its own pid rather than its shell's. If a run still fails oddly, check the
load first - a target failing here that passed a moment ago is usually the
machine, not the plugin.

## WorldGuard regions

The [region probe](regions/README.md) tests real geometry, inherited parents, overlapping priorities, unavailable data, and startup without WorldGuard. It uses isolated servers and supplied compatible WorldGuard and WorldEdit jars.

## Remembered kit selections

The [selection probe](selections/README.md) checks personal/public regear and respawn selection, enderchest choices, process restarts, and SQLite-to-YAML migration.

## Item rules and kit room navigation

The [feature probe](features/README.md) checks real Bukkit item metadata, nested item filtering, potion limits, and the kit room arrow/save controls using a connected client.
