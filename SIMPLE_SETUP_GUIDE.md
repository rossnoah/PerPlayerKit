# 🎮 PerPlayerKit - Simple Setup Guide for Server Owners

_Don't worry, we'll get you set up step by step! No technical experience needed._

## What is PerPlayerKit?

PerPlayerKit lets your players create their own custom PvP kits (armor, weapons, potions, etc.) instead of everyone using the same boring kits. Think of it like a "build your own loadout" system for Minecraft PvP!

## 📋 Before You Start

**You need:**

- A Minecraft server running Paper or Spigot (version 1.19 or newer)
- Access to your server files (usually through FTP or a control panel)

**Don't have Paper?** Download it from [papermc.io](https://papermc.io/) - it's better than regular Spigot!

---

## 🚀 Step 1: Install the Plugin

1. **Download PerPlayerKit** from wherever you got it
2. **Stop your server** (important!)
3. **Put the .jar file** in your `plugins` folder
4. **Start your server** - it will create the config files
5. **Stop your server again** - we need to edit the config

---

## 🎯 Step 2: Set Up Your Kit Room

The kitroom is where players pick items for their kits. You need to create these in-game.

### Creating Kit Rooms:

1. **Start your server**
2. **Join as an admin** (make sure you have OP or the `perplayerkit.admin` permission)
3. **Type `/kit`** - this opens the main plugin menu
4. **Click the Nether Star** - this opens the kit room edito
5. **For each kit room:**
   - Fill the GUI with items you want players to choose from
   - Hover over the **barrier** block in the cornor and _shift right click_ to save the contents of the menu
   - You can adjust the names and items representing each page of the kit room in the config

### Example Kit Room Ideas:

- **Crystal PvP**: End crystals, obsidian, armor, totems
- **Sword PvP**: Swords, shields, armor, food
- **UHC**: Golden apples, potions, bows, materials
- **Axe Combat**: Axes, shields, specialized gear
- **Utility**: Potions, wind charges, ender pearls, misc items

---

## 👥 Step 3: Set Up Permissions

_It is recommended to setup specific permissions instead of using the general ones but this will due for a basic setup._
Your players need permissions to use the plugin. Add these to your permissions plugin:

### For Regular Players:

```
perplayerkit.use
```

_This gives them access to everything they need!_

### For Staff Members:

```
perplayerkit.staff
```

_This lets them inspect player kits and moderate_

### For Admins:

```
perplayerkit.admin
```

_Full access to everything_

**Don't have a permissions plugin?** Get LuckPerms - it's the best and easiest to use!

---

## 🎮 Step 4: Tell Your Players How to Use It

Share this with your players:

### Basic Commands:

- **`/kit`** or **`/k`** - Open the main kit menu
- **`/k1`** through **`/k9`** - Quickly load kit 1-9

### How to Make a Kit:

1. Type `/kit` to open the menu
2. Click "Create New Kit"
3. Choose items from the kit rooms you set up
4. Arrange them in your inventory how you want
5. Save the kit with a name
6. Done! You can now load this kit anytime

---

## 🔧 Optional: Useful Settings

Here are some settings you might want to change in your config:

### Make Players Heal When They Load Kits:

```yaml
feature:
  set-health-on-kit-load: true
  set-hunger-on-kit-load: true
```

### Give Players Their Kit Back When They Respawn:

```yaml
feature:
  rekit-on-respawn: true
```

### Disable Kits in Certain Worlds:

```yaml
disabled-command-worlds:
  - "spawn"
  - "lobby"
```

---

## 🆘 Common Problems & Solutions

### "Players can't use /kit command"

- **Fix**: Check permissions! They need `perplayerkit.use`

### "Kit rooms are empty"

- **Fix**: You, the admin, need to set them up with as explained above

### "Plugin won't start"

- **Fix**: Make sure you're using Paper/Spigot 1.19+ and Java 17+

### "Database errors"

- **Fix**: Use SQLite unless you specifically need MySQL

### "Items are disappearing from player kits"

- **Fix**: This is the anti-exploit filter working! By default, players can only use items that exist in your kit rooms. If you want to allow all items, set `only-allow-kitroom-items: false` in your config.yml under the `anti-exploit` section.

---

## 🎉 You're Done!

Your players can now:

- Create up to 9 custom kits each
- Share kits with friends
- Quickly load their favorite setups
- Have way more fun in PvP!

**Need help?** Join the Discord: [https://discord.gg/5djuBSKWuV](https://discord.gg/5djuBSKWuV)

---

## 📚 Want More Advanced Features?

Once you're comfortable with the basics, check out:

- [CONFIG.md](./CONFIG.md) - All configuration options
- [COMMANDS.md](./COMMANDS.md) - Complete command list
- [API.md](./API.md) - For developers

**Remember**: Start simple! You can always add more features later once your players are used to the plugin.
