/*
 * Example plugin showing how to integrate with PerPlayerKit API v2.0.0
 * 
 * This example demonstrates modern API usage patterns including:
 * - Fluent kit operations
 * - Event handling
 * - Player management
 * - Async operations
 * - Error handling
 */
package com.example;

import dev.noah.perplayerkit.api.PerPlayerKitAPI;
import dev.noah.perplayerkit.api.events.PerPlayerKitEventManager;
import dev.noah.perplayerkit.api.kit.KitAPI;
import dev.noah.perplayerkit.api.player.PlayerAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin implements Listener {
    
    private PerPlayerKitAPI perPlayerKitAPI;
    private PerPlayerKitAPI.APIRegistration apiRegistration;
    
    @Override
    public void onEnable() {
        // Check if PerPlayerKit is available
        if (!PerPlayerKitAPI.isAvailable()) {
            getLogger().severe("PerPlayerKit is not available! Disabling plugin.");
            getPluginLoader().disablePlugin(this);
            return;
        }
        
        // Get the API instance
        perPlayerKitAPI = PerPlayerKitAPI.getInstance();
        
        // Register our plugin with the API (recommended for better support)
        apiRegistration = perPlayerKitAPI.registerPlugin(this);
        getLogger().info("Registered with PerPlayerKit API v" + perPlayerKitAPI.getAPIVersion());
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        
        // Set up PerPlayerKit event listeners
        setupPerPlayerKitEvents();
        
        getLogger().info("ExamplePlugin enabled with PerPlayerKit integration!");
    }
    
    @Override
    public void onDisable() {
        // Unregister from the API
        if (apiRegistration != null) {
            apiRegistration.unregister();
        }
    }
    
    /**
     * Set up event listeners for PerPlayerKit events
     */
    private void setupPerPlayerKitEvents() {
        PerPlayerKitEventManager events = perPlayerKitAPI.events();
        
        // Listen to kit saved events
        events.onKitSaved(event -> {
            Player player = event.getPlayer();
            int slot = event.getSlot();
            getLogger().info(player.getName() + " saved a kit in slot " + slot);
            
            // Send a congratulatory message
            player.sendMessage("§aGreat! You saved your kit in slot " + slot + "!");
        });
        
        // Listen to kit loaded events
        events.onKitLoaded(event -> {
            Player player = event.getPlayer();
            int slot = event.getSlot();
            getLogger().info(player.getName() + " loaded kit from slot " + slot);
        });
        
        // Listen to kit loading events (cancellable) - example: prevent loading during combat
        events.onKitLoading(PerPlayerKitEventManager.EventPriority.HIGH, event -> {
            Player player = event.getPlayer();
            
            // Example: Check if player is in combat (you'd implement this check)
            if (isPlayerInCombat(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot load kits while in combat!");
            }
        });
    }
    
    /**
     * Example: Give new players a starter kit
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if this is a new player
        if (!player.hasPlayedBefore()) {
            // Give them a starter kit after a delay
            getServer().getScheduler().runTaskLater(this, () -> {
                giveStarterKit(player);
            }, 40L); // 2 seconds delay
        }
        
        // Show player their kit information
        showPlayerKitInfo(player);
    }
    
    /**
     * Give a new player a starter kit
     */
    private void giveStarterKit(Player player) {
        KitAPI kits = perPlayerKitAPI.kits();
        
        // Check if player has permission for slot 1
        if (!perPlayerKitAPI.players().hasKitPermission(player, 1)) {
            player.sendMessage("§cYou don't have permission to use kit slot 1!");
            return;
        }
        
        // Save their current (empty) inventory as a starter kit
        kits.forPlayer(player)
            .saveCurrentInventory(1)
            .thenRun(() -> {
                player.sendMessage("§aWelcome! Your starter kit has been saved in slot 1.");
                player.sendMessage("§7Use /kit to manage your kits!");
            })
            .exceptionally(throwable -> {
                getLogger().severe("Failed to save starter kit for " + player.getName() + ": " + throwable.getMessage());
                player.sendMessage("§cFailed to save your starter kit. Please contact an administrator.");
                return null;
            });
    }
    
    /**
     * Show player information about their kits
     */
    private void showPlayerKitInfo(Player player) {
        PlayerAPI players = perPlayerKitAPI.players();
        KitAPI kits = perPlayerKitAPI.kits();
        
        // Get accessible slots
        players.getAccessibleSlots(player.getUniqueId())
            .thenAccept(accessibleSlots -> {
                player.sendMessage("§7You can access kit slots: §e" + accessibleSlots);
                
                // Show which slots have kits
                var occupiedSlots = kits.forPlayer(player).getOccupiedSlots();
                if (!occupiedSlots.isEmpty()) {
                    player.sendMessage("§7You have kits saved in slots: §a" + occupiedSlots);
                } else {
                    player.sendMessage("§7You don't have any kits saved yet. Use /kit to get started!");
                }
            });
        
        // Get player statistics
        players.getStatistics(player.getUniqueId())
            .thenAccept(stats -> {
                player.sendMessage("§7Your kit stats: §e" + stats.getKitsSaved() + " saved, " + 
                    stats.getKitsLoaded() + " loaded");
            });
    }
    
    /**
     * Example method to demonstrate advanced kit operations
     */
    public void demonstrateAdvancedKitOperations(Player player) {
        KitAPI kits = perPlayerKitAPI.kits();
        
        // Advanced builder pattern example
        kits.builder()
            .forPlayer(player)
            .inSlot(5)
            .withItems(player.getInventory().getContents())
            .withValidation(true)
            .withNotification(true)
            .save()
            .thenRun(() -> {
                getLogger().info("Advanced kit saved for " + player.getName());
            });
        
        // Chain multiple operations
        kits.forPlayer(player)
            .copyKit(1, 2)  // Copy kit from slot 1 to slot 2
            .thenCompose(v -> kits.forPlayer(player).swapKits(2, 3))  // Then swap slots 2 and 3
            .thenRun(() -> {
                player.sendMessage("§aKit operations completed!");
            })
            .exceptionally(throwable -> {
                player.sendMessage("§cKit operations failed: " + throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Example method to demonstrate player data API
     */
    public void demonstratePlayerDataAPI(Player player) {
        var data = perPlayerKitAPI.data();
        
        // Store custom data
        data.setData(player.getUniqueId(), "example.last_kit_used", 3)
            .thenCompose(v -> data.setData(player.getUniqueId(), "example.total_kit_uses", 1))
            .thenRun(() -> {
                getLogger().info("Custom data saved for " + player.getName());
            });
        
        // Retrieve and use data
        data.getData(player.getUniqueId(), "example.last_kit_used", Integer.class)
            .thenAccept(lastKit -> {
                if (lastKit.isPresent()) {
                    player.sendMessage("§7Your last used kit was slot " + lastKit.get());
                }
            });
        
        // Use data builder for batch operations
        data.forPlayer(player.getUniqueId())
            .set("example.login_count", 1)
            .set("example.last_login", System.currentTimeMillis())
            .set("example.preferred_theme", "dark")
            .apply()
            .thenRun(() -> {
                getLogger().info("Batch data operations completed for " + player.getName());
            });
    }
    
    /**
     * Example method - you would implement actual combat detection logic
     */
    private boolean isPlayerInCombat(Player player) {
        // This is just an example - implement your own combat detection
        return false;
    }
}