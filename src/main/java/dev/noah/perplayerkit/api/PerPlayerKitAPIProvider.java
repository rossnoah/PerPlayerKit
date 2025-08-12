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
package dev.noah.perplayerkit.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider class for managing the PerPlayerKit API singleton instance.
 * 
 * This class handles the lifecycle of the API instance and provides
 * thread-safe access to the API.
 */
public final class PerPlayerKitAPIProvider {
    
    private static volatile PerPlayerKitAPI instance;
    private static volatile boolean initialized = false;
    
    private PerPlayerKitAPIProvider() {
        // Utility class - no instantiation
    }
    
    /**
     * Gets the API instance, creating it if necessary.
     * 
     * @return the API instance
     * @throws IllegalStateException if PerPlayerKit is not available
     */
    @NotNull
    public static PerPlayerKitAPI getInstance() {
        PerPlayerKitAPI result = instance;
        if (result == null) {
            synchronized (PerPlayerKitAPIProvider.class) {
                result = instance;
                if (result == null) {
                    result = createInstance();
                    instance = result;
                    initialized = true;
                }
            }
        }
        return result;
    }
    
    /**
     * Safely gets the API instance without throwing exceptions.
     * 
     * @return the API instance, or null if not available
     */
    @Nullable
    public static PerPlayerKitAPI getInstanceSafe() {
        try {
            return getInstance();
        } catch (IllegalStateException e) {
            return null;
        }
    }
    
    /**
     * Checks if PerPlayerKit is available on the server.
     * 
     * @return true if PerPlayerKit is available
     */
    public static boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PerPlayerKit");
        return plugin != null && plugin.isEnabled();
    }
    
    /**
     * Sets the API instance (used internally by the plugin).
     * 
     * @param apiInstance the API instance to set
     */
    public static void setInstance(@NotNull PerPlayerKitAPI apiInstance) {
        synchronized (PerPlayerKitAPIProvider.class) {
            instance = apiInstance;
            initialized = true;
        }
    }
    
    /**
     * Clears the API instance (used during plugin shutdown).
     */
    public static void clearInstance() {
        synchronized (PerPlayerKitAPIProvider.class) {
            instance = null;
            initialized = false;
        }
    }
    
    /**
     * Checks if the API has been initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Creates a new API instance by looking up the plugin.
     * 
     * @return new API instance
     * @throws IllegalStateException if PerPlayerKit is not available
     */
    @NotNull
    private static PerPlayerKitAPI createInstance() {
        if (!isAvailable()) {
            throw new IllegalStateException("PerPlayerKit plugin is not available");
        }
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PerPlayerKit");
        
        // Try to get the API from the plugin instance
        if (plugin instanceof APIProvider) {
            return ((APIProvider) plugin).getAPI();
        }
        
        // Fallback: create a legacy API bridge
        return new LegacyAPIBridge(plugin);
    }
    
    /**
     * Interface for plugins that provide API access.
     */
    public interface APIProvider {
        @NotNull PerPlayerKitAPI getAPI();
    }
}