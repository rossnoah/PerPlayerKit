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

import dev.noah.perplayerkit.api.gui.GuiAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implementation of the GuiAPI interface.
 */
public class GuiAPIImpl implements GuiAPI {
    
    private final Plugin plugin;
    private final ConcurrentHashMap<String, GuiActionHandler> actionHandlers;
    private String defaultTheme = "default";
    
    public GuiAPIImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.actionHandlers = new ConcurrentHashMap<>();
    }
    
    @Override
    public void openMainGui(@NotNull Player player) {
        openMainGui(player, null);
    }
    
    @Override
    public void openMainGui(@NotNull Player player, @Nullable String theme) {
        // In a real implementation, this would use the GUI system to open the main menu
        player.sendMessage("Opening main GUI" + (theme != null ? " with theme: " + theme : ""));
    }
    
    @Override
    public void openKitPreview(@NotNull Player player, int slot) {
        openKitPreview(player, slot, null);
    }
    
    @Override
    public void openKitPreview(@NotNull Player player, int slot, @Nullable String theme) {
        // In a real implementation, this would open the kit preview GUI
        player.sendMessage("Opening kit preview for slot " + slot + (theme != null ? " with theme: " + theme : ""));
    }
    
    @Override
    public void openSettingsGui(@NotNull Player player) {
        openSettingsGui(player, null);
    }
    
    @Override
    public void openSettingsGui(@NotNull Player player, @Nullable String theme) {
        // In a real implementation, this would open the settings GUI
        player.sendMessage("Opening settings GUI" + (theme != null ? " with theme: " + theme : ""));
    }
    
    @Override
    public void closeGui(@NotNull Player player) {
        player.closeInventory();
    }
    
    @Override
    public void refreshGui(@NotNull Player player) {
        // In a real implementation, this would refresh the currently open GUI
    }
    
    @Override
    public boolean hasGuiOpen(@NotNull Player player) {
        // In a real implementation, this would check if the player has a PerPlayerKit GUI open
        return false;
    }
    
    @Override
    @Nullable
    public GuiType getOpenGuiType(@NotNull Player player) {
        // In a real implementation, this would return the type of GUI currently open
        return null;
    }
    
    @Override
    @NotNull
    public List<String> getAvailableThemes() {
        return List.of("default", "dark", "neon", "medieval");
    }
    
    @Override
    public boolean isThemeAvailable(@NotNull String theme) {
        return getAvailableThemes().contains(theme);
    }
    
    @Override
    @NotNull
    public String getDefaultTheme() {
        return defaultTheme;
    }
    
    @Override
    public void setDefaultTheme(@NotNull String theme) {
        this.defaultTheme = theme;
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> reloadConfigurations() {
        return CompletableFuture.runAsync(() -> {
            // In a real implementation, this would reload GUI configurations
        });
    }
    
    @Override
    @NotNull
    public GuiBuilder builder() {
        return new GuiBuilderImpl();
    }
    
    @Override
    public void registerActionHandler(@NotNull String actionId, @NotNull GuiActionHandler handler) {
        actionHandlers.put(actionId, handler);
    }
    
    @Override
    public void unregisterActionHandler(@NotNull String actionId) {
        actionHandlers.remove(actionId);
    }
    
    /**
     * Implementation of GuiActionContext.
     */
    private static class GuiActionContextImpl implements GuiActionContext {
        
        private final Player player;
        private final int slot;
        private final ItemStack clickedItem;
        private final ClickType clickType;
        private final ConcurrentHashMap<String, Object> data;
        private boolean cancelled = false;
        
        public GuiActionContextImpl(@NotNull Player player, int slot, @Nullable ItemStack clickedItem, @NotNull ClickType clickType) {
            this.player = player;
            this.slot = slot;
            this.clickedItem = clickedItem;
            this.clickType = clickType;
            this.data = new ConcurrentHashMap<>();
        }
        
        @Override
        @NotNull
        public Player getPlayer() {
            return player;
        }
        
        @Override
        public int getSlot() {
            return slot;
        }
        
        @Override
        @Nullable
        public ItemStack getClickedItem() {
            return clickedItem;
        }
        
        @Override
        @NotNull
        public ClickType getClickType() {
            return clickType;
        }
        
        @Override
        @Nullable
        public Object getData(@NotNull String key) {
            return data.get(key);
        }
        
        @Override
        public void setData(@NotNull String key, @Nullable Object value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }
        
        @Override
        public void cancel() {
            this.cancelled = true;
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
    
    /**
     * Implementation of GuiBuilder.
     */
    private class GuiBuilderImpl implements GuiBuilder {
        
        private String title = "Custom GUI";
        private int size = 27;
        private String theme;
        private final ItemStack[] items = new ItemStack[54];
        private final Consumer<GuiActionContext>[] clickHandlers = new Consumer[54];
        private Consumer<GuiActionContext> globalClickHandler;
        private Consumer<Player> closeHandler;
        private boolean allowMove = false;
        private int autoRefreshInterval = 0;
        
        @Override
        @NotNull
        public GuiBuilder title(@NotNull String title) {
            this.title = title;
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder size(int size) {
            if (size % 9 != 0 || size < 9 || size > 54) {
                throw new IllegalArgumentException("Size must be a multiple of 9 between 9 and 54");
            }
            this.size = size;
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder theme(@Nullable String theme) {
            this.theme = theme;
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder item(int slot, @Nullable ItemStack item) {
            if (slot >= 0 && slot < size) {
                items[slot] = item;
            }
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder item(int slot, @Nullable ItemStack item, @Nullable Consumer<GuiActionContext> onClick) {
            item(slot, item);
            if (slot >= 0 && slot < size) {
                clickHandlers[slot] = onClick;
            }
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder fillEmpty(@Nullable ItemStack item) {
            for (int i = 0; i < size; i++) {
                if (items[i] == null) {
                    items[i] = item;
                }
            }
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder onClick(@Nullable Consumer<GuiActionContext> onClick) {
            this.globalClickHandler = onClick;
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder onClose(@Nullable Consumer<Player> onClose) {
            this.closeHandler = onClose;
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder allowMove(boolean allowMove) {
            this.allowMove = allowMove;
            return this;
        }
        
        @Override
        @NotNull
        public GuiBuilder autoRefresh(int interval) {
            this.autoRefreshInterval = interval;
            return this;
        }
        
        @Override
        @NotNull
        public CustomGui build() {
            return new CustomGuiImpl(title, size, theme, items.clone(), clickHandlers.clone(), 
                globalClickHandler, closeHandler, allowMove, autoRefreshInterval);
        }
    }
    
    /**
     * Implementation of CustomGui.
     */
    private static class CustomGuiImpl implements CustomGui {
        
        private final String title;
        private final int size;
        private final String theme;
        private final ItemStack[] items;
        private final Consumer<GuiActionContext>[] clickHandlers;
        private final Consumer<GuiActionContext> globalClickHandler;
        private final Consumer<Player> closeHandler;
        private final boolean allowMove;
        private final int autoRefreshInterval;
        
        public CustomGuiImpl(@NotNull String title, int size, @Nullable String theme, 
                           @NotNull ItemStack[] items, @NotNull Consumer<GuiActionContext>[] clickHandlers,
                           @Nullable Consumer<GuiActionContext> globalClickHandler, 
                           @Nullable Consumer<Player> closeHandler,
                           boolean allowMove, int autoRefreshInterval) {
            this.title = title;
            this.size = size;
            this.theme = theme;
            this.items = items;
            this.clickHandlers = clickHandlers;
            this.globalClickHandler = globalClickHandler;
            this.closeHandler = closeHandler;
            this.allowMove = allowMove;
            this.autoRefreshInterval = autoRefreshInterval;
        }
        
        @Override
        public void open(@NotNull Player player) {
            // In a real implementation, this would create and open the inventory
            player.sendMessage("Opening custom GUI: " + title);
        }
        
        @Override
        public void close(@NotNull Player player) {
            player.closeInventory();
        }
        
        @Override
        public void refresh() {
            // In a real implementation, this would refresh the GUI for all viewers
        }
        
        @Override
        public void updateItem(int slot, @Nullable ItemStack item) {
            if (slot >= 0 && slot < size) {
                items[slot] = item;
                // In a real implementation, this would update the inventory
            }
        }
        
        @Override
        @NotNull
        public String getTitle() {
            return title;
        }
        
        @Override
        public int getSize() {
            return size;
        }
        
        @Override
        @NotNull
        public List<Player> getViewers() {
            // In a real implementation, this would return actual viewers
            return List.of();
        }
        
        @Override
        public boolean isViewing(@NotNull Player player) {
            return getViewers().contains(player);
        }
    }
}