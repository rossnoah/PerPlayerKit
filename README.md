## License Disclaimer - ALL RIGHTS RESERVED

### Unlicensed Software - All Rights Reserved

Although the source code is posted here, neither the source code nor any binaries are licensed for use, copying, modification, or distribution. All rights are reserved.

### What Does This Mean?

- **Unauthorized Use:** You are not allowed to use this software for any purpose without explicit permission.
- **Copying:** You are not allowed to copy or download the software or any part of it.
- **Modification:** You are not allowed to alter the software in any way.
- **Distribution:** You are not allowed to share or distribute the software.

# PerPlayerKits

PerPlayerKits is a [Spigot](https://www.spigotmc.org/) plugin that allows players to make their own unique kits to PvP with. Administrators create a set of items in the "Virtual Kit Room", from which players pick their items. They can then make up to 9 kits each with can be shared, modified, and inspected by moderators. It features a GUI for simple kit usage of the plugin. Additionally, commands can be used to quickly load kits and for moderator features. It also features protections against abusive items, limiting NBT data and enchantments to what is available in vanilla Minecraft and only allowing items that can be found in the "virtual kit room".
Players can also use the plugin to create enderchest kits that will be loaded into their enderchest when they select the enderchest kit.

## Dependencies

PerPlayerKits uses a minecraft GUI library called canvas, it can be found [here](https://github.com/IPVP-MC/canvas)

PerPlayerKits was built on Java 17 for Minecraft version 1.17 but should work with any version of 1.17 or above.

## Installation

PerPlayerKits has a minimal configuration just requiring selecting the database type. You can use SQLite or a MySQL/MySQL compatible (MariaDB) database. To use the plugin run the server with the plugin in the "plugins" folder, then stop the server and edit the config.yml file in the "plugins/PerPlayerKits" folder. Fill in the database credentials and restart the server. The plugin will automatically create the database and tables as needed.

```yml
database:
  type: "mysql" # "mysql" or "sqlite"
  host: "localhost"
  port: "3306"
  dbname: "kitdatabase"
  username: "username"
  password: "pa55w0rd"
```

## Commands and Permissions

For detailed information on commands and permissions, please refer to the [commands documentation](commands.md).
