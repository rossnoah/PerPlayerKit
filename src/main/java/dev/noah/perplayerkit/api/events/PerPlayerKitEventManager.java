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
package dev.noah.perplayerkit.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Event manager for PerPlayerKit events.
 * 
 * This interface provides a modern event system with fluent listener registration
 * and comprehensive event handling for all PerPlayerKit operations.
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * PerPlayerKitEventManager events = PerPlayerKitAPI.getInstance().events();
 * 
 * // Listen to kit saved events
 * events.onKitSaved(event -> {
 *     Player player = event.getPlayer();
 *     int slot = event.getSlot();
 *     System.out.println(player.getName() + " saved kit in slot " + slot);
 * });
 * 
 * // Listen to kit loaded events with priority
 * events.onKitLoaded(EventPriority.HIGH, event -> {
 *     // Handle kit loaded with high priority
 * });
 * 
 * // Listen to cancellable events
 * events.onKitSaving(event -> {
 *     if (event.getPlayer().getName().equals("BadPlayer")) {
 *         event.setCancelled(true);
 *         event.getPlayer().sendMessage("You cannot save kits!");
 *     }
 * });
 * }</pre>
 * 
 * @since 2.0.0
 */
public interface PerPlayerKitEventManager {
    
    /**
     * Registers a listener for kit saved events.
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitSaved(@NotNull Consumer<KitSavedEvent> listener);
    
    /**
     * Registers a listener for kit saved events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitSaved(@NotNull EventPriority priority, @NotNull Consumer<KitSavedEvent> listener);
    
    /**
     * Registers a listener for kit saving events (cancellable).
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitSaving(@NotNull Consumer<KitSavingEvent> listener);
    
    /**
     * Registers a listener for kit saving events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitSaving(@NotNull EventPriority priority, @NotNull Consumer<KitSavingEvent> listener);
    
    /**
     * Registers a listener for kit loaded events.
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitLoaded(@NotNull Consumer<KitLoadedEvent> listener);
    
    /**
     * Registers a listener for kit loaded events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitLoaded(@NotNull EventPriority priority, @NotNull Consumer<KitLoadedEvent> listener);
    
    /**
     * Registers a listener for kit loading events (cancellable).
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitLoading(@NotNull Consumer<KitLoadingEvent> listener);
    
    /**
     * Registers a listener for kit loading events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitLoading(@NotNull EventPriority priority, @NotNull Consumer<KitLoadingEvent> listener);
    
    /**
     * Registers a listener for kit deleted events.
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitDeleted(@NotNull Consumer<KitDeletedEvent> listener);
    
    /**
     * Registers a listener for kit deleted events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onKitDeleted(@NotNull EventPriority priority, @NotNull Consumer<KitDeletedEvent> listener);
    
    /**
     * Registers a listener for GUI opened events.
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onGuiOpened(@NotNull Consumer<GuiOpenedEvent> listener);
    
    /**
     * Registers a listener for GUI opened events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onGuiOpened(@NotNull EventPriority priority, @NotNull Consumer<GuiOpenedEvent> listener);
    
    /**
     * Registers a listener for GUI closed events.
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onGuiClosed(@NotNull Consumer<GuiClosedEvent> listener);
    
    /**
     * Registers a listener for GUI closed events with specific priority.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onGuiClosed(@NotNull EventPriority priority, @NotNull Consumer<GuiClosedEvent> listener);
    
    /**
     * Registers a listener for player data changed events.
     * 
     * @param listener the event listener
     * @return event registration handle
     */
    @NotNull
    EventRegistration onPlayerDataChanged(@NotNull Consumer<PlayerDataChangedEvent> listener);
    
    /**
     * Unregisters all event listeners for a specific plugin.
     * 
     * @param plugin the plugin class
     */
    void unregisterAll(@NotNull Class<?> plugin);
    
    /**
     * Gets the total number of registered event listeners.
     * 
     * @return listener count
     */
    int getListenerCount();
    
    /**
     * Event priority levels.
     */
    enum EventPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST
    }
    
    /**
     * Event registration handle.
     */
    interface EventRegistration {
        
        /**
         * Unregisters this event listener.
         */
        void unregister();
        
        /**
         * Checks if this registration is still active.
         * 
         * @return true if active
         */
        boolean isActive();
        
        /**
         * Gets the registration priority.
         * 
         * @return the priority
         */
        @NotNull
        EventPriority getPriority();
    }
    
    /**
     * Base interface for all PerPlayerKit events.
     */
    interface PerPlayerKitEvent {
        
        /**
         * Gets the player associated with this event.
         * 
         * @return the player
         */
        @NotNull
        Player getPlayer();
        
        /**
         * Gets the player's UUID.
         * 
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();
        
        /**
         * Gets the event timestamp.
         * 
         * @return timestamp in milliseconds
         */
        long getTimestamp();
    }
    
    /**
     * Event fired when a kit is successfully saved.
     */
    interface KitSavedEvent extends PerPlayerKitEvent {
        
        /**
         * Gets the kit slot that was saved.
         * 
         * @return the kit slot (1-9)
         */
        int getSlot();
        
        /**
         * Gets the items that were saved.
         * 
         * @return the kit items
         */
        @NotNull
        ItemStack[] getItems();
        
        /**
         * Checks if this was an overwrite of an existing kit.
         * 
         * @return true if kit was overwritten
         */
        boolean wasOverwrite();
    }
    
    /**
     * Cancellable event fired before a kit is saved.
     */
    interface KitSavingEvent extends PerPlayerKitEvent, Cancellable {
        
        /**
         * Gets the kit slot being saved.
         * 
         * @return the kit slot (1-9)
         */
        int getSlot();
        
        /**
         * Gets the items being saved.
         * 
         * @return the kit items
         */
        @NotNull
        ItemStack[] getItems();
        
        /**
         * Modifies the items being saved.
         * 
         * @param items the new items
         */
        void setItems(@NotNull ItemStack[] items);
        
        /**
         * Checks if this will overwrite an existing kit.
         * 
         * @return true if kit will be overwritten
         */
        boolean willOverwrite();
    }
    
    /**
     * Event fired when a kit is successfully loaded.
     */
    interface KitLoadedEvent extends PerPlayerKitEvent {
        
        /**
         * Gets the kit slot that was loaded.
         * 
         * @return the kit slot (1-9)
         */
        int getSlot();
        
        /**
         * Gets the items that were loaded.
         * 
         * @return the kit items
         */
        @NotNull
        ItemStack[] getItems();
    }
    
    /**
     * Cancellable event fired before a kit is loaded.
     */
    interface KitLoadingEvent extends PerPlayerKitEvent, Cancellable {
        
        /**
         * Gets the kit slot being loaded.
         * 
         * @return the kit slot (1-9)
         */
        int getSlot();
    }
    
    /**
     * Event fired when a kit is successfully deleted.
     */
    interface KitDeletedEvent extends PerPlayerKitEvent {
        
        /**
         * Gets the kit slot that was deleted.
         * 
         * @return the kit slot (1-9)
         */
        int getSlot();
        
        /**
         * Gets the items that were deleted.
         * 
         * @return the deleted kit items, or null if unknown
         */
        @Nullable
        ItemStack[] getDeletedItems();
    }
    
    /**
     * Event fired when a GUI is opened for a player.
     */
    interface GuiOpenedEvent extends PerPlayerKitEvent {
        
        /**
         * Gets the type of GUI that was opened.
         * 
         * @return the GUI type
         */
        @NotNull
        GuiType getGuiType();
        
        /**
         * Gets additional context for the GUI.
         * 
         * @return GUI context, or null if none
         */
        @Nullable
        String getContext();
        
        /**
         * GUI types.
         */
        enum GuiType {
            MAIN_MENU,
            KIT_PREVIEW,
            SETTINGS,
            CUSTOM
        }
    }
    
    /**
     * Event fired when a GUI is closed for a player.
     */
    interface GuiClosedEvent extends PerPlayerKitEvent {
        
        /**
         * Gets the type of GUI that was closed.
         * 
         * @return the GUI type
         */
        @NotNull
        GuiOpenedEvent.GuiType getGuiType();
        
        /**
         * Gets the close reason.
         * 
         * @return the close reason
         */
        @NotNull
        CloseReason getCloseReason();
        
        /**
         * Reasons for GUI closure.
         */
        enum CloseReason {
            PLAYER_CLOSED,
            PLUGIN_CLOSED,
            SERVER_SHUTDOWN,
            OTHER
        }
    }
    
    /**
     * Event fired when player data is changed.
     */
    interface PlayerDataChangedEvent extends PerPlayerKitEvent {
        
        /**
         * Gets the data key that was changed.
         * 
         * @return the data key
         */
        @NotNull
        String getKey();
        
        /**
         * Gets the old value.
         * 
         * @return the old value, or null if new key
         */
        @Nullable
        Object getOldValue();
        
        /**
         * Gets the new value.
         * 
         * @return the new value, or null if removed
         */
        @Nullable
        Object getNewValue();
        
        /**
         * Gets the change type.
         * 
         * @return the change type
         */
        @NotNull
        ChangeType getChangeType();
        
        /**
         * Types of data changes.
         */
        enum ChangeType {
            CREATED,
            UPDATED,
            REMOVED
        }
    }
}