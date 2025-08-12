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
package dev.noah.perplayerkit.api.impl;

import dev.noah.perplayerkit.api.player.PlayerAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of the PlayerAPI interface.
 */
public class PlayerAPIImpl implements PlayerAPI {
    
    private final Plugin plugin;
    
    public PlayerAPIImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean hasKitPermission(@NotNull Player player, int slot) {
        return player.hasPermission("perplayerkit.kit." + slot) || 
               player.hasPermission("perplayerkit.kit.*") ||
               player.hasPermission("perplayerkit.*");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasKitPermission(@NotNull UUID playerId, int slot) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = plugin.getServer().getPlayer(playerId);
            return player != null ? hasKitPermission(player, slot) : false;
        });
    }
    
    @Override
    public int getMaxKitSlots(@NotNull Player player) {
        // Check for specific slot permissions
        for (int i = 9; i >= 1; i--) {
            if (hasKitPermission(player, i)) {
                return i;
            }
        }
        return 0;
    }
    
    @Override
    @NotNull
    public CompletableFuture<Integer> getMaxKitSlots(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = plugin.getServer().getPlayer(playerId);
            return player != null ? getMaxKitSlots(player) : 0;
        });
    }
    
    @Override
    @NotNull
    public List<Integer> getAccessibleSlots(@NotNull Player player) {
        return IntStream.rangeClosed(1, 9)
            .filter(slot -> hasKitPermission(player, slot))
            .boxed()
            .collect(Collectors.toList());
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<Integer>> getAccessibleSlots(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = plugin.getServer().getPlayer(playerId);
            return player != null ? getAccessibleSlots(player) : List.of();
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<PlayerStatistics> getStatistics(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(new PlayerStatisticsImpl(playerId));
    }
    
    @Override
    @NotNull
    public CompletableFuture<PlayerPreferences> getPreferences(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(new PlayerPreferencesImpl(playerId));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> updatePreferences(@NotNull UUID playerId, @NotNull PlayerPreferences preferences) {
        // In a real implementation, this would save to database
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public boolean isAutoSaveEnabled(@NotNull Player player) {
        // Default implementation - would be stored in player data
        return true;
    }
    
    @Override
    public void setAutoSaveEnabled(@NotNull Player player, boolean enabled) {
        // In a real implementation, this would save to player data
    }
    
    @Override
    public boolean areGuiSoundsEnabled(@NotNull Player player) {
        // Default implementation
        return true;
    }
    
    @Override
    public void setGuiSoundsEnabled(@NotNull Player player, boolean enabled) {
        // In a real implementation, this would save to player data
    }
    
    @Override
    @Nullable
    public String getPreferredGuiTheme(@NotNull Player player) {
        // Default implementation
        return null;
    }
    
    @Override
    public void setPreferredGuiTheme(@NotNull Player player, @Nullable String theme) {
        // In a real implementation, this would save to player data
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> resetPlayerData(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<Long>> getLastSeen(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }
    
    @Override
    @NotNull
    public PlayerBuilder forPlayer(@NotNull Player player) {
        return new PlayerBuilderImpl(player.getUniqueId(), Optional.of(player));
    }
    
    @Override
    @NotNull
    public PlayerBuilder forPlayer(@NotNull UUID playerId) {
        return new PlayerBuilderImpl(playerId, Optional.empty());
    }
    
    /**
     * Implementation of PlayerStatistics.
     */
    private static class PlayerStatisticsImpl implements PlayerStatistics {
        
        private final UUID playerId;
        
        public PlayerStatisticsImpl(@NotNull UUID playerId) {
            this.playerId = playerId;
        }
        
        @Override
        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }
        
        @Override
        public int getKitsSaved() {
            return 0; // Would be loaded from database
        }
        
        @Override
        public int getKitsLoaded() {
            return 0; // Would be loaded from database
        }
        
        @Override
        public int getGuiOpens() {
            return 0; // Would be loaded from database
        }
        
        @Override
        public long getTotalPlayTime() {
            return 0; // Would be loaded from database
        }
        
        @Override
        @NotNull
        public Optional<Long> getFirstJoin() {
            return Optional.empty(); // Would be loaded from database
        }
        
        @Override
        @NotNull
        public Optional<Long> getLastSeen() {
            return Optional.empty(); // Would be loaded from database
        }
    }
    
    /**
     * Implementation of PlayerPreferences.
     */
    private static class PlayerPreferencesImpl implements PlayerPreferences {
        
        private final UUID playerId;
        
        public PlayerPreferencesImpl(@NotNull UUID playerId) {
            this.playerId = playerId;
        }
        
        @Override
        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }
        
        @Override
        public boolean isAutoSaveEnabled() {
            return true; // Default value
        }
        
        @Override
        public boolean areGuiSoundsEnabled() {
            return true; // Default value
        }
        
        @Override
        @Nullable
        public String getPreferredGuiTheme() {
            return null; // Default (no preference)
        }
        
        @Override
        @Nullable
        public String getPreferredLanguage() {
            return null; // Default (server language)
        }
        
        @Override
        public boolean areNotificationsEnabled() {
            return true; // Default value
        }
    }
    
    /**
     * Implementation of PlayerBuilder.
     */
    private class PlayerBuilderImpl implements PlayerBuilder {
        
        private final UUID playerId;
        private final Optional<Player> player;
        
        public PlayerBuilderImpl(@NotNull UUID playerId, @NotNull Optional<Player> player) {
            this.playerId = playerId;
            this.player = player;
        }
        
        @Override
        @NotNull
        public Optional<Player> getPlayer() {
            return player.filter(Player::isOnline);
        }
        
        @Override
        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }
        
        @Override
        public boolean hasKitPermission(int slot) {
            return player.map(p -> PlayerAPIImpl.this.hasKitPermission(p, slot)).orElse(false);
        }
        
        @Override
        @NotNull
        public List<Integer> getAccessibleSlots() {
            return player.map(PlayerAPIImpl.this::getAccessibleSlots).orElse(List.of());
        }
        
        @Override
        public int getMaxKitSlots() {
            return player.map(PlayerAPIImpl.this::getMaxKitSlots).orElse(0);
        }
        
        @Override
        @NotNull
        public CompletableFuture<PlayerStatistics> getStatistics() {
            return PlayerAPIImpl.this.getStatistics(playerId);
        }
        
        @Override
        @NotNull
        public CompletableFuture<PlayerPreferences> getPreferences() {
            return PlayerAPIImpl.this.getPreferences(playerId);
        }
        
        @Override
        @NotNull
        public PlayerBuilder withAutoSave(boolean enabled) {
            player.ifPresent(p -> PlayerAPIImpl.this.setAutoSaveEnabled(p, enabled));
            return this;
        }
        
        @Override
        @NotNull
        public PlayerBuilder withGuiSounds(boolean enabled) {
            player.ifPresent(p -> PlayerAPIImpl.this.setGuiSoundsEnabled(p, enabled));
            return this;
        }
        
        @Override
        @NotNull
        public PlayerBuilder withGuiTheme(@Nullable String theme) {
            player.ifPresent(p -> PlayerAPIImpl.this.setPreferredGuiTheme(p, theme));
            return this;
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> apply() {
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> reset() {
            return PlayerAPIImpl.this.resetPlayerData(playerId);
        }
    }
}