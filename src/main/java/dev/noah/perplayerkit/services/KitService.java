/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.services;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.config.ConfigurationManager;
import org.bukkit.plugin.java.JavaPlugin;
import dev.noah.perplayerkit.exceptions.KitException;
import dev.noah.perplayerkit.logging.PerPlayerKitLogger;
import dev.noah.perplayerkit.metrics.MetricsCollector;
import dev.noah.perplayerkit.util.AdventureCompat;
import dev.noah.perplayerkit.util.FoliaCompatScheduler;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import dev.noah.perplayerkit.validation.Validator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Modern kit management service with improved error handling, validation, and async operations.
 * Replaces the legacy KitManager with modern Java patterns and better separation of concerns.
 */
public class KitService {
    
    private final JavaPlugin plugin;
    private final PerPlayerKitLogger logger;
    private final ConfigurationManager configManager;
    private final MetricsCollector metrics;
    private final ConcurrentHashMap<String, ItemStack[]> kitCache;
    
    public KitService(@NotNull JavaPlugin plugin, 
                     @NotNull PerPlayerKitLogger logger, 
                     @NotNull ConfigurationManager configManager,
                     @NotNull MetricsCollector metrics) {
        this.plugin = Validator.requireNonNull(plugin, "plugin");
        this.logger = Validator.requireNonNull(logger, "logger");
        this.configManager = Validator.requireNonNull(configManager, "configManager");
        this.metrics = Validator.requireNonNull(metrics, "metrics");
        this.kitCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Saves a kit for a player asynchronously.
     *
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @param items the items to save
     * @return a CompletableFuture that completes when the kit is saved
     * @throws KitException if the operation fails
     */
    @NotNull
    public CompletableFuture<Void> saveKitAsync(@NotNull UUID playerId, int slot, @NotNull ItemStack[] items) {
        // Validate inputs
        Validator.requireValidUuid(playerId, "playerId");
        Validator.requireValidKitSlot(slot);
        Validator.requireNonEmptyKit(items);
        
        return CompletableFuture.runAsync(() -> {
            try (var timer = metrics.startTimer("kit.save")) {
                try {
                    // Validate armor slots
                    ItemStack[] validatedItems = validateAndFilterArmorSlots(items);
                    
                    // Generate kit ID and cache the kit
                    String kitId = IDUtil.getPlayerKitId(playerId, slot);
                    kitCache.put(kitId, validatedItems);
                    
                    // Save to database
                    saveKitToDatabase(kitId, validatedItems);
                    
                    // Update metrics
                    metrics.incrementCounter("kits.saved");
                    
                    // Notify player if online
                    getPlayerById(playerId).ifPresent(player -> 
                        AdventureCompat.sendMessage(player, "&aKit " + slot + " saved successfully!"));
                    
                    logger.info("Kit saved successfully", playerId);
                    
                } catch (Exception e) {
                    metrics.incrementCounter("kits.save.errors");
                    throw new KitException("Failed to save kit", e, playerId, slot);
                }
            }
        });
    }
    
    /**
     * Loads a kit for a player asynchronously.
     *
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return a CompletableFuture containing the kit items, or empty if not found
     */
    @NotNull
    public CompletableFuture<Optional<ItemStack[]>> loadKitAsync(@NotNull UUID playerId, int slot) {
        Validator.requireValidUuid(playerId, "playerId");
        Validator.requireValidKitSlot(slot);
        
        return CompletableFuture.supplyAsync(() -> {
            try (var timer = metrics.startTimer("kit.load")) {
                String kitId = IDUtil.getPlayerKitId(playerId, slot);
                
                // Check cache first
                ItemStack[] cached = kitCache.get(kitId);
                if (cached != null) {
                    metrics.incrementCounter("kits.cache.hits");
                    return Optional.of(cached.clone()); // Return a copy to prevent mutations
                }
                
                metrics.incrementCounter("kits.cache.misses");
                
                // Load from database
                return loadKitFromDatabase(kitId)
                    .map(items -> {
                        kitCache.put(kitId, items); // Cache the loaded kit
                        metrics.incrementCounter("kits.loaded");
                        logger.debug("Kit loaded from database", playerId);
                        return items.clone(); // Return a copy
                    });
            }
        });
    }
    
    /**
     * Checks if a player has a kit in the specified slot.
     *
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return true if the player has a kit in the slot
     */
    public boolean hasKit(@NotNull UUID playerId, int slot) {
        Validator.requireValidUuid(playerId, "playerId");
        Validator.requireValidKitSlot(slot);
        
        String kitId = IDUtil.getPlayerKitId(playerId, slot);
        
        // Check cache first
        if (kitCache.containsKey(kitId)) {
            return true;
        }
        
        // Check database (this is a quick operation)
        return loadKitFromDatabase(kitId).isPresent();
    }
    
    /**
     * Deletes a kit asynchronously.
     *
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return a CompletableFuture that completes when the kit is deleted
     */
    @NotNull
    public CompletableFuture<Void> deleteKitAsync(@NotNull UUID playerId, int slot) {
        Validator.requireValidUuid(playerId, "playerId");
        Validator.requireValidKitSlot(slot);
        
        return CompletableFuture.runAsync(() -> {
            try (var timer = metrics.startTimer("kit.delete")) {
                String kitId = IDUtil.getPlayerKitId(playerId, slot);
                
                // Remove from cache
                kitCache.remove(kitId);
                
                // Delete from database
                FoliaCompatScheduler.runAsync(plugin, () -> 
                    PerPlayerKit.storageManager.deleteKitByID(kitId));
                
                metrics.incrementCounter("kits.deleted");
                
                // Notify player if online
                getPlayerById(playerId).ifPresent(player -> 
                    AdventureCompat.sendMessage(player, "&cKit " + slot + " deleted."));
                
                logger.info("Kit deleted successfully", playerId);
            }
        });
    }
    
    /**
     * Gets all kit slots that contain kits for a player.
     *
     * @param playerId the player's UUID
     * @return a stream of slot numbers that contain kits
     */
    @NotNull
    public Stream<Integer> getOccupiedSlots(@NotNull UUID playerId) {
        Validator.requireValidUuid(playerId, "playerId");
        
        return Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
            .filter(slot -> hasKit(playerId, slot));
    }
    
    /**
     * Loads all kits for a player from the database asynchronously.
     *
     * @param playerId the player's UUID
     * @return a CompletableFuture that completes when all kits are loaded
     */
    @NotNull
    public CompletableFuture<Void> loadAllPlayerKitsAsync(@NotNull UUID playerId) {
        Validator.requireValidUuid(playerId, "playerId");
        
        return CompletableFuture.runAsync(() -> {
            try (var timer = metrics.startTimer("kits.load_all")) {
                for (int slot = 1; slot <= 9; slot++) {
                    String kitId = IDUtil.getPlayerKitId(playerId, slot);
                    loadKitFromDatabase(kitId).ifPresent(items -> 
                        kitCache.put(kitId, items));
                }
                
                logger.debug("All kits loaded for player", playerId);
            }
        });
    }
    
    /**
     * Clears all cached kits for a player (useful when player disconnects).
     *
     * @param playerId the player's UUID
     */
    public void clearPlayerCache(@NotNull UUID playerId) {
        Validator.requireValidUuid(playerId, "playerId");
        
        for (int slot = 1; slot <= 9; slot++) {
            String kitId = IDUtil.getPlayerKitId(playerId, slot);
            kitCache.remove(kitId);
        }
        
        logger.debug("Player kit cache cleared", playerId);
    }
    
    /**
     * Gets cache statistics.
     *
     * @return cache size
     */
    public int getCacheSize() {
        return kitCache.size();
    }
    
    /**
     * Clears the entire kit cache.
     */
    public void clearCache() {
        kitCache.clear();
        logger.info("Kit cache cleared");
    }
    
    /**
     * Validates and filters armor slots according to configuration rules.
     *
     * @param items the items to validate
     * @return the validated items
     */
    @NotNull
    private ItemStack[] validateAndFilterArmorSlots(@NotNull ItemStack[] items) {
        ItemStack[] validated = items.clone();
        
        // Validate armor slots (36-39 are armor slots)
        if (validated.length > 36) {
            // Validate boots slot (36)
            if (validated[36] != null && !validated[36].getType().toString().contains("BOOTS")) {
                validated[36] = null;
            }
            // Validate leggings slot (37)
            if (validated.length > 37 && validated[37] != null && 
                !validated[37].getType().toString().contains("LEGGINGS")) {
                validated[37] = null;
            }
            // Validate chestplate slot (38)
            if (validated.length > 38 && validated[38] != null && 
                !validated[38].getType().toString().contains("CHESTPLATE")) {
                validated[38] = null;
            }
            // Validate helmet slot (39)
            if (validated.length > 39 && validated[39] != null && 
                !validated[39].getType().toString().contains("HELMET")) {
                validated[39] = null;
            }
        }
        
        return validated;
    }
    
    /**
     * Saves a kit to the database.
     *
     * @param kitId the kit ID
     * @param items the items to save
     */
    private void saveKitToDatabase(@NotNull String kitId, @NotNull ItemStack[] items) {
        try {
            String serializedKit = Serializer.itemStackArrayToBase64(items);
            PerPlayerKit.storageManager.saveKitDataByID(kitId, serializedKit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save kit to database: " + kitId, e);
        }
    }
    
    /**
     * Loads a kit from the database.
     *
     * @param kitId the kit ID
     * @return the loaded kit, or empty if not found
     */
    @NotNull
    private Optional<ItemStack[]> loadKitFromDatabase(@NotNull String kitId) {
        try {
            String data = PerPlayerKit.storageManager.getKitDataByID(kitId);
            if ("error".equalsIgnoreCase(data)) {
                return Optional.empty();
            }
            
            ItemStack[] items = Serializer.itemStackArrayFromBase64(data);
            return Optional.of(items);
        } catch (Exception e) {
            logger.warn("Failed to load kit from database: " + kitId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Gets an online player by UUID.
     *
     * @param playerId the player's UUID
     * @return the player if online, otherwise empty
     */
    @NotNull
    private Optional<Player> getPlayerById(@NotNull UUID playerId) {
        return Optional.ofNullable(plugin.getServer().getPlayer(playerId))
            .filter(Player::isOnline);
    }
}