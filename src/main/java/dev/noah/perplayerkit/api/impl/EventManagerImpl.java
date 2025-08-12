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

import dev.noah.perplayerkit.api.events.PerPlayerKitEventManager;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Implementation of the PerPlayerKitEventManager interface.
 */
public class EventManagerImpl implements PerPlayerKitEventManager {
    
    private final Plugin plugin;
    private final ConcurrentHashMap<Long, EventRegistrationImpl> registrations;
    private final AtomicLong registrationIdCounter;
    
    public EventManagerImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.registrations = new ConcurrentHashMap<>();
        this.registrationIdCounter = new AtomicLong(0);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitSaved(@NotNull Consumer<KitSavedEvent> listener) {
        return onKitSaved(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitSaved(@NotNull EventPriority priority, @NotNull Consumer<KitSavedEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitSaving(@NotNull Consumer<KitSavingEvent> listener) {
        return onKitSaving(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitSaving(@NotNull EventPriority priority, @NotNull Consumer<KitSavingEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitLoaded(@NotNull Consumer<KitLoadedEvent> listener) {
        return onKitLoaded(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitLoaded(@NotNull EventPriority priority, @NotNull Consumer<KitLoadedEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitLoading(@NotNull Consumer<KitLoadingEvent> listener) {
        return onKitLoading(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitLoading(@NotNull EventPriority priority, @NotNull Consumer<KitLoadingEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitDeleted(@NotNull Consumer<KitDeletedEvent> listener) {
        return onKitDeleted(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onKitDeleted(@NotNull EventPriority priority, @NotNull Consumer<KitDeletedEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onGuiOpened(@NotNull Consumer<GuiOpenedEvent> listener) {
        return onGuiOpened(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onGuiOpened(@NotNull EventPriority priority, @NotNull Consumer<GuiOpenedEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onGuiClosed(@NotNull Consumer<GuiClosedEvent> listener) {
        return onGuiClosed(EventPriority.NORMAL, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onGuiClosed(@NotNull EventPriority priority, @NotNull Consumer<GuiClosedEvent> listener) {
        return createRegistration(priority, listener);
    }
    
    @Override
    @NotNull
    public EventRegistration onPlayerDataChanged(@NotNull Consumer<PlayerDataChangedEvent> listener) {
        return createRegistration(EventPriority.NORMAL, listener);
    }
    
    @Override
    public void unregisterAll(@NotNull Class<?> plugin) {
        registrations.values().removeIf(registration -> 
            registration.getOwnerClass().equals(plugin));
    }
    
    @Override
    public int getListenerCount() {
        return registrations.size();
    }
    
    /**
     * Creates a new event registration.
     * 
     * @param priority the event priority
     * @param listener the event listener
     * @return the registration
     */
    private EventRegistration createRegistration(@NotNull EventPriority priority, @NotNull Consumer<?> listener) {
        long id = registrationIdCounter.incrementAndGet();
        EventRegistrationImpl registration = new EventRegistrationImpl(id, priority, listener, getCallerClass());
        registrations.put(id, registration);
        return registration;
    }
    
    /**
     * Gets the class that called the registration method.
     * 
     * @return the caller class
     */
    private Class<?> getCallerClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        // Skip getStackTrace(), getCallerClass(), createRegistration(), and the event registration method
        for (int i = 4; i < stack.length; i++) {
            try {
                String className = stack[i].getClassName();
                if (!className.startsWith("dev.noah.perplayerkit.api.impl.")) {
                    return Class.forName(className);
                }
            } catch (ClassNotFoundException e) {
                // Continue searching
            }
        }
        return Object.class; // Fallback
    }
    
    /**
     * Implementation of EventRegistration.
     */
    private class EventRegistrationImpl implements EventRegistration {
        
        private final long id;
        private final EventPriority priority;
        private final Consumer<?> listener;
        private final Class<?> ownerClass;
        private volatile boolean active = true;
        
        public EventRegistrationImpl(long id, @NotNull EventPriority priority, @NotNull Consumer<?> listener, @NotNull Class<?> ownerClass) {
            this.id = id;
            this.priority = priority;
            this.listener = listener;
            this.ownerClass = ownerClass;
        }
        
        @Override
        public void unregister() {
            active = false;
            registrations.remove(id);
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
        
        @Override
        @NotNull
        public EventPriority getPriority() {
            return priority;
        }
        
        /**
         * Gets the class that owns this registration.
         * 
         * @return the owner class
         */
        @NotNull
        public Class<?> getOwnerClass() {
            return ownerClass;
        }
        
        /**
         * Gets the event listener.
         * 
         * @return the listener
         */
        @NotNull
        public Consumer<?> getListener() {
            return listener;
        }
    }
}