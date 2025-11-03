# Configuration Overview:

## MiniMessage Format

All color and text formatting in this configuration uses **MiniMessage format**. This is a modern, flexible text formatting system that replaces legacy color codes.

### Common Colors

Named colors are the simplest way to add color:
- `<red>` - Red text
- `<green>` - Green text
- `<blue>` - Blue text
- `<yellow>` - Yellow text
- `<aqua>` - Aqua/Cyan text
- `<gray>` - Gray text
- `<white>` - White text
- `<black>` - Black text
- `<dark_red>` - Dark red
- `<dark_green>` - Dark green
- `<dark_blue>` - Dark blue
- `<dark_gray>` - Dark gray
- `<dark_aqua>` - Dark aqua
- `<gold>` - Gold/Orange

### RGB Colors

For precise color control, use RGB hex colors:
- `<color:#FF0000>` - Red (RGB format: #RRGGBB)
- `<color:#00FF00>` - Green
- `<color:#0000FF>` - Blue
- `<color:#FF6B35>` - Custom orange
- `<color:#A0E7E5>` - Custom teal

### Text Formatting

Combine colors with formatting:
- `<b>` - Bold text
- `<i>` - Italic text
- `<u>` - Underlined text
- `<st>` - Strikethrough text
- `<obf>` - Obfuscated text
- `<reset>` - Reset formatting

### Examples

```
<red>Error:</red> <gray>Something went wrong</gray>
<green><b>Success!</b></green> Your kit has been saved.
<color:#FF6B35><b>Important:</b></color> This is a custom orange color.
<aqua>Type <u>/kit</u> to get started!</aqua>
```

For a complete reference of all available options, see the [MiniMessage documentation](https://docs.papermc.io/adventure/minimessage/).

---

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

Defines kits and their visual representation in the UI. Kit names are displayed in their default color.

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

Lists worlds where kit commands are disabled. Players in these worlds will see a custom error message if they attempt to use the commands. This section uses mini message format for styling.

```yaml
disabled-command-worlds:
  - "example_world" # Add worlds where kit commands should be restricted.

disabled-command-message: "<red>Kits are disabled here!</red>" # Message displayed to players in disabled worlds (MiniMessage format).
```

---

### **Public Kits**

Allows customization of publicly available kits. By default, this section is commented out and needs to be configured based on requirements. Kit names are displayed in their default color.

```yaml
publickits:
  # 1:
  #   name: "Kit 1" # Kit name.
  #   icon: "DIAMOND_SWORD" # Icon for the kit.
```


### **Messages**

This section controls the messages broadcast to players when they perform various kit-related actions. Messages use mini message format for styling. Each message type can be individually enabled/disabled and customized with a custom permission node.

```yaml
messages:
  disable-kit-messages: false # Set to true to disable all kit action messages (e.g. player loaded a kit, player repaired gear, etc.)
  player-repaired:
    enabled: true # Enable or disable this specific message type
    message: "<gray>%player% repaired their gear</gray>" # Message content in mini message format
    permission: "perplayerkit.kitnotify" # Permission required to see this message (default permission defaults to true)
  player-healed:
    enabled: true
    message: "<gray>%player% healed themselves</gray>"
    permission: "perplayerkit.kitnotify"
  player-opened-kit-room:
    enabled: true
    message: "<gray>%player% opened the Kit Room</gray>"
    permission: "perplayerkit.kitnotify"
  player-loaded-private-kit:
    enabled: true
    message: "<gray>%player% loaded a kit</gray>"
    permission: "perplayerkit.kitnotify"
  player-loaded-public-kit:
    enabled: true
    message: "<gray>%player% loaded a public kit</gray>"
    permission: "perplayerkit.kitnotify"
  player-loaded-enderchest:
    enabled: true
    message: "<gray>%player% loaded an ender chest.</gray>"
    permission: "perplayerkit.kitnotify"
  player-copied-kit:
    enabled: true
    message: "<gray>%player% copied a kit</gray>"
    permission: "perplayerkit.kitnotify"
  player-copied-ec:
    enabled: true
    message: "<gray>%player% copied an ender chest</gray>"
    permission: "perplayerkit.kitnotify"
  player-regeared:
    enabled: true
    message: "<gray>%player% regeared</gray>"
    permission: "perplayerkit.kitnotify"
```

#### Message Configuration Fields:

- **enabled**: Set to `false` to disable a specific message type. When disabled, the message will not be broadcast to any players.
- **message**: The content of the message in mini message format. Supports the `%player%` placeholder which is replaced with the player's name (or display name if `use-display-name` is enabled).
- **permission**: The permission node required for players to see this message. Players without this permission will not see the broadcast. Defaults to `perplayerkit.kitnotify` which defaults to `true`.

#### Global Settings:

- **disable-kit-messages**: Set to `true` to completely suppress all kit action messages. When enabled, players will not see any broadcasts for kit-related actions, regardless of the broadcast-on-player-action feature flag or individual message permissions. This acts as a global on/off switch.

---

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

Allows customization of the regear commands which are used on some servers in various competition formats. The `/rg` and `/regear` commands can be configured independently to use different modes.

```yaml
regear:
  rg-mode: "command" #OPTIONS: command, shulker - Behavior for /rg command
  regear-mode: "command" #OPTIONS: command, shulker - Behavior for /regear command
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

#### Regear Mode Options:

- **command**: Directly restocks items from the player's loaded kit (only whitelisted items are restocked)
- **shulker**: Gives the player a physical regear shulker box that they can place and interact with

#### Example Configurations:

**Both commands use same mode:**
```yaml
regear:
  rg-mode: "command"
  regear-mode: "command"
```

**Different modes for different commands:**
```yaml
regear:
  rg-mode: "shulker"      # /rg gives a shulker
  regear-mode: "command"  # /regear directly restocks items
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

    broadcast-kit-messages: true #broadcasts when a player loads a kit or enderchest

    broadcast-on-player-action: true #broadcasts when a player uses a kit, copies a kit, etc.

    send-update-message-on-join: true #sends a message to players with perplayerkit.admin when they join the server if a new version is available

    old-death-drops: false #makes it so players drop items in a condensed area rather than spreading out when they die
```

#### Feature Flag Descriptions:

**Kit Loading Features:**
- **rekit-on-respawn**: Automatically loads the player's last used kit when they respawn after death
- **rekit-on-kill**: Automatically loads the player's last used kit when they kill another player
- **broadcast-kit-messages**: Controls whether broadcast messages are sent when players load kits or enderchesets (e.g., "Player loaded a kit"). When set to `false`, these specific kit-loading broadcast messages are suppressed

**Action Broadcast Features:**
- **broadcast-on-player-action**: Controls whether broadcast messages are sent for other player actions like copying kits, repairing gear, opening kit room, etc. This does NOT affect kit loading messages (controlled by `broadcast-kit-messages`)

**Health/Hunger Features:**
- **set-health-on-kit-load**: Sets player health to full when loading a kit
- **set-hunger-on-kit-load**: Sets player hunger to full when loading a kit
- **set-saturation-on-kit-load**: Sets player saturation to full when loading a kit
- **remove-potion-effects-on-kit-load**: Removes all potion effects when loading a kit
- **heal-on-enderchest-load**: Sets player health to full when loading an enderchest
- **feed-on-enderchest-load**: Sets player hunger to full when loading an enderchest
- **set-saturation-on-enderchest-load**: Sets player saturation to full when loading an enderchest
- **remove-potion-effects-on-enderchest-load**: Removes all potion effects when loading an enderchest
