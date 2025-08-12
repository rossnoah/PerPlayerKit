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

import dev.noah.perplayerkit.api.events.PerPlayerKitEventManager;
import dev.noah.perplayerkit.api.kit.KitAPI;
import dev.noah.perplayerkit.api.player.PlayerAPI;
import dev.noah.perplayerkit.api.gui.GuiAPI;
import dev.noah.perplayerkit.api.data.PlayerDataAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main API interface for PerPlayerKit.
 * 
 * This is the primary entry point for external plugins to interact with PerPlayerKit.
 * Provides access to all major functionality through specialized API interfaces.
 * 
 * <p><strong>Thread Safety:</strong> All API methods are thread-safe and can be called
 * from any thread. Async methods return CompletableFuture for non-blocking operations.</p>
 * 
 * <p><strong>Backwards Compatibility:</strong> This API maintains backwards compatibility
 * with all previous versions. Deprecated methods are clearly marked and alternatives provided.</p>
 * 
 * <h3>Basic Usage Example:</h3>
 * <pre>{@code
 * // Get the API instance
 * PerPlayerKitAPI api = PerPlayerKitAPI.getInstance();
 * 
 * // Work with kits
 * api.kits()
 *    .forPlayer(player)
 *    .saveCurrentInventory(1)
 *    .thenRun(() -> player.sendMessage("Kit saved!"));
 * 
 * // Listen to events
 * api.events().onKitSaved(event -> {
 *     Player player = event.getPlayer();
 *     int slot = event.getSlot();
 *     // Handle kit saved
 * });
 * }</pre>
 * 
 * @since 1.6.3
 * @author Noah Ross
 */
public interface PerPlayerKitAPI {
    
    /**
     * Gets the current API version.
     * 
     * @return the API version string (e.g., "2.0.0")
     */
    @NotNull
    String getAPIVersion();
    
    /**
     * Gets the plugin version.
     * 
     * @return the plugin version string
     */
    @NotNull
    String getPluginVersion();
    
    /**
     * Checks if the API is available and ready for use.
     * 
     * @return true if the API is ready
     */
    boolean isReady();
    
    /**
     * Gets the kit management API.
     * 
     * @return the kit API interface
     */
    @NotNull
    KitAPI kits();
    
    /**
     * Gets the player management API.
     * 
     * @return the player API interface
     */
    @NotNull
    PlayerAPI players();
    
    /**
     * Gets the GUI management API.
     * 
     * @return the GUI API interface
     */
    @NotNull
    GuiAPI gui();
    
    /**
     * Gets the player data API.
     * 
     * @return the player data API interface
     */
    @NotNull
    PlayerDataAPI data();
    
    /**
     * Gets the event management API.
     * 
     * @return the event API interface
     */
    @NotNull
    PerPlayerKitEventManager events();
    
    /**
     * Registers a plugin for API usage tracking and debugging.
     * 
     * <p>This is optional but recommended for better support and debugging.
     * Registered plugins appear in API usage statistics and logs.</p>
     * 
     * @param plugin the plugin to register
     * @return a registration handle that can be used to unregister
     */
    @NotNull
    APIRegistration registerPlugin(@NotNull Plugin plugin);
    
    /**
     * Gets API usage statistics.
     * 
     * @return API usage statistics
     */
    @NotNull
    APIStatistics getStatistics();
    
    /**
     * Creates a new API builder for advanced configuration.
     * 
     * <p>This is useful for plugins that need custom API behavior or configuration.</p>
     * 
     * @return a new API builder
     */
    @NotNull
    APIBuilder builder();
    
    /**
     * Gets the singleton API instance.
     * 
     * <p>This is the standard way to access the PerPlayerKit API.</p>
     * 
     * @return the API instance
     * @throws IllegalStateException if PerPlayerKit is not loaded
     */
    @NotNull
    static PerPlayerKitAPI getInstance() {
        return PerPlayerKitAPIProvider.getInstance();
    }
    
    /**
     * Safely gets the API instance without throwing exceptions.
     * 
     * @return the API instance, or null if PerPlayerKit is not available
     */
    @Nullable
    static PerPlayerKitAPI getInstanceSafe() {
        return PerPlayerKitAPIProvider.getInstanceSafe();
    }
    
    /**
     * Checks if PerPlayerKit is available on the server.
     * 
     * @return true if PerPlayerKit is available
     */
    static boolean isAvailable() {
        return PerPlayerKitAPIProvider.isAvailable();
    }
    
    /**
     * API registration handle for tracking plugin usage.
     */
    interface APIRegistration {
        
        /**
         * Gets the registered plugin.
         * 
         * @return the plugin
         */
        @NotNull
        Plugin getPlugin();
        
        /**
         * Gets the registration timestamp.
         * 
         * @return timestamp in milliseconds
         */
        long getRegistrationTime();
        
        /**
         * Unregisters the plugin.
         */
        void unregister();
        
        /**
         * Checks if this registration is still valid.
         * 
         * @return true if valid
         */
        boolean isValid();
    }
    
    /**
     * API usage statistics.
     */
    interface APIStatistics {
        
        /**
         * Gets the number of registered plugins.
         * 
         * @return registered plugin count
         */
        int getRegisteredPluginCount();
        
        /**
         * Gets the total number of API calls made.
         * 
         * @return total API call count
         */
        long getTotalAPICalls();
        
        /**
         * Gets the number of API calls in the last minute.
         * 
         * @return recent API call count
         */
        long getRecentAPICalls();
        
        /**
         * Gets the average API response time in milliseconds.
         * 
         * @return average response time
         */
        double getAverageResponseTime();
    }
    
    /**
     * Builder for creating customized API instances.
     */
    interface APIBuilder {
        
        /**
         * Enables or disables async operations.
         * 
         * @param enabled true to enable async operations
         * @return this builder
         */
        @NotNull
        APIBuilder withAsync(boolean enabled);
        
        /**
         * Sets the default timeout for async operations.
         * 
         * @param timeoutMs timeout in milliseconds
         * @return this builder
         */
        @NotNull
        APIBuilder withTimeout(long timeoutMs);
        
        /**
         * Enables or disables event firing.
         * 
         * @param enabled true to enable event firing
         * @return this builder
         */
        @NotNull
        APIBuilder withEvents(boolean enabled);
        
        /**
         * Sets custom error handling behavior.
         * 
         * @param failFast true to fail fast on errors
         * @return this builder
         */
        @NotNull
        APIBuilder withFailFast(boolean failFast);
        
        /**
         * Builds the API instance with the specified configuration.
         * 
         * @return configured API instance
         */
        @NotNull
        PerPlayerKitAPI build();
    }
}