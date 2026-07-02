# PerPlayerKit Command Docs

- **Version:** 1.6.2
- **Author(s):** Noah Ross
- **Minimum Spigot/Paper Version:** 1.19

## Commands

The following table outlines each command, its usage, aliases, and permissions required.

| Command             | Aliases                  | Permission                     |
|---------------------|--------------------------|--------------------------------|
| `perplayerkit`      | `N/A`                    | `perplayerkit.admin`           |
| `aboutperplayerkit` | `N/A`                    | `N/A`                          |
| `kitroom`           | `N/A`                    | `perplayerkit.admin`           |
| `kit`               | `k`                      | `perplayerkit.menu`            |
| `copykit`           | `copyec, copyenderchest` | `perplayerkit.copykit`         |
| `sharekit`          | `N/A`                    | `perplayerkit.sharekit`        |
| `shareec`           | `shareenderchest`        | `perplayerkit.shareenderchest` |
| `transferkits`      | `transferkit`            | `perplayerkit.transferkits`    |
| `shareaccept`       | `N/A`                    | `perplayerkit.copykit`         |
| `sharedecline`      | `N/A`                    | `perplayerkit.copykit`         |
| `swapkit`           | `N/A`                    | `perplayerkit.swapkit`         |
| `deletekit`         | `N/A`                    | `perplayerkit.deletekit`       |
| `inspectkit`        | `N/A`                    | `perplayerkit.staff`           |
| `inspectec`         | `N/A`                    | `perplayerkit.staff`           |
| `publickit`         | `pk, premadekit`         | `perplayerkit.publickit`       |
| `k1`..`k<max-kits>` | `kit1`..`kit<max-kits>`  | `perplayerkit.kit`             |
| `ec1`..`ec<max-kits>` | `enderchest1`..`enderchest<max-kits>` | `perplayerkit.enderchest` |
| `enderchest`        | `ec`                     | `perplayerkit.viewenderchest`  |
| `savepublickit`     | `N/A`                    | `perplayerkit.admin`           |
| `purgeitem`         | `purgeitems`             | `perplayerkit.admin`           |
| `regear`            | `rg`                     | `perplayerkit.regear`          |
| `heal`              | `N/A`                    | `perplayerkit.heal`            |
| `repair`            | `N/A`                    | `perplayerkit.repair`          |

The `k<N>`/`ec<N>` shortcut commands cover every kit slot up to the `max-kits` config option (default 9, up to 99). Slots 1-9 are declared statically; commands for slots 10 and above only exist when `max-kits` is raised above 9 and are registered automatically at startup, also reachable as `/perplayerkit:k<N>`.

## Kit Sharing and Transfers

### Sharing with a player directly

`/sharekit <slot> <player>` (or `/shareec <slot> <player>` for ender chests) sends the target player a request in chat with clickable **[ACCEPT]** and **[DECLINE]** buttons. If they accept, a snapshot of the shared kit is applied to their current inventory (or ender chest), exactly like redeeming a share code. Requests expire after 120 seconds, and a newer request to the same player replaces the previous one. The buttons run `/shareaccept <id>` and `/sharedecline <id>` under the hood; with only one pending request, `/shareaccept` and `/sharedecline` also work without an id.

### Sharing with a code

`/sharekit <slot>` (or `/shareec <slot>`) without a player still generates a share code, which is useful when sharing with multiple people at once. Anyone can redeem it with `/copykit <code>` while it is valid. Codes expire after 15 minutes.

### Transferring kits between accounts

`/transferkits <player>` sends the target player a request to receive **all** of your kits and ender chests. If they accept, a snapshot of every kit and ender chest slot you have is saved into their corresponding slots, overwriting whatever they had in those slots (slots you don't have are left untouched). Your own kits are not modified. This is useful when moving to a new account: log in with both accounts, run `/transferkits <new account>` from the old one, and accept on the new one.

## Purging Items from Stored Kits

`/purgeitem <item> <all confirm|player ...>` (permission: `perplayerkit.admin`, also usable from the console) deletes every occurrence of a specific item type from stored kits and ender chests:

- `/purgeitem TNT PlayerOne PlayerTwo` — removes the item from all kit and ender chest slots of the listed players. Players can be given by name or UUID and do not need to be online. If any name cannot be resolved, the purge is cancelled so it never silently skips someone.
- `/purgeitem TNT all confirm` — removes the item from **every** player kit and ender chest entry in the database. Because this touches all stored data, the `confirm` keyword is required; running the command without it only prints a warning.

Details worth knowing:

- **Nested containers are searched too.** The item is also removed from inside shulker boxes of all colors, any other container block stored as an item (chests, barrels, dispensers, etc.), and bundles — including containers nested inside bundles.
- **Works with every storage backend.** Kits are stored as serialized blobs in all backends (SQLite, MySQL, PostgreSQL, Redis, YAML), so the purge rewrites each affected entry in place and behaves identically everywhere.
- **Empty entries are deleted.** If removing the item leaves a kit or ender chest completely empty, the entry is deleted instead of being kept as an empty kit.
- **Online players are covered.** Cached kits of online players are refreshed, so the next `/k1`-style load uses the purged version. The purge does not modify anyone's *live* inventory — only stored kits and ender chests.
- **Public kits and the kit room are not touched.** Those are admin-managed; edit them with `/savepublickit` and `/kitroom` if needed.
- The purge runs asynchronously; progress is logged to the console and a summary (items removed, entries modified/deleted/scanned) is sent when it finishes. Only one purge can run at a time.

## Regear Command Details

The regear system allows players to restock items from their loaded kit. The behavior of the `/rg` and `/regear` commands can be configured independently:

### Modes

**Command Mode**: Directly restocks whitelisted items from the player's loaded kit
- Cooldown applies between uses
- Damage timer prevents regearing while in combat
- Only whitelisted items are restocked

**Shulker Mode**: Gives the player a regear shulker box
- Player places the shulker on the ground to open a special interface
- Player clicks the regear shell inside to trigger the restock
- Cooldown applies between command uses
- Damage timer prevents regearing while in combat

### Configuration

Both `/rg` and `/regear` can use different modes. See **CONFIG.md** → **Regear Command** for configuration options (`rg-mode` and `regear-mode`).

## Permissions

The following table outlines each top-level permission and the sub-permissions it grants.

| Permission         | Grants                                                                                                                                                                                                                                                                                                                                                                                       |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `kit.admin`        | `perplayerkit.admin`                                                                                                                                                                                                                                                                                                                                                                         |
| `kit.staff`        | `perplayerkit.staff`                                                                                                                                                                                                                                                                                                                                                                         |
| `kit.use`          | `perplayerkit.use`                                                                                                                                                                                                                                                                                                                                                                           |
| `perplayerkit.use` | `perplayerkit.menu`, `perplayerkit.copykit`, `perplayerkit.sharekit`, `perplayerkit.shareenderchest`, `perplayerkit.transferkits`, `perplayerkit.swapkit`, `perplayerkit.deletekit`, `perplayerkit.publickit`, `perplayerkit.kit`, `perplayerkit.enderchest`, `perplayerkit.viewenderchest`, `perplayerkit.regear`, `perplayerkit.heal`, `perplayerkit.repair`, `perplayerkit.rekitonrespawn`, `perplayerkit.rekitonkill` |

## Message Notifications

The following permission controls which kit-related action messages players see:

| Permission              | Purpose                                                                                          |
|-------------------------|--------------------------------------------------------------------------------------------------|
| `perplayerkit.kitnotify` | Allows players to see notifications about kit-related actions (e.g., when other players load kits, repair gear, etc.). **Defaults to `true`** - all players can see these messages by default. Set to `false` to hide all kit action messages from a player. |