# PerPlayerKit API v2.0.0 - Developer Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Core Concepts](#core-concepts)
4. [API Reference](#api-reference)
5. [Usage Examples](#usage-examples)
6. [Best Practices](#best-practices)
7. [Migration Guide](#migration-guide)
8. [Troubleshooting](#troubleshooting)

## Introduction

The PerPlayerKit API v2.0.0 is a comprehensive, modern interface for interacting with the PerPlayerKit Minecraft plugin. It provides developers with powerful tools to manage player kits, handle GUI interactions, store persistent data, and listen to plugin events.

### Key Features

- **🔒 Thread-Safe** - All operations are safe to call from any thread
- **⚡ Asynchronous** - Non-blocking operations using CompletableFuture
- **🎯 Type-Safe** - Full generic type support with null safety
- **🔗 Fluent APIs** - Chainable method calls for complex operations
- **📊 Event-Driven** - Modern event system with priority support
- **🔄 Backwards Compatible** - Works with existing integrations
- **📖 Well Documented** - Comprehensive JavaDoc and examples

### System Requirements

- **Java**: 17+
- **Bukkit/Spigot**: 1.17+
- **PerPlayerKit**: 1.6.3+

## Getting Started

### Adding PerPlayerKit as a Dependency

#### Maven
```xml
<repositories>
    <repository>
        <id>local-repo</id>
        <url>file://${project.basedir}/lib</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.noah</groupId>
        <artifactId>PerPlayerKit</artifactId>
        <version>1.6.3</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle
```groovy
repositories {
    flatDir {
        dirs 'lib'
    }
}

dependencies {
    compileOnly 'dev.noah:PerPlayerKit:1.6.3'
}
```

### Basic Setup

```java
public class MyPlugin extends JavaPlugin {
    
    private PerPlayerKitAPI perPlayerKitAPI;
    private APIRegistration apiRegistration;
    
    @Override
    public void onEnable() {
        // Check if PerPlayerKit is available
        if (!PerPlayerKitAPI.isAvailable()) {
            getLogger().severe("PerPlayerKit is not available!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get the API instance
        perPlayerKitAPI = PerPlayerKitAPI.getInstance();
        
        // Register your plugin (recommended)
        apiRegistration = perPlayerKitAPI.registerPlugin(this);
        
        getLogger().info("Connected to PerPlayerKit API v" + perPlayerKitAPI.getAPIVersion());
    }
    
    @Override
    public void onDisable() {
        // Clean up
        if (apiRegistration != null) {
            apiRegistration.unregister();
        }
    }
}
```

## Core Concepts

### 1. API Architecture

The API is organized into five main interfaces:

```java
PerPlayerKitAPI api = PerPlayerKitAPI.getInstance();

KitAPI kits = api.kits();                    // Kit management
PlayerAPI players = api.players();           // Player data & permissions
GuiAPI gui = api.gui();                     // GUI operations
PlayerDataAPI data = api.data();            // Persistent data storage
PerPlayerKitEventManager events = api.events(); // Event handling
```

### 2. Fluent Builders

Most operations support fluent builder patterns for complex operations:

```java
// Kit operations
api.kits()
   .forPlayer(player)
   .saveCurrentInventory(1)
   .thenRun(() -> player.sendMessage("Kit saved!"));

// Advanced kit builder
api.kits()
   .builder()
   .forPlayer(player)
   .inSlot(2)
   .withItems(customItems)
   .withValidation(true)
   .save();
```

### 3. Asynchronous Operations

All potentially blocking operations return CompletableFuture:

```java
// Chain async operations
api.kits().saveKit(playerId, slot, items)
   .thenCompose(v -> api.players().getStatistics(playerId))
   .thenAccept(stats -> updateDisplay(stats))
   .exceptionally(throwable -> {
       getLogger().severe("Operation failed: " + throwable.getMessage());
       return null;
   });
```

### 4. Event System

Modern event handling with priority support:

```java
// Listen to events with default priority
events.onKitSaved(event -> {
    Player player = event.getPlayer();
    getLogger().info(player.getName() + " saved a kit");
});

// Listen with specific priority
events.onKitLoading(EventPriority.HIGH, event -> {
    if (shouldBlockKitLoading(event.getPlayer())) {
        event.setCancelled(true);
    }
});
```

## API Reference

### KitAPI - Kit Management

The KitAPI provides comprehensive kit management functionality.

#### Key Methods

```java
// Player-specific operations
PlayerKitBuilder forPlayer(Player player)
PlayerKitBuilder forPlayer(UUID playerId)

// Direct operations
CompletableFuture<Optional<ItemStack[]>> getKit(UUID playerId, int slot)
CompletableFuture<Void> saveKit(UUID playerId, int slot, ItemStack[] items)
CompletableFuture<Void> deleteKit(UUID playerId, int slot)
boolean hasKit(UUID playerId, int slot)
Stream<Integer> getOccupiedSlots(UUID playerId)

// Advanced operations
CompletableFuture<Void> copyKit(UUID playerId, int fromSlot, int toSlot)
CompletableFuture<Void> swapKits(UUID playerId, int slot1, int slot2)
CompletableFuture<Optional<KitMetadata>> getKitMetadata(UUID playerId, int slot)

// Builder for complex operations
KitBuilder builder()
```

#### PlayerKitBuilder

Fluent interface for player-specific kit operations:

```java
PlayerKitBuilder builder = api.kits().forPlayer(player);

// Load and save operations
CompletableFuture<Boolean> loadKit(int slot)
CompletableFuture<Void> saveCurrentInventory(int slot)
CompletableFuture<Void> saveItems(int slot, ItemStack[] items)

// Query operations
boolean hasKit(int slot)
List<Integer> getOccupiedSlots()
int getKitCount()

// Management operations
CompletableFuture<Void> deleteKit(int slot)
CompletableFuture<Void> clearAllKits()
CompletableFuture<Void> copyKit(int fromSlot, int toSlot)
CompletableFuture<Void> swapKits(int slot1, int slot2)
```

### PlayerAPI - Player Management

Handles player permissions, statistics, and preferences.

#### Key Methods

```java
// Permission checks
boolean hasKitPermission(Player player, int slot)
CompletableFuture<Boolean> hasKitPermission(UUID playerId, int slot)
int getMaxKitSlots(Player player)
List<Integer> getAccessibleSlots(Player player)

// Statistics and data
CompletableFuture<PlayerStatistics> getStatistics(UUID playerId)
CompletableFuture<PlayerPreferences> getPreferences(UUID playerId)
CompletableFuture<Void> updatePreferences(UUID playerId, PlayerPreferences preferences)

// Player settings
boolean isAutoSaveEnabled(Player player)
void setAutoSaveEnabled(Player player, boolean enabled)
boolean areGuiSoundsEnabled(Player player)
void setGuiSoundsEnabled(Player player, boolean enabled)
String getPreferredGuiTheme(Player player)
void setPreferredGuiTheme(Player player, String theme)

// Utility operations
CompletableFuture<Void> resetPlayerData(UUID playerId)
CompletableFuture<Optional<Long>> getLastSeen(UUID playerId)

// Builder pattern
PlayerBuilder forPlayer(Player player)
PlayerBuilder forPlayer(UUID playerId)
```

### GuiAPI - GUI Management

Manages GUI interactions and custom GUI creation.

#### Key Methods

```java
// Open standard GUIs
void openMainGui(Player player)
void openMainGui(Player player, String theme)
void openKitPreview(Player player, int slot)
void openKitPreview(Player player, int slot, String theme)
void openSettingsGui(Player player)

// GUI state management
void closeGui(Player player)
void refreshGui(Player player)
boolean hasGuiOpen(Player player)
GuiType getOpenGuiType(Player player)

// Theme management
List<String> getAvailableThemes()
boolean isThemeAvailable(String theme)
String getDefaultTheme()
void setDefaultTheme(String theme)

// Custom GUI creation
GuiBuilder builder()

// Action handlers
void registerActionHandler(String actionId, GuiActionHandler handler)
void unregisterActionHandler(String actionId)

// System operations
CompletableFuture<Void> reloadConfigurations()
```

#### GuiBuilder

Create custom GUIs with fluent interface:

```java
CustomGui gui = api.gui().builder()
    .title("My Custom GUI")
    .size(27)  // Must be multiple of 9, max 54
    .theme("dark")
    .item(13, myItem, context -> {
        // Handle click
        Player player = context.getPlayer();
        player.sendMessage("Clicked item!");
    })
    .fillEmpty(new ItemStack(Material.GRAY_STAINED_GLASS_PANE))
    .onClick(context -> {
        // Global click handler
    })
    .onClose(player -> {
        // Handle GUI close
    })
    .allowMove(false)
    .autoRefresh(20)  // Refresh every second
    .build();

// Open the GUI
gui.open(player);
```

### PlayerDataAPI - Data Storage

Persistent data storage system for custom plugin data.

#### Key Methods

```java
// Basic operations
CompletableFuture<Void> setData(UUID playerId, String key, Object value)
CompletableFuture<Optional<Object>> getData(UUID playerId, String key)
CompletableFuture<Optional<T>> getData(UUID playerId, String key, Class<T> type)
CompletableFuture<T> getDataOrDefault(UUID playerId, String key, T defaultValue)
CompletableFuture<Void> removeData(UUID playerId, String key)
CompletableFuture<Boolean> hasData(UUID playerId, String key)

// Bulk operations
CompletableFuture<List<String>> getDataKeys(UUID playerId)
CompletableFuture<Map<String, Object>> getAllData(UUID playerId)
CompletableFuture<Map<String, Object>> getDataByPrefix(UUID playerId, String prefix)
CompletableFuture<Void> clearAllData(UUID playerId)
CompletableFuture<Void> clearDataByPrefix(UUID playerId, String prefix)

// Data management
CompletableFuture<Void> copyData(UUID fromPlayerId, UUID toPlayerId, boolean overwrite)
CompletableFuture<Void> copyData(UUID fromPlayerId, UUID toPlayerId, List<String> keys, boolean overwrite)

// Query operations
CompletableFuture<Integer> getPlayerCount()
CompletableFuture<List<UUID>> getAllPlayerIds()
CompletableFuture<List<UUID>> getPlayersWithKey(String key)
CompletableFuture<List<UUID>> getPlayersWithValue(String key, Object value)

// Batch operations
CompletableFuture<Void> batchOperation(List<DataOperation> operations)
DataBuilder forPlayer(UUID playerId)
```

#### DataBuilder

Batch data operations:

```java
api.data().forPlayer(playerId)
    .set("score", 1000)
    .set("level", 5)
    .set("last_login", System.currentTimeMillis())
    .remove("old_key")
    .clearPrefix("temporary.")
    .apply()  // Execute all operations
    .thenRun(() -> getLogger().info("Batch operations completed"));
```

### PerPlayerKitEventManager - Event System

Modern event handling with priority support and fluent registration.

#### Event Types

```java
// Kit events
EventRegistration onKitSaved(Consumer<KitSavedEvent> listener)
EventRegistration onKitSaving(Consumer<KitSavingEvent> listener)  // Cancellable
EventRegistration onKitLoaded(Consumer<KitLoadedEvent> listener)
EventRegistration onKitLoading(Consumer<KitLoadingEvent> listener)  // Cancellable
EventRegistration onKitDeleted(Consumer<KitDeletedEvent> listener)

// GUI events
EventRegistration onGuiOpened(Consumer<GuiOpenedEvent> listener)
EventRegistration onGuiClosed(Consumer<GuiClosedEvent> listener)

// Data events
EventRegistration onPlayerDataChanged(Consumer<PlayerDataChangedEvent> listener)
```

#### Event Priorities

```java
public enum EventPriority {
    LOWEST,   // Last to execute, can see all changes
    LOW,      // Low priority
    NORMAL,   // Default priority
    HIGH,     // High priority
    HIGHEST   // First to execute
}
```

## Usage Examples

### Basic Kit Management

```java
public void savePlayerKit(Player player, int slot) {
    api.kits()
       .forPlayer(player)
       .saveCurrentInventory(slot)
       .thenRun(() -> {
           player.sendMessage("§aKit saved in slot " + slot + "!");
       })
       .exceptionally(throwable -> {
           player.sendMessage("§cFailed to save kit: " + throwable.getMessage());
           getLogger().severe("Kit save failed for " + player.getName(), throwable);
           return null;
       });
}

public void loadPlayerKit(Player player, int slot) {
    // Check permission first
    if (!api.players().hasKitPermission(player, slot)) {
        player.sendMessage("§cYou don't have permission to use slot " + slot + "!");
        return;
    }
    
    api.kits()
       .forPlayer(player)
       .loadKit(slot)
       .thenAccept(success -> {
           if (success) {
               player.sendMessage("§aKit loaded from slot " + slot + "!");
           } else {
               player.sendMessage("§cNo kit found in slot " + slot + "!");
           }
       });
}
```

### Advanced Kit Operations

```java
public void setupPlayerKits(Player player) {
    KitAPI kits = api.kits();
    
    // Get all occupied slots
    List<Integer> occupiedSlots = kits.forPlayer(player).getOccupiedSlots();
    player.sendMessage("§7You have kits in slots: §e" + occupiedSlots);
    
    // Copy kit 1 to kit 2, then swap them
    kits.forPlayer(player)
        .copyKit(1, 2)
        .thenCompose(v -> kits.forPlayer(player).swapKits(1, 2))
        .thenRun(() -> {
            player.sendMessage("§aKit operations completed!");
        });
    
    // Advanced builder usage
    ItemStack[] customItems = createCustomItems();
    kits.builder()
        .forPlayer(player)
        .inSlot(5)
        .withItems(customItems)
        .withValidation(true)
        .withNotification(true)
        .save()
        .thenRun(() -> {
            getLogger().info("Custom kit saved for " + player.getName());
        });
}
```

### Event Handling

```java
public void setupEventListeners() {
    PerPlayerKitEventManager events = api.events();
    
    // Basic event listening
    events.onKitSaved(event -> {
        Player player = event.getPlayer();
        int slot = event.getSlot();
        boolean wasOverwrite = event.wasOverwrite();
        
        String message = wasOverwrite ? "Kit updated" : "Kit saved";
        player.sendMessage("§a" + message + " in slot " + slot + "!");
        
        // Award points for saving kits
        awardPoints(player, 10);
    });
    
    // Cancellable event with priority
    events.onKitLoading(EventPriority.HIGH, event -> {
        Player player = event.getPlayer();
        int slot = event.getSlot();
        
        // Check cooldown
        if (isOnCooldown(player, "kit_load")) {
            event.setCancelled(true);
            player.sendMessage("§cYou must wait before loading another kit!");
            return;
        }
        
        // Check special conditions
        if (slot == 9 && !player.hasPermission("special.kit.9")) {
            event.setCancelled(true);
            player.sendMessage("§cSlot 9 is reserved for VIP players!");
            return;
        }
        
        // Set cooldown
        setCooldown(player, "kit_load", 30); // 30 seconds
    });
    
    // GUI events
    events.onGuiOpened(event -> {
        Player player = event.getPlayer();
        GuiType type = event.getGuiType();
        
        // Track GUI usage
        api.data().setData(player.getUniqueId(), "stats.gui_opens", 
            api.data().getDataOrDefault(player.getUniqueId(), "stats.gui_opens", 0).join() + 1);
            
        getLogger().info(player.getName() + " opened " + type + " GUI");
    });
}
```

### Custom GUI Creation

```java
public void openCustomKitSelector(Player player) {
    GuiAPI gui = api.gui();
    
    gui.builder()
       .title("§6Kit Selector")
       .size(45) // 5 rows
       .theme("dark")
       // Add kit slots
       .item(10, createKitIcon(1), context -> loadKit(context.getPlayer(), 1))
       .item(11, createKitIcon(2), context -> loadKit(context.getPlayer(), 2))
       .item(12, createKitIcon(3), context -> loadKit(context.getPlayer(), 3))
       // Add navigation
       .item(40, createCloseIcon(), context -> context.getPlayer().closeInventory())
       .item(38, createSettingsIcon(), context -> openSettings(context.getPlayer()))
       // Fill empty slots
       .fillEmpty(new ItemStack(Material.GRAY_STAINED_GLASS_PANE))
       // Global click handler
       .onClick(context -> {
           // Play sound for all clicks
           if (api.players().areGuiSoundsEnabled(context.getPlayer())) {
               context.getPlayer().playSound(context.getPlayer().getLocation(), 
                   Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
           }
       })
       // Handle close
       .onClose(player -> {
           player.sendMessage("§7GUI closed");
       })
       .allowMove(false)
       .autoRefresh(20) // Refresh every second
       .build()
       .open(player);
}

private ItemStack createKitIcon(int slot) {
    Material material = api.kits().forPlayer(player).hasKit(slot) ? 
        Material.CHEST : Material.BARRIER;
    
    return new ItemBuilder(material)
        .name("§6Kit " + slot)
        .lore(api.kits().forPlayer(player).hasKit(slot) ? 
            Arrays.asList("§7Click to load", "§7Right-click to preview") :
            Arrays.asList("§7No kit saved", "§7Click to save current inventory"))
        .build();
}
```

### Player Data Management

```java
public void managePlayerData(Player player) {
    PlayerDataAPI data = api.data();
    UUID playerId = player.getUniqueId();
    
    // Store various data types
    data.setData(playerId, "stats.deaths", 5)
        .thenCompose(v -> data.setData(playerId, "settings.auto_save", true))
        .thenCompose(v -> data.setData(playerId, "last_kit_used", 3))
        .thenRun(() -> {
            player.sendMessage("§aData saved!");
        });
    
    // Retrieve data with type safety
    data.getData(playerId, "stats.deaths", Integer.class)
        .thenAccept(deaths -> {
            if (deaths.isPresent()) {
                player.sendMessage("§7Deaths: §c" + deaths.get());
            }
        });
    
    // Use defaults for missing data
    data.getDataOrDefault(playerId, "stats.kills", 0)
        .thenAccept(kills -> {
            player.sendMessage("§7Kills: §a" + kills);
        });
    
    // Batch operations for efficiency
    data.forPlayer(playerId)
        .set("session.login_time", System.currentTimeMillis())
        .set("session.server", getServer().getName())
        .set("session.world", player.getWorld().getName())
        .remove("temporary.old_data")
        .clearPrefix("cache.")
        .apply()
        .thenRun(() -> {
            getLogger().info("Session data updated for " + player.getName());
        });
}
```

### Player Statistics and Preferences

```java
public void showPlayerStats(Player player) {
    PlayerAPI players = api.players();
    UUID playerId = player.getUniqueId();
    
    // Get player statistics
    players.getStatistics(playerId)
           .thenAccept(stats -> {
               player.sendMessage("§6=== Your Kit Statistics ===");
               player.sendMessage("§7Kits Saved: §e" + stats.getKitsSaved());
               player.sendMessage("§7Kits Loaded: §e" + stats.getKitsLoaded());
               player.sendMessage("§7GUI Opens: §e" + stats.getGuiOpens());
               player.sendMessage("§7Total Play Time: §e" + formatTime(stats.getTotalPlayTime()));
               
               stats.getFirstJoin().ifPresent(time -> 
                   player.sendMessage("§7First Join: §e" + formatDate(time)));
           });
    
    // Show accessible slots
    players.getAccessibleSlots(playerId)
           .thenAccept(slots -> {
               player.sendMessage("§7Accessible Slots: §e" + slots);
               player.sendMessage("§7Max Slots: §e" + players.getMaxKitSlots(player));
           });
    
    // Show preferences
    players.getPreferences(playerId)
           .thenAccept(prefs -> {
               player.sendMessage("§6=== Your Preferences ===");
               player.sendMessage("§7Auto Save: §e" + (prefs.isAutoSaveEnabled() ? "Enabled" : "Disabled"));
               player.sendMessage("§7GUI Sounds: §e" + (prefs.areGuiSoundsEnabled() ? "Enabled" : "Disabled"));
               player.sendMessage("§7Theme: §e" + (prefs.getPreferredGuiTheme() != null ? 
                   prefs.getPreferredGuiTheme() : "Default"));
           });
}

public void updatePlayerPreferences(Player player, String theme, boolean autoSave) {
    PlayerAPI players = api.players();
    
    // Use the builder pattern for preferences
    players.forPlayer(player)
           .withGuiTheme(theme)
           .withAutoSave(autoSave)
           .withGuiSounds(true)
           .apply()
           .thenRun(() -> {
               player.sendMessage("§aPreferences updated!");
           });
}
```

## Best Practices

### 1. Always Check API Availability

```java
@Override
public void onEnable() {
    if (!PerPlayerKitAPI.isAvailable()) {
        getLogger().severe("PerPlayerKit is required but not available!");
        getServer().getPluginManager().disablePlugin(this);
        return;
    }
    
    // Continue with initialization
}
```

### 2. Register Your Plugin

```java
// This helps with debugging and support
APIRegistration registration = api.registerPlugin(this);

// Clean up on disable
@Override
public void onDisable() {
    if (registration != null) {
        registration.unregister();
    }
}
```

### 3. Handle Async Operations Properly

```java
// Good: Chain operations
api.kits().saveKit(playerId, slot, items)
   .thenCompose(v -> api.players().getStatistics(playerId))
   .thenAccept(stats -> updateDisplay(stats))
   .exceptionally(throwable -> {
       handleError(throwable);
       return null;
   });

// Avoid: Blocking operations
CompletableFuture<Void> future = api.kits().saveKit(playerId, slot, items);
future.join(); // This blocks the thread - avoid!
```

### 4. Use Batch Operations

```java
// Good: Batch operations
api.data().forPlayer(playerId)
    .set("key1", value1)
    .set("key2", value2)
    .set("key3", value3)
    .apply(); // Single database transaction

// Avoid: Individual operations
api.data().setData(playerId, "key1", value1);
api.data().setData(playerId, "key2", value2);
api.data().setData(playerId, "key3", value3);
// This creates multiple database transactions
```

### 5. Validate Input Parameters

```java
public void loadKit(Player player, int slot) {
    // Validate slot number
    if (!api.kits().isValidSlot(slot)) {
        player.sendMessage("§cInvalid slot number: " + slot);
        return;
    }
    
    // Check permissions
    if (!api.players().hasKitPermission(player, slot)) {
        player.sendMessage("§cYou don't have permission for slot " + slot);
        return;
    }
    
    // Proceed with operation
    api.kits().forPlayer(player).loadKit(slot)
       .thenAccept(success -> {
           // Handle result
       });
}
```

### 6. Use Event Priorities Appropriately

```java
// High priority for validation/cancellation
events.onKitSaving(EventPriority.HIGH, this::validateKitSaving);

// Normal priority for business logic
events.onKitSaved(EventPriority.NORMAL, this::handleKitSaved);

// Low priority for cleanup/logging
events.onKitDeleted(EventPriority.LOW, this::logKitDeletion);
```

### 7. Implement Proper Error Handling

```java
public void saveKitSafely(Player player, int slot) {
    api.kits().forPlayer(player)
       .saveCurrentInventory(slot)
       .thenRun(() -> {
           player.sendMessage("§aKit saved successfully!");
       })
       .exceptionally(throwable -> {
           // Log the full error for debugging
           getLogger().severe("Failed to save kit for " + player.getName() + 
               " in slot " + slot, throwable);
           
           // Send user-friendly message
           player.sendMessage("§cFailed to save kit. Please try again or contact an administrator.");
           
           // Handle specific error types
           if (throwable.getCause() instanceof SecurityException) {
               player.sendMessage("§cSecurity error: You may not have permission to save this kit.");
           } else if (throwable.getCause() instanceof IllegalArgumentException) {
               player.sendMessage("§cInvalid kit data. Please check your inventory.");
           }
           
           return null;
       });
}
```

### 8. Use Type-Safe Data Operations

```java
// Good: Type-safe operations
api.data().getData(playerId, "score", Integer.class)
   .thenAccept(score -> {
       if (score.isPresent()) {
           int actualScore = score.get(); // Safe cast
           // Use the score
       }
   });

// With defaults
api.data().getDataOrDefault(playerId, "level", 1)
   .thenAccept(level -> {
       // level is guaranteed to be non-null
   });
```

## Migration Guide

### From Legacy API (v1.x)

The old API used simple static methods:

```java
// Old API (v1.x)
API api = API.getInstance();
List<PublicKit> publicKits = api.getPublicKits();
api.loadPublicKit(player, publicKits.get(0));
```

The new API provides much more functionality:

```java
// New API (v2.0.0)
PerPlayerKitAPI api = PerPlayerKitAPI.getInstance();

// Kit operations
api.kits()
   .forPlayer(player)
   .saveCurrentInventory(1)
   .thenRun(() -> player.sendMessage("Kit saved!"));

// Event handling
api.events().onKitSaved(event -> {
    // Handle kit saved
});
```

### Migration Steps

1. **Update Dependencies**: Change your dependency to use the new API version
2. **Update Imports**: Change imports from old API to new API packages
3. **Replace Static Calls**: Replace static API calls with fluent API calls
4. **Handle Async Operations**: Update code to use CompletableFuture properly
5. **Update Event Handling**: Migrate to new event system if needed

### Backwards Compatibility

The new API includes a backwards compatibility layer, so existing plugins will continue to work without changes. However, it's recommended to migrate to the new API for better features and performance.

## Troubleshooting

### Common Issues

#### 1. API Not Available
```java
// Problem
PerPlayerKitAPI api = PerPlayerKitAPI.getInstance(); // Throws exception

// Solution
if (!PerPlayerKitAPI.isAvailable()) {
    getLogger().warning("PerPlayerKit is not available");
    return;
}
PerPlayerKitAPI api = PerPlayerKitAPI.getInstance();
```

#### 2. Permission Errors
```java
// Problem: Player can't use kit
api.kits().forPlayer(player).loadKit(1); // Fails silently

// Solution: Check permissions first
if (!api.players().hasKitPermission(player, 1)) {
    player.sendMessage("No permission for slot 1");
    return;
}
api.kits().forPlayer(player).loadKit(1);
```

#### 3. Async Operation Errors
```java
// Problem: Exception not handled
api.kits().saveKit(playerId, slot, items); // Exceptions lost

// Solution: Handle exceptions
api.kits().saveKit(playerId, slot, items)
   .exceptionally(throwable -> {
       getLogger().severe("Save failed", throwable);
       return null;
   });
```

#### 4. Data Type Errors
```java
// Problem: ClassCastException
Object data = api.data().getData(playerId, "score").join().orElse(null);
int score = (int) data; // Unsafe cast

// Solution: Use type-safe methods
int score = api.data().getData(playerId, "score", Integer.class)
    .join().orElse(0); // Safe with default
```

### Debug Mode

Enable debug logging for detailed API operation information:

```java
// Check API statistics
APIStatistics stats = api.getStatistics();
getLogger().info("Total API calls: " + stats.getTotalAPICalls());
getLogger().info("Registered plugins: " + stats.getRegisteredPluginCount());

// Monitor API health
if (!api.isReady()) {
    getLogger().warning("PerPlayerKit API is not ready");
}
```

### Getting Help

1. **Check the Logs**: Enable debug logging and check for error messages
2. **Validate Input**: Ensure slot numbers are valid (1-9) and players have permissions
3. **Handle Async Properly**: Make sure CompletableFuture operations are handled correctly
4. **Report Issues**: Submit bug reports with logs and reproduction steps

### Performance Tips

1. **Use Batch Operations**: Combine multiple data operations into batches
2. **Cache Permission Checks**: Cache permission results for frequently checked operations
3. **Avoid Blocking**: Never call `.join()` or `.get()` on CompletableFuture in main thread
4. **Monitor API Usage**: Use API statistics to identify performance bottlenecks

---

## Conclusion

The PerPlayerKit API v2.0.0 provides a powerful, modern interface for interacting with player kits and data. By following the patterns and best practices outlined in this guide, you can create robust integrations that take full advantage of the API's capabilities while maintaining good performance and reliability.

For additional examples and advanced usage patterns, see the included `ExamplePlugin.java` file and the comprehensive JavaDoc documentation in the API interfaces.