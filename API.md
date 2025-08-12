# PerPlayerKit API Documentation

## Overview

The PerPlayerKit API provides a comprehensive, modern interface for interacting with the PerPlayerKit plugin. It features:

- **Thread-safe operations** - All API methods can be safely called from any thread
- **Async/await patterns** - CompletableFuture-based operations for non-blocking execution  
- **Fluent builders** - Chainable method calls for complex operations
- **Backwards compatibility** - Seamless integration with existing code
- **Modern Java patterns** - Uses Optional, Stream API, and other Java 8+ features

## Getting Started

### Basic Usage

```java
// Get the API instance
PerPlayerKitAPI api = PerPlayerKitAPI.getInstance();

// Check if PerPlayerKit is available
if (!PerPlayerKitAPI.isAvailable()) {
    getLogger().warning("PerPlayerKit is not available!");
    return;
}

// Register your plugin (recommended for better support)
APIRegistration registration = api.registerPlugin(this);
```

### Kit Management

```java
// Save player's current inventory to slot 1
api.kits()
   .forPlayer(player)
   .saveCurrentInventory(1)
   .thenRun(() -> player.sendMessage("Kit saved!"));

// Load a kit for a player
api.kits()
   .forPlayer(player)
   .loadKit(1)
   .thenAccept(success -> {
       if (success) {
           player.sendMessage("Kit loaded!");
       } else {
           player.sendMessage("No kit found in slot 1");
       }
   });

// Check if player has a kit
if (api.kits().forPlayer(player).hasKit(1)) {
    player.sendMessage("You have a kit in slot 1");
}

// Get all occupied slots
List<Integer> slots = api.kits().forPlayer(player).getOccupiedSlots();
player.sendMessage("You have kits in slots: " + slots);
```

### Advanced Kit Operations

```java
// Use the advanced builder pattern
api.kits()
   .builder()
   .forPlayer(player)
   .inSlot(2)
   .withItems(customItems)
   .withValidation(true)
   .withNotification(true)
   .save()
   .thenRun(() -> getLogger().info("Kit saved with validation"));

// Copy a kit to another slot
api.kits()
   .forPlayer(player)
   .copyKit(1, 2)
   .thenRun(() -> player.sendMessage("Kit copied from slot 1 to slot 2"));

// Swap two kits
api.kits()
   .forPlayer(player)
   .swapKits(1, 2)
   .thenRun(() -> player.sendMessage("Kits swapped"));
```

### Player Management

```java
// Check player permissions
PlayerAPI players = api.players();
if (players.hasKitPermission(player, 5)) {
    player.sendMessage("You can use kit slot 5");
}

// Get accessible slots
List<Integer> accessibleSlots = players.getAccessibleSlots(player);
player.sendMessage("You can access slots: " + accessibleSlots);

// Get player statistics
players.getStatistics(player.getUniqueId())
       .thenAccept(stats -> {
           player.sendMessage("Kits saved: " + stats.getKitsSaved());
           player.sendMessage("Kits loaded: " + stats.getKitsLoaded());
       });

// Manage preferences
players.setAutoSaveEnabled(player, true);
players.setGuiSoundsEnabled(player, false);
players.setPreferredGuiTheme(player, "dark");
```

### Event Handling

```java
// Listen to kit events
PerPlayerKitEventManager events = api.events();

// Kit saved event
events.onKitSaved(event -> {
    Player p = event.getPlayer();
    int slot = event.getSlot();
    getLogger().info(p.getName() + " saved kit in slot " + slot);
});

// Kit loading event (cancellable)
events.onKitLoading(EventPriority.HIGH, event -> {
    if (event.getPlayer().getName().equals("BlockedPlayer")) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("You cannot load kits!");
    }
});
```

### GUI Management

```java
// Open GUIs
GuiAPI gui = api.gui();
gui.openMainGui(player);
gui.openMainGui(player, "dark"); // With specific theme
gui.openKitPreview(player, 1);   // Preview kit slot 1

// Create custom GUI
gui.builder()
   .title("Custom Kit Menu")
   .size(27)
   .theme("neon")
   .item(13, customItem, context -> {
       context.getPlayer().sendMessage("Clicked!");
   })
   .build()
   .open(player);
```

### Data Management

```java
// Store custom data
PlayerDataAPI data = api.data();
data.setData(player.getUniqueId(), "custom.score", 100)
    .thenRun(() -> getLogger().info("Score saved"));

// Retrieve data with type safety
data.getData(player.getUniqueId(), "custom.score", Integer.class)
    .thenAccept(score -> {
        if (score.isPresent()) {
            player.sendMessage("Your score: " + score.get());
        }
    });
```

## API Interfaces

### Core Interfaces

- **`PerPlayerKitAPI`** - Main entry point providing access to all functionality
- **`KitAPI`** - Kit management operations with fluent builders
- **`PlayerAPI`** - Player permissions, statistics, and preferences  
- **`GuiAPI`** - GUI management and custom GUI creation
- **`PlayerDataAPI`** - Persistent data storage and retrieval
- **`PerPlayerKitEventManager`** - Event system with modern listeners

### Builder Patterns

All major operations support fluent builder patterns:

- **`PlayerKitBuilder`** - Player-specific kit operations
- **`KitBuilder`** - Advanced kit creation and management
- **`PlayerBuilder`** - Player preference management
- **`GuiBuilder`** - Custom GUI creation
- **`DataBuilder`** - Batch data operations

## Error Handling

```java
// CompletableFuture error handling
api.kits().saveKit(playerId, slot, items)
   .thenRun(() -> getLogger().info("Kit saved successfully"))
   .exceptionally(throwable -> {
       getLogger().severe("Failed to save kit: " + throwable.getMessage());
       return null;
   });
```

## Thread Safety

All API operations are thread-safe and can be called from any thread:

```java
// Safe to call from any thread
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    api.kits().saveKit(playerId, slot, items)
       .thenRun(() -> getLogger().info("Kit saved asynchronously"));
});
```

## Best Practices

### 1. Register Your Plugin
```java
@Override
public void onEnable() {
    if (PerPlayerKitAPI.isAvailable()) {
        APIRegistration registration = PerPlayerKitAPI.getInstance().registerPlugin(this);
    }
}
```

### 2. Handle Async Operations Properly
```java
// Good: Chain operations
api.kits().saveKit(playerId, slot, items)
   .thenCompose(v -> api.players().getStatistics(playerId))
   .thenAccept(stats -> updatePlayerDisplay(stats));
```

### 3. Validate Player Permissions
```java
if (!api.players().hasKitPermission(player, slot)) {
    player.sendMessage("You don't have permission to use this kit slot!");
    return;
}
```

## Version Compatibility

- **API Version**: 2.0.0
- **Minimum Plugin Version**: 1.6.3+
- **Java Version**: 17+
- **Bukkit/Spigot**: 1.17+

## Migration from Legacy API

```java
// Old way (still works for backwards compatibility)
API legacyApi = API.getInstance();
List<PublicKit> publicKits = legacyApi.getPublicKits();

// New way
PerPlayerKitAPI api = PerPlayerKitAPI.getInstance();
api.kits()
   .forPlayer(player)
   .saveCurrentInventory(1)
   .thenRun(() -> player.sendMessage("Kit saved!"));
```
