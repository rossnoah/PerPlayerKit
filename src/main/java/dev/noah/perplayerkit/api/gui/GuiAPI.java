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
package dev.noah.perplayerkit.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * API for managing GUI interactions and customization.
 * 
 * This interface provides comprehensive GUI management functionality including
 * opening custom GUIs, managing themes, and handling player interactions.
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * GuiAPI gui = PerPlayerKitAPI.getInstance().gui();
 * 
 * // Open the main kit GUI
 * gui.openMainGui(player);
 * 
 * // Open with a specific theme
 * gui.openMainGui(player, "dark");
 * 
 * // Create a custom GUI
 * gui.builder()
 *    .title("Custom Kit Menu")
 *    .size(54)
 *    .theme("neon")
 *    .onClick((player, slot, item) -> {
 *        // Handle click
 *    })
 *    .build()
 *    .open(player);
 * }</pre>
 * 
 * @since 2.0.0
 */
public interface GuiAPI {
    
    /**
     * Opens the main kit GUI for a player.
     * 
     * @param player the player
     */
    void openMainGui(@NotNull Player player);
    
    /**
     * Opens the main kit GUI with a specific theme.
     * 
     * @param player the player
     * @param theme the theme name, or null for default
     */
    void openMainGui(@NotNull Player player, @Nullable String theme);
    
    /**
     * Opens the kit preview GUI for a specific slot.
     * 
     * @param player the player
     * @param slot the kit slot to preview (1-9)
     */
    void openKitPreview(@NotNull Player player, int slot);
    
    /**
     * Opens the kit preview GUI with a specific theme.
     * 
     * @param player the player
     * @param slot the kit slot to preview (1-9)
     * @param theme the theme name, or null for default
     */
    void openKitPreview(@NotNull Player player, int slot, @Nullable String theme);
    
    /**
     * Opens the settings GUI for a player.
     * 
     * @param player the player
     */
    void openSettingsGui(@NotNull Player player);
    
    /**
     * Opens the settings GUI with a specific theme.
     * 
     * @param player the player
     * @param theme the theme name, or null for default
     */
    void openSettingsGui(@NotNull Player player, @Nullable String theme);
    
    /**
     * Closes any open PerPlayerKit GUI for a player.
     * 
     * @param player the player
     */
    void closeGui(@NotNull Player player);
    
    /**
     * Refreshes the currently open GUI for a player.
     * 
     * @param player the player
     */
    void refreshGui(@NotNull Player player);
    
    /**
     * Checks if a player has a PerPlayerKit GUI open.
     * 
     * @param player the player
     * @return true if a GUI is open
     */
    boolean hasGuiOpen(@NotNull Player player);
    
    /**
     * Gets the type of GUI currently open for a player.
     * 
     * @param player the player
     * @return the GUI type, or null if no GUI is open
     */
    @Nullable
    GuiType getOpenGuiType(@NotNull Player player);
    
    /**
     * Gets all available theme names.
     * 
     * @return list of theme names
     */
    @NotNull
    List<String> getAvailableThemes();
    
    /**
     * Checks if a theme exists.
     * 
     * @param theme the theme name
     * @return true if the theme exists
     */
    boolean isThemeAvailable(@NotNull String theme);
    
    /**
     * Gets the default theme name.
     * 
     * @return the default theme name
     */
    @NotNull
    String getDefaultTheme();
    
    /**
     * Sets the default theme.
     * 
     * @param theme the theme name
     */
    void setDefaultTheme(@NotNull String theme);
    
    /**
     * Reloads all GUI configurations and themes.
     * 
     * @return CompletableFuture that completes when reloaded
     */
    @NotNull
    CompletableFuture<Void> reloadConfigurations();
    
    /**
     * Creates a new custom GUI builder.
     * 
     * @return GUI builder instance
     */
    @NotNull
    GuiBuilder builder();
    
    /**
     * Registers a custom GUI action handler.
     * 
     * @param actionId the action identifier
     * @param handler the action handler
     */
    void registerActionHandler(@NotNull String actionId, @NotNull GuiActionHandler handler);
    
    /**
     * Unregisters a custom GUI action handler.
     * 
     * @param actionId the action identifier
     */
    void unregisterActionHandler(@NotNull String actionId);
    
    /**
     * Types of GUIs in PerPlayerKit.
     */
    enum GuiType {
        MAIN_MENU,
        KIT_PREVIEW,
        SETTINGS,
        CUSTOM
    }
    
    /**
     * Interface for handling GUI actions.
     */
    @FunctionalInterface
    interface GuiActionHandler {
        
        /**
         * Handles a GUI action.
         * 
         * @param context the action context
         */
        void handle(@NotNull GuiActionContext context);
    }
    
    /**
     * Context information for GUI actions.
     */
    interface GuiActionContext {
        
        /**
         * Gets the player who triggered the action.
         * 
         * @return the player
         */
        @NotNull
        Player getPlayer();
        
        /**
         * Gets the slot that was clicked.
         * 
         * @return the slot number
         */
        int getSlot();
        
        /**
         * Gets the item that was clicked.
         * 
         * @return the item, or null if empty slot
         */
        @Nullable
        ItemStack getClickedItem();
        
        /**
         * Gets the click type.
         * 
         * @return the click type
         */
        @NotNull
        ClickType getClickType();
        
        /**
         * Gets additional action data.
         * 
         * @param key the data key
         * @return the data value, or null if not present
         */
        @Nullable
        Object getData(@NotNull String key);
        
        /**
         * Sets additional action data.
         * 
         * @param key the data key
         * @param value the data value
         */
        void setData(@NotNull String key, @Nullable Object value);
        
        /**
         * Cancels the default action.
         */
        void cancel();
        
        /**
         * Checks if the action is cancelled.
         * 
         * @return true if cancelled
         */
        boolean isCancelled();
    }
    
    /**
     * Types of clicks in GUIs.
     */
    enum ClickType {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        MIDDLE,
        DROP,
        CONTROL_DROP
    }
    
    /**
     * Builder for creating custom GUIs.
     */
    interface GuiBuilder {
        
        /**
         * Sets the GUI title.
         * 
         * @param title the title
         * @return this builder
         */
        @NotNull
        GuiBuilder title(@NotNull String title);
        
        /**
         * Sets the GUI size (must be multiple of 9, max 54).
         * 
         * @param size the inventory size
         * @return this builder
         */
        @NotNull
        GuiBuilder size(int size);
        
        /**
         * Sets the GUI theme.
         * 
         * @param theme the theme name
         * @return this builder
         */
        @NotNull
        GuiBuilder theme(@Nullable String theme);
        
        /**
         * Sets an item in a specific slot.
         * 
         * @param slot the slot (0-based)
         * @param item the item to set
         * @return this builder
         */
        @NotNull
        GuiBuilder item(int slot, @Nullable ItemStack item);
        
        /**
         * Sets an item with a click handler.
         * 
         * @param slot the slot (0-based)
         * @param item the item to set
         * @param onClick the click handler
         * @return this builder
         */
        @NotNull
        GuiBuilder item(int slot, @Nullable ItemStack item, @Nullable Consumer<GuiActionContext> onClick);
        
        /**
         * Fills empty slots with a specific item.
         * 
         * @param item the fill item
         * @return this builder
         */
        @NotNull
        GuiBuilder fillEmpty(@Nullable ItemStack item);
        
        /**
         * Sets a global click handler.
         * 
         * @param onClick the click handler
         * @return this builder
         */
        @NotNull
        GuiBuilder onClick(@Nullable Consumer<GuiActionContext> onClick);
        
        /**
         * Sets a close handler.
         * 
         * @param onClose the close handler
         * @return this builder
         */
        @NotNull
        GuiBuilder onClose(@Nullable Consumer<Player> onClose);
        
        /**
         * Enables or disables item movement.
         * 
         * @param allowMove true to allow item movement
         * @return this builder
         */
        @NotNull
        GuiBuilder allowMove(boolean allowMove);
        
        /**
         * Sets whether the GUI should automatically refresh.
         * 
         * @param interval refresh interval in ticks, or 0 to disable
         * @return this builder
         */
        @NotNull
        GuiBuilder autoRefresh(int interval);
        
        /**
         * Builds the GUI instance.
         * 
         * @return the built GUI
         */
        @NotNull
        CustomGui build();
    }
    
    /**
     * Represents a custom GUI instance.
     */
    interface CustomGui {
        
        /**
         * Opens the GUI for a player.
         * 
         * @param player the player
         */
        void open(@NotNull Player player);
        
        /**
         * Closes the GUI for a player.
         * 
         * @param player the player
         */
        void close(@NotNull Player player);
        
        /**
         * Refreshes the GUI for all viewing players.
         */
        void refresh();
        
        /**
         * Updates an item in the GUI.
         * 
         * @param slot the slot (0-based)
         * @param item the new item
         */
        void updateItem(int slot, @Nullable ItemStack item);
        
        /**
         * Gets the GUI title.
         * 
         * @return the title
         */
        @NotNull
        String getTitle();
        
        /**
         * Gets the GUI size.
         * 
         * @return the inventory size
         */
        int getSize();
        
        /**
         * Gets all players currently viewing this GUI.
         * 
         * @return list of viewing players
         */
        @NotNull
        List<Player> getViewers();
        
        /**
         * Checks if a player is viewing this GUI.
         * 
         * @param player the player
         * @return true if viewing
         */
        boolean isViewing(@NotNull Player player);
    }
}