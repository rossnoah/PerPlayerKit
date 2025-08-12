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
package dev.noah.perplayerkit.api.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for managing player data and operations.
 * 
 * This interface provides comprehensive player management functionality including
 * permission checks, statistics, and player-specific operations.
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * PlayerAPI players = PerPlayerKitAPI.getInstance().players();
 * 
 * // Check player permissions
 * if (players.hasKitPermission(player, 5)) {
 *     // Player can use kit slot 5
 * }
 * 
 * // Get player statistics
 * players.getStatistics(player.getUniqueId())
 *        .thenAccept(stats -> {
 *            int kitsSaved = stats.getKitsSaved();
 *            int kitsLoaded = stats.getKitsLoaded();
 *        });
 * 
 * // Manage player preferences
 * players.setAutoSaveEnabled(player, true);
 * }</pre>
 * 
 * @since 2.0.0
 */
public interface PlayerAPI {
    
    /**
     * Checks if a player has permission to use a specific kit slot.
     * 
     * @param player the player
     * @param slot the kit slot (1-9)
     * @return true if the player has permission
     */
    boolean hasKitPermission(@NotNull Player player, int slot);
    
    /**
     * Checks if a player has permission to use a specific kit slot by UUID.
     * 
     * @param playerId the player's UUID
     * @param slot the kit slot (1-9)
     * @return CompletableFuture containing permission result
     */
    @NotNull
    CompletableFuture<Boolean> hasKitPermission(@NotNull UUID playerId, int slot);
    
    /**
     * Gets the maximum number of kit slots a player can use.
     * 
     * @param player the player
     * @return the maximum kit slots (1-9)
     */
    int getMaxKitSlots(@NotNull Player player);
    
    /**
     * Gets the maximum number of kit slots a player can use by UUID.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing the maximum kit slots
     */
    @NotNull
    CompletableFuture<Integer> getMaxKitSlots(@NotNull UUID playerId);
    
    /**
     * Gets all kit slots a player has permission to use.
     * 
     * @param player the player
     * @return list of accessible slot numbers
     */
    @NotNull
    List<Integer> getAccessibleSlots(@NotNull Player player);
    
    /**
     * Gets all kit slots a player has permission to use by UUID.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing accessible slot numbers
     */
    @NotNull
    CompletableFuture<List<Integer>> getAccessibleSlots(@NotNull UUID playerId);
    
    /**
     * Gets player statistics.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing player statistics
     */
    @NotNull
    CompletableFuture<PlayerStatistics> getStatistics(@NotNull UUID playerId);
    
    /**
     * Gets player preferences.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing player preferences
     */
    @NotNull
    CompletableFuture<PlayerPreferences> getPreferences(@NotNull UUID playerId);
    
    /**
     * Updates player preferences.
     * 
     * @param playerId the player's UUID
     * @param preferences the new preferences
     * @return CompletableFuture that completes when preferences are updated
     */
    @NotNull
    CompletableFuture<Void> updatePreferences(@NotNull UUID playerId, @NotNull PlayerPreferences preferences);
    
    /**
     * Checks if auto-save is enabled for a player.
     * 
     * @param player the player
     * @return true if auto-save is enabled
     */
    boolean isAutoSaveEnabled(@NotNull Player player);
    
    /**
     * Sets auto-save preference for a player.
     * 
     * @param player the player
     * @param enabled true to enable auto-save
     */
    void setAutoSaveEnabled(@NotNull Player player, boolean enabled);
    
    /**
     * Checks if GUI sounds are enabled for a player.
     * 
     * @param player the player
     * @return true if GUI sounds are enabled
     */
    boolean areGuiSoundsEnabled(@NotNull Player player);
    
    /**
     * Sets GUI sounds preference for a player.
     * 
     * @param player the player
     * @param enabled true to enable GUI sounds
     */
    void setGuiSoundsEnabled(@NotNull Player player, boolean enabled);
    
    /**
     * Gets the player's preferred GUI theme.
     * 
     * @param player the player
     * @return the theme name, or null for default
     */
    @Nullable
    String getPreferredGuiTheme(@NotNull Player player);
    
    /**
     * Sets the player's preferred GUI theme.
     * 
     * @param player the player
     * @param theme the theme name, or null for default
     */
    void setPreferredGuiTheme(@NotNull Player player, @Nullable String theme);
    
    /**
     * Resets all player data and preferences.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture that completes when data is reset
     */
    @NotNull
    CompletableFuture<Void> resetPlayerData(@NotNull UUID playerId);
    
    /**
     * Gets the last time a player was seen online.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing the last seen timestamp, or empty if never seen
     */
    @NotNull
    CompletableFuture<Optional<Long>> getLastSeen(@NotNull UUID playerId);
    
    /**
     * Creates a player builder for fluent operations.
     * 
     * @param player the player
     * @return player builder
     */
    @NotNull
    PlayerBuilder forPlayer(@NotNull Player player);
    
    /**
     * Creates a player builder for fluent operations using UUID.
     * 
     * @param playerId the player's UUID
     * @return player builder
     */
    @NotNull
    PlayerBuilder forPlayer(@NotNull UUID playerId);
    
    /**
     * Player statistics data.
     */
    interface PlayerStatistics {
        
        /**
         * Gets the player's UUID.
         * 
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();
        
        /**
         * Gets the number of kits saved by the player.
         * 
         * @return kits saved count
         */
        int getKitsSaved();
        
        /**
         * Gets the number of kits loaded by the player.
         * 
         * @return kits loaded count
         */
        int getKitsLoaded();
        
        /**
         * Gets the number of times the player opened the GUI.
         * 
         * @return GUI opens count
         */
        int getGuiOpens();
        
        /**
         * Gets the total play time with PerPlayerKit (in milliseconds).
         * 
         * @return total play time
         */
        long getTotalPlayTime();
        
        /**
         * Gets the player's first join timestamp.
         * 
         * @return first join time, or empty if unknown
         */
        @NotNull
        Optional<Long> getFirstJoin();
        
        /**
         * Gets the player's last seen timestamp.
         * 
         * @return last seen time, or empty if unknown
         */
        @NotNull
        Optional<Long> getLastSeen();
    }
    
    /**
     * Player preferences data.
     */
    interface PlayerPreferences {
        
        /**
         * Gets the player's UUID.
         * 
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();
        
        /**
         * Checks if auto-save is enabled.
         * 
         * @return true if auto-save is enabled
         */
        boolean isAutoSaveEnabled();
        
        /**
         * Checks if GUI sounds are enabled.
         * 
         * @return true if GUI sounds are enabled
         */
        boolean areGuiSoundsEnabled();
        
        /**
         * Gets the preferred GUI theme.
         * 
         * @return the theme name, or null for default
         */
        @Nullable
        String getPreferredGuiTheme();
        
        /**
         * Gets the preferred language.
         * 
         * @return the language code, or null for default
         */
        @Nullable
        String getPreferredLanguage();
        
        /**
         * Checks if notifications are enabled.
         * 
         * @return true if notifications are enabled
         */
        boolean areNotificationsEnabled();
    }
    
    /**
     * Builder for player operations.
     */
    interface PlayerBuilder {
        
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
         * Checks kit slot permissions.
         * 
         * @param slot the slot to check
         * @return true if player has permission
         */
        boolean hasKitPermission(int slot);
        
        /**
         * Gets all accessible kit slots.
         * 
         * @return list of accessible slots
         */
        @NotNull
        List<Integer> getAccessibleSlots();
        
        /**
         * Gets the maximum kit slots.
         * 
         * @return maximum slots
         */
        int getMaxKitSlots();
        
        /**
         * Gets player statistics.
         * 
         * @return CompletableFuture containing statistics
         */
        @NotNull
        CompletableFuture<PlayerStatistics> getStatistics();
        
        /**
         * Gets player preferences.
         * 
         * @return CompletableFuture containing preferences
         */
        @NotNull
        CompletableFuture<PlayerPreferences> getPreferences();
        
        /**
         * Sets auto-save preference.
         * 
         * @param enabled true to enable
         * @return this builder
         */
        @NotNull
        PlayerBuilder withAutoSave(boolean enabled);
        
        /**
         * Sets GUI sounds preference.
         * 
         * @param enabled true to enable
         * @return this builder
         */
        @NotNull
        PlayerBuilder withGuiSounds(boolean enabled);
        
        /**
         * Sets preferred GUI theme.
         * 
         * @param theme the theme name
         * @return this builder
         */
        @NotNull
        PlayerBuilder withGuiTheme(@Nullable String theme);
        
        /**
         * Applies all configured preferences.
         * 
         * @return CompletableFuture that completes when applied
         */
        @NotNull
        CompletableFuture<Void> apply();
        
        /**
         * Resets all player data.
         * 
         * @return CompletableFuture that completes when reset
         */
        @NotNull
        CompletableFuture<Void> reset();
    }
}