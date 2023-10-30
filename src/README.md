# PerPlayerKits

PerPlayerKits is a [Spigot](https://www.spigotmc.org/) plugin that allows players to make their own unique kits to PvP with. Adminstrators create a set of items in the "Virtual Kit Room", from which players pick their items. They can then make upto 9 kits each with can be shared, modified, and inspected by moderators. It features a GUI for simple kit useage of the plugin. Additionally commands can be used to quickly load kits and for moderator features. It also features protections against abusive items, limiting NBT data and enchantments to what is available in vanilla Minecraft and only allowing items that can be found in the "virtual kit room".

## Dependencies

PerPlayerKits uses the canvas library which can be found [here](https://github.com/rossnoah/canvas)

PerPlayerKits was built on Java 17 for Minecraft version 1.17 but should work with any version of 1.17 or above.

## Installation

PerPlayerKits has a minimal configuration just requiring MySQL or MySQL compatible (MariaDB) database credentials. To use the plugin run the server with the plugin in the "plugins" folder, then stop the server and edit the config.yml file in the "plugins/PerPlayerKits" folder. Fill in the database credentials and restart the server. The plugin will automatically create the database and tables as needed.

```yml
database:
  host: "localhost"
  port: "3306"
  dbname: "kitdatabase"
  username: "username"
  password: "pa55w0rd"
```

## Commands and Permissions

### kitroom

- **Usage:** `/<command>`
- **Permission:** `kit.admin`

### kit

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `[k]`

### copykit

- **Usage:** `/<command>`
- **Permission:** `kit.use`

### sharekit

- **Usage:** `/<command>`
- **Permission:** `kit.use`

### swapkit

- **Usage:** `/<command>`
- **Permission:** `kit.use`

### deletekit

- **Usage:** `/<command>`
- **Permission:** `kit.use`

### inspectkit

- **Usage:** `/<command>`
- **Permission:** `kit.staff`

### k1

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit1`

### k2

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit2`

### k3

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit3`

### k4

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit4`

### k5

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit5`

### k6

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit6`

### k7

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit7`

### k8

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit8`

### k9

- **Usage:** `/<command>`
- **Permission:** `kit.use`
- **Aliases:** `kit9`

### enderchest

- **Usage:** `/<command>`
- **Permission:** `kit.enderchest`
- **Aliases:** `ec`
