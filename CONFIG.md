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


### **Sounds**

This section allows you to enable, disable, and customize the sounds played by the plugin.

```yaml
sounds:
  enabled: true # Set to false to disable all plugin sounds.
  # Sound played on successful actions (e.g. saving a kit)
  success: ENTITY_PLAYER_LEVELUP
  # Sound played on failed actions (e.g. attempting to use a disabled command)
  failure: ENTITY_ITEM_BREAK
  # Sound played when a button is clicked in a GUI
  click: UI_BUTTON_CLICK
  # Sound played when a GUI is opened
  open_gui: UI_BUTTON_CLICK
  # Sound played when a GUI is closed
  close_gui: UI_BUTTON_CLICK
```


### **Anti-Exploit**

This section allows you to enable or disable anti-exploit features, commonly applicable to this plugin.

```yaml
anti-exploit:
  only-allow-kitroom-items: false #requires that items be in the kitroom before they can be used in a kit.
  import-filter: false #requires only-allow-kitroom-items to be true. prevents the duplications of items not in the kitroom by using the kit import button.
  block-spaces-in-commands: false #prevents bypassing command filters.
  prevent-shulkers-dropping-items: false #prevents shulkers from dropping items when broken. Anti-lag feature.
```

### **Regear Command**

Allows customization of the regear command which is used on some servers in various competition formats.

```yaml
regear:
  mode: "command" #OPTIONS: command, shulker
  command-cooldown: 5 #command cooldown in seconds. recommended to be low or 0 if using shulker mode.
  damage-timer: 5 #time in seconds to wait after taking damage before players can regear
  allow-while-using-elytra: true #set false to block regearing command while using elytra
  #
  # Allow regearing all items by setting:
  # invert-whitelist: true
  # whitelist: []
  #
  #
  invert-whitelist: false #setting this to true makes the whitelist a blacklist
  whitelist:
    - ENDER_PEARL
    - END_CRYSTAL
    - OBSIDIAN
    - GLOWSTONE
    - RESPAWN_ANCHOR


```



### **Feature Flags**

A number of plugin features and settings. These can be enabled or disabled based on your server's requirements.
```yaml
# Various feature flags for the plugin
feature:
    set-health-on-kit-load: false
    set-hunger-on-kit-load: false
    set-saturation-on-kit-load: false
    remove-potion-effects-on-kit-load: false
    
    heal-on-enderchest-load: false
    feed-on-enderchest-load: false
    set-saturation-on-enderchest-load: false
    remove-potion-effects-on-enderchest-load: false
    
    rekit-on-respawn: true
    rekit-on-kill: false
    
    broadcast-on-player-action: true #broadcasts when a player uses a kit, copies a kit, etc.
    
    send-update-message-on-join: true #sends a message to players with perplayerkit.admin when they join the server if a new version is available
    
    old-death-drops: false #makes it so players drop items in a condensed area rather than spreading out when they die
```
