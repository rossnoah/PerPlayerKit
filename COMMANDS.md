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
| `k1`                | `kit1`                   | `perplayerkit.kit`             |
| `k2`                | `kit2`                   | `perplayerkit.kit`             |
| `k3`                | `kit3`                   | `perplayerkit.kit`             |
| `k4`                | `kit4`                   | `perplayerkit.kit`             |
| `k5`                | `kit5`                   | `perplayerkit.kit`             |
| `k6`                | `kit6`                   | `perplayerkit.kit`             |
| `k7`                | `kit7`                   | `perplayerkit.kit`             |
| `k8`                | `kit8`                   | `perplayerkit.kit`             |
| `k9`                | `kit9`                   | `perplayerkit.kit`             |
| `ec1`               | `enderchest1`            | `perplayerkit.enderchest`      |
| `ec2`               | `enderchest2`            | `perplayerkit.enderchest`      |
| `ec3`               | `enderchest3`            | `perplayerkit.enderchest`      |
| `ec4`               | `enderchest4`            | `perplayerkit.enderchest`      |
| `ec5`               | `enderchest5`            | `perplayerkit.enderchest`      |
| `ec6`               | `enderchest6`            | `perplayerkit.enderchest`      |
| `ec7`               | `enderchest7`            | `perplayerkit.enderchest`      |
| `ec8`               | `enderchest8`            | `perplayerkit.enderchest`      |
| `ec9`               | `enderchest9`            | `perplayerkit.enderchest`      |
| `enderchest`        | `ec`                     | `perplayerkit.viewenderchest`  |
| `savepublickit`     | `N/A`                    | `perplayerkit.admin`           |
| `regear`            | `rg`                     | `perplayerkit.regear`          |
| `heal`              | `N/A`                    | `perplayerkit.heal`            |
| `repair`            | `N/A`                    | `perplayerkit.repair`          |

## Kit Sharing and Transfers

### Sharing with a player directly

`/sharekit <slot> <player>` (or `/shareec <slot> <player>` for ender chests) sends the target player a request in chat with clickable **[ACCEPT]** and **[DECLINE]** buttons. If they accept, a snapshot of the shared kit is applied to their current inventory (or ender chest), exactly like redeeming a share code. Requests expire after 120 seconds, and a newer request to the same player replaces the previous one. The buttons run `/shareaccept <id>` and `/sharedecline <id>` under the hood; with only one pending request, `/shareaccept` and `/sharedecline` also work without an id.

### Sharing with a code

`/sharekit <slot>` (or `/shareec <slot>`) without a player still generates a share code, which is useful when sharing with multiple people at once. Anyone can redeem it with `/copykit <code>` while it is valid. Codes expire after 15 minutes.

### Transferring kits between accounts

`/transferkits <player>` sends the target player a request to receive **all** of your kits and ender chests. If they accept, a snapshot of every kit and ender chest slot you have is saved into their corresponding slots, overwriting whatever they had in those slots (slots you don't have are left untouched). Your own kits are not modified. This is useful when moving to a new account: log in with both accounts, run `/transferkits <new account>` from the old one, and accept on the new one.

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