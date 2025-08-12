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
package dev.noah.perplayerkit.api.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * API for managing player kits.
 * 
 * This interface provides comprehensive kit management functionality with both
 * synchronous and asynchronous operations. All methods are thread-safe.
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * KitAPI kits = PerPlayerKitAPI.getInstance().kits();
 * 
 * // Fluent API for player-specific operations
 * kits.forPlayer(player)
 *     .saveCurrentInventory(1)
 *     .thenCompose(v -> kits.forPlayer(player).loadKit(1))
 *     .thenAccept(success -> player.sendMessage(success ? "Kit loaded!" : "Failed to load kit"));
 * 
 * // Check if player has kits
 * if (kits.forPlayer(player).hasKit(1)) {
 *     // Player has a kit in slot 1
 * }
 * 
 * // Get all occupied slots
 * List<Integer> occupiedSlots = kits.forPlayer(player).getOccupiedSlots();
 * }</pre>
 * 
 * @since 2.0.0
 */
public interface KitAPI {
    
    /**
     * Creates a player-specific kit builder for fluent operations.
     * 
     * @param player the player
     * @return player kit builder
     * @throws IllegalArgumentException if player is null or offline
     */
    @NotNull
    PlayerKitBuilder forPlayer(@NotNull Player player);
    
    /**
     * Creates a player-specific kit builder using UUID.
     * 
     * @param playerId the player's UUID
     * @return player kit builder
     * @throws IllegalArgumentException if playerId is null
     */
    @NotNull
    PlayerKitBuilder forPlayer(@NotNull UUID playerId);
    
    /**
     * Gets a kit directly without using the builder pattern.
     * 
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return CompletableFuture containing the kit items, or empty if not found
     */
    @NotNull
    CompletableFuture<Optional<ItemStack[]>> getKit(@NotNull UUID playerId, int slot);
    
    /**
     * Saves a kit directly without using the builder pattern.
     * 
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @param items the items to save
     * @return CompletableFuture that completes when the kit is saved
     */
    @NotNull
    CompletableFuture<Void> saveKit(@NotNull UUID playerId, int slot, @NotNull ItemStack[] items);
    
    /**
     * Deletes a kit directly without using the builder pattern.
     * 
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return CompletableFuture that completes when the kit is deleted
     */
    @NotNull
    CompletableFuture<Void> deleteKit(@NotNull UUID playerId, int slot);
    
    /**
     * Checks if a player has a kit in the specified slot.
     * 
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return true if the player has a kit in the slot
     */
    boolean hasKit(@NotNull UUID playerId, int slot);
    
    /**
     * Gets all occupied kit slots for a player.
     * 
     * @param playerId the player's UUID
     * @return stream of occupied slot numbers
     */
    @NotNull
    Stream<Integer> getOccupiedSlots(@NotNull UUID playerId);
    
    /**
     * Gets the total number of kits a player has.
     * 
     * @param playerId the player's UUID
     * @return the number of kits
     */
    int getKitCount(@NotNull UUID playerId);
    
    /**
     * Gets kit metadata without loading the full kit.
     * 
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return CompletableFuture containing kit metadata, or empty if not found
     */
    @NotNull
    CompletableFuture<Optional<KitMetadata>> getKitMetadata(@NotNull UUID playerId, int slot);
    
    /**
     * Copies a kit from one slot to another.
     * 
     * @param playerId the player's UUID
     * @param fromSlot the source slot (1-9)
     * @param toSlot the destination slot (1-9)
     * @return CompletableFuture that completes when the kit is copied
     */
    @NotNull
    CompletableFuture<Void> copyKit(@NotNull UUID playerId, int fromSlot, int toSlot);
    
    /**
     * Swaps two kits.
     * 
     * @param playerId the player's UUID
     * @param slot1 the first slot (1-9)
     * @param slot2 the second slot (1-9)
     * @return CompletableFuture that completes when the kits are swapped
     */
    @NotNull
    CompletableFuture<Void> swapKits(@NotNull UUID playerId, int slot1, int slot2);
    
    /**
     * Validates that a slot number is valid (1-9).
     * 
     * @param slot the slot number to validate
     * @return true if valid
     */
    default boolean isValidSlot(int slot) {
        return slot >= 1 && slot <= 9;
    }
    
    /**
     * Creates a new kit builder for advanced kit operations.
     * 
     * @return a new kit builder
     */
    @NotNull
    KitBuilder builder();
    
    /**
     * Player-specific kit operations builder.
     */
    interface PlayerKitBuilder {
        
        /**
         * Gets the player associated with this builder.
         * 
         * @return the player, or empty if offline
         */
        @NotNull
        Optional<Player> getPlayer();
        
        /**
         * Gets the player UUID.
         * 
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();
        
        /**
         * Loads a kit and applies it to the player's inventory.
         * 
         * @param slot the kit slot (1-9)
         * @return CompletableFuture that completes with true if successful
         */
        @NotNull
        CompletableFuture<Boolean> loadKit(int slot);
        
        /**
         * Saves the player's current inventory to a kit slot.
         * 
         * @param slot the kit slot (1-9)
         * @return CompletableFuture that completes when saved
         */
        @NotNull
        CompletableFuture<Void> saveCurrentInventory(int slot);
        
        /**
         * Saves specific items to a kit slot.
         * 
         * @param slot the kit slot (1-9)
         * @param items the items to save
         * @return CompletableFuture that completes when saved
         */
        @NotNull
        CompletableFuture<Void> saveItems(int slot, @NotNull ItemStack[] items);
        
        /**
         * Gets a kit without applying it to the player.
         * 
         * @param slot the kit slot (1-9)
         * @return CompletableFuture containing the kit items
         */
        @NotNull
        CompletableFuture<Optional<ItemStack[]>> getKit(int slot);
        
        /**
         * Deletes a kit.
         * 
         * @param slot the kit slot (1-9)
         * @return CompletableFuture that completes when deleted
         */
        @NotNull
        CompletableFuture<Void> deleteKit(int slot);
        
        /**
         * Checks if the player has a kit in the specified slot.
         * 
         * @param slot the kit slot (1-9)
         * @return true if the player has a kit in the slot
         */
        boolean hasKit(int slot);
        
        /**
         * Gets all occupied kit slots for this player.
         * 
         * @return list of occupied slot numbers
         */
        @NotNull
        List<Integer> getOccupiedSlots();
        
        /**
         * Gets the total number of kits this player has.
         * 
         * @return the number of kits
         */
        int getKitCount();
        
        /**
         * Clears all kits for this player.
         * 
         * @return CompletableFuture that completes when all kits are cleared
         */
        @NotNull
        CompletableFuture<Void> clearAllKits();
        
        /**
         * Copies a kit to another slot.
         * 
         * @param fromSlot the source slot (1-9)
         * @param toSlot the destination slot (1-9)
         * @return CompletableFuture that completes when copied
         */
        @NotNull
        CompletableFuture<Void> copyKit(int fromSlot, int toSlot);
        
        /**
         * Swaps two kits.
         * 
         * @param slot1 the first slot (1-9)
         * @param slot2 the second slot (1-9)
         * @return CompletableFuture that completes when swapped
         */
        @NotNull
        CompletableFuture<Void> swapKits(int slot1, int slot2);
    }
    
    /**
     * Advanced kit builder for complex operations.
     */
    interface KitBuilder {
        
        /**
         * Sets the player for this kit operation.
         * 
         * @param player the player
         * @return this builder
         */
        @NotNull
        KitBuilder forPlayer(@NotNull Player player);
        
        /**
         * Sets the player UUID for this kit operation.
         * 
         * @param playerId the player UUID
         * @return this builder
         */
        @NotNull
        KitBuilder forPlayer(@NotNull UUID playerId);
        
        /**
         * Sets the kit slot.
         * 
         * @param slot the slot (1-9)
         * @return this builder
         */
        @NotNull
        KitBuilder inSlot(int slot);
        
        /**
         * Sets the items for this kit.
         * 
         * @param items the items
         * @return this builder
         */
        @NotNull
        KitBuilder withItems(@NotNull ItemStack[] items);
        
        /**
         * Enables or disables validation during kit operations.
         * 
         * @param validate true to enable validation
         * @return this builder
         */
        @NotNull
        KitBuilder withValidation(boolean validate);
        
        /**
         * Sets whether to send notifications to the player.
         * 
         * @param notify true to send notifications
         * @return this builder
         */
        @NotNull
        KitBuilder withNotification(boolean notify);
        
        /**
         * Saves the kit with the configured settings.
         * 
         * @return CompletableFuture that completes when saved
         */
        @NotNull
        CompletableFuture<Void> save();
        
        /**
         * Loads the kit with the configured settings.
         * 
         * @return CompletableFuture containing the kit items
         */
        @NotNull
        CompletableFuture<Optional<ItemStack[]>> load();
        
        /**
         * Deletes the kit with the configured settings.
         * 
         * @return CompletableFuture that completes when deleted
         */
        @NotNull
        CompletableFuture<Void> delete();
    }
}