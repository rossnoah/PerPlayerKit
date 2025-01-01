# Configuration Overview:

### **Database Credentials**

Select either storage type. SQLite is recommended for small servers, while MySQL is recommended for larger servers or multi-server setups.

```yaml
# It is strongly recommended to use MySQL or SQLite
# YAML should not be used in any environment


storage: #sqlite, mysql, redis, yml (yaml)
  type: "sqlite"

mysql:
  host: "localhost"
  port: "3306"
  dbname: "kitdatabase"
  username: "username"
  password: "pa55w0rd"

redis:
  host: "localhost"
  port: 6379
  password: "pa55w0rd"
```

---

### **Message of the Day (MOTD)**

The MOTD displays a message to users when they join the server. This section uses mini message format to style and format the message.

```yaml
motd:
  enabled: true # Enable or disable the MOTD feature.
  delay: 5 # The delay in seconds before the MOTD is shown after a player joins.
  message: # The message content in mini message format.
    - "" # Empty line for spacing.
    - "<gray>   <st>                </st> <aqua><b>Per Player Kits</b><gray> <st>                </st>"
    - "" # Additional spacing.
    - "         <white>Type <aqua>/kit<white>, <aqua>/k <white>or <aqua>/pk<white> to get started!"
    - ""
    - "<gray>   <st>                                                         "
    - ""
```

---

### **Scheduled Broadcast**

Broadcasts messages periodically to all players, using a defined time interval. This section uses mini message format to style and format the message.

```yaml
scheduled-broadcast:
  enabled: true # Enable or disable periodic broadcasts.
  period: 90 # Interval between broadcasts in seconds.
  messages: # List of messages to broadcast, in mini message format.
    - "Example message"
```

---

### **Kit Room**

Defines kits and their visual representation in the UI. This section uses the old text formating using & color codes.

```yaml
kitroom:
  items:
    1:
      name: "OG Vanilla" # Kit name.
      material: "DIAMOND_SWORD" # Icon representing the kit.
    2:
      name: "Training"
      material: "WOODEN_SWORD"
    3:
      name: "Potions"
      material: "SPLASH_POTION"
    4:
      name: "Armory"
      material: "NETHERITE_SWORD"
    5:
      name: "Axe & UHC"
      material: "SHIELD"
```

---

### **Disabled Command Worlds**

Lists worlds where kit commands are disabled. Players in these worlds will see a custom error message if they attempt to use the commands. This section uses the old text formating using & color codes.

```yaml
disabled-command-worlds:
  - "example_world" # Add worlds where kit commands should be restricted.

disabled-command-message: "&cKits are disabled here!" # Message displayed to players in disabled worlds.
```

---

### **Public Kits**

Allows customization of publicly available kits. By default, this section is commented out and needs to be configured based on requirements. This section uses the old text formating using & color codes.

```yaml
publickits:
  # 1:
  #   name: "Kit 1" # Kit name.
  #   icon: "DIAMOND_SWORD" # Icon for the kit.
```


### **Anti-Exploit**

This section allows you to enable or disable anti-exploit features, commonly applicable to this plugin.

```yaml
anti-exploit:
  block-spaces-in-commands: true
  import-filter: true
```