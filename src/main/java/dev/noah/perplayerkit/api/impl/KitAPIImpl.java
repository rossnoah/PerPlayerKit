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

import dev.noah.perplayerkit.api.kit.KitAPI;
import dev.noah.perplayerkit.api.kit.KitMetadata;
import dev.noah.perplayerkit.services.KitService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the KitAPI interface using the modern KitService.
 */
public class KitAPIImpl implements KitAPI {
    
    private final Plugin plugin;
    private final KitService kitService;
    
    public KitAPIImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        // For now, create a basic KitService instance
        // In a real implementation, this would be injected via DI container
        this.kitService = null; // Will be implemented with actual service later
    }
    
    @Override
    @NotNull
    public PlayerKitBuilder forPlayer(@NotNull Player player) {
        return new PlayerKitBuilderImpl(player.getUniqueId(), Optional.of(player));
    }
    
    @Override
    @NotNull
    public PlayerKitBuilder forPlayer(@NotNull UUID playerId) {
        return new PlayerKitBuilderImpl(playerId, Optional.empty());
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<ItemStack[]>> getKit(@NotNull UUID playerId, int slot) {
        if (kitService != null) {
            return kitService.loadKitAsync(playerId, slot);
        }
        // Fallback implementation for now
        return CompletableFuture.completedFuture(Optional.empty());
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> saveKit(@NotNull UUID playerId, int slot, @NotNull ItemStack[] items) {
        if (kitService != null) {
            return kitService.saveKitAsync(playerId, slot, items);
        }
        // Fallback implementation for now
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> deleteKit(@NotNull UUID playerId, int slot) {
        if (kitService != null) {
            return kitService.deleteKitAsync(playerId, slot);
        }
        // Fallback implementation for now
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public boolean hasKit(@NotNull UUID playerId, int slot) {
        if (kitService != null) {
            return kitService.hasKit(playerId, slot);
        }
        // Fallback implementation for now
        return false;
    }
    
    @Override
    @NotNull
    public Stream<Integer> getOccupiedSlots(@NotNull UUID playerId) {
        if (kitService != null) {
            return kitService.getOccupiedSlots(playerId);
        }
        // Fallback implementation for now
        return Stream.empty();
    }
    
    @Override
    public int getKitCount(@NotNull UUID playerId) {
        return (int) getOccupiedSlots(playerId).count();
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<KitMetadata>> getKitMetadata(@NotNull UUID playerId, int slot) {
        // For now, return empty - this would need to be implemented in KitService
        return CompletableFuture.completedFuture(Optional.empty());
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> copyKit(@NotNull UUID playerId, int fromSlot, int toSlot) {
        return getKit(playerId, fromSlot)
            .thenCompose(kit -> {
                if (kit.isPresent()) {
                    return saveKit(playerId, toSlot, kit.get());
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> swapKits(@NotNull UUID playerId, int slot1, int slot2) {
        return getKit(playerId, slot1)
            .thenCombine(getKit(playerId, slot2), (kit1, kit2) -> {
                CompletableFuture<Void> future1 = kit2.map(items -> saveKit(playerId, slot1, items))
                    .orElseGet(() -> deleteKit(playerId, slot1));
                CompletableFuture<Void> future2 = kit1.map(items -> saveKit(playerId, slot2, items))
                    .orElseGet(() -> deleteKit(playerId, slot2));
                
                return CompletableFuture.allOf(future1, future2);
            })
            .thenCompose(future -> future);
    }
    
    @Override
    @NotNull
    public KitBuilder builder() {
        return new KitBuilderImpl();
    }
    
    /**
     * Implementation of PlayerKitBuilder.
     */
    private class PlayerKitBuilderImpl implements PlayerKitBuilder {
        
        private final UUID playerId;
        private final Optional<Player> player;
        
        public PlayerKitBuilderImpl(@NotNull UUID playerId, @NotNull Optional<Player> player) {
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
        @NotNull
        public CompletableFuture<Boolean> loadKit(int slot) {
            return KitAPIImpl.this.getKit(playerId, slot)
                .thenApply(kit -> {
                    if (kit.isPresent() && player.isPresent()) {
                        Player p = player.get();
                        p.getInventory().setContents(kit.get());
                        return true;
                    }
                    return false;
                });
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> saveCurrentInventory(int slot) {
            if (player.isPresent()) {
                ItemStack[] contents = player.get().getInventory().getContents();
                return KitAPIImpl.this.saveKit(playerId, slot, contents);
            }
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> saveItems(int slot, @NotNull ItemStack[] items) {
            return KitAPIImpl.this.saveKit(playerId, slot, items);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Optional<ItemStack[]>> getKit(int slot) {
            return KitAPIImpl.this.getKit(playerId, slot);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> deleteKit(int slot) {
            return KitAPIImpl.this.deleteKit(playerId, slot);
        }
        
        @Override
        public boolean hasKit(int slot) {
            return KitAPIImpl.this.hasKit(playerId, slot);
        }
        
        @Override
        @NotNull
        public List<Integer> getOccupiedSlots() {
            return KitAPIImpl.this.getOccupiedSlots(playerId).collect(Collectors.toList());
        }
        
        @Override
        public int getKitCount() {
            return KitAPIImpl.this.getKitCount(playerId);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> clearAllKits() {
            List<CompletableFuture<Void>> deletions = getOccupiedSlots().stream()
                .map(this::deleteKit)
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(deletions.toArray(new CompletableFuture[0]));
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> copyKit(int fromSlot, int toSlot) {
            return KitAPIImpl.this.copyKit(playerId, fromSlot, toSlot);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> swapKits(int slot1, int slot2) {
            return KitAPIImpl.this.swapKits(playerId, slot1, slot2);
        }
    }
    
    /**
     * Implementation of KitBuilder.
     */
    private class KitBuilderImpl implements KitBuilder {
        
        private UUID playerId;
        private int slot = -1;
        private ItemStack[] items;
        private boolean validate = true;
        private boolean notify = true;
        
        @Override
        @NotNull
        public KitBuilder forPlayer(@NotNull Player player) {
            this.playerId = player.getUniqueId();
            return this;
        }
        
        @Override
        @NotNull
        public KitBuilder forPlayer(@NotNull UUID playerId) {
            this.playerId = playerId;
            return this;
        }
        
        @Override
        @NotNull
        public KitBuilder inSlot(int slot) {
            this.slot = slot;
            return this;
        }
        
        @Override
        @NotNull
        public KitBuilder withItems(@NotNull ItemStack[] items) {
            this.items = items.clone();
            return this;
        }
        
        @Override
        @NotNull
        public KitBuilder withValidation(boolean validate) {
            this.validate = validate;
            return this;
        }
        
        @Override
        @NotNull
        public KitBuilder withNotification(boolean notify) {
            this.notify = notify;
            return this;
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> save() {
            if (playerId == null || slot == -1 || items == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Builder not properly configured"));
            }
            return KitAPIImpl.this.saveKit(playerId, slot, items);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Optional<ItemStack[]>> load() {
            if (playerId == null || slot == -1) {
                return CompletableFuture.failedFuture(new IllegalStateException("Builder not properly configured"));
            }
            return KitAPIImpl.this.getKit(playerId, slot);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> delete() {
            if (playerId == null || slot == -1) {
                return CompletableFuture.failedFuture(new IllegalStateException("Builder not properly configured"));
            }
            return KitAPIImpl.this.deleteKit(playerId, slot);
        }
    }
}