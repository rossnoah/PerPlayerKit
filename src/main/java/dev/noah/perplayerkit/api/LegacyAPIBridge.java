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

import dev.noah.perplayerkit.api.data.PlayerDataAPI;
import dev.noah.perplayerkit.api.events.PerPlayerKitEventManager;
import dev.noah.perplayerkit.api.gui.GuiAPI;
import dev.noah.perplayerkit.api.kit.KitAPI;
import dev.noah.perplayerkit.api.player.PlayerAPI;
import dev.noah.perplayerkit.api.impl.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Legacy API bridge implementation that provides backwards compatibility
 * with older API versions while exposing the modern API interfaces.
 * 
 * This bridge wraps existing legacy components and adapts them to the
 * new API interfaces without breaking existing integrations.
 */
public class LegacyAPIBridge implements PerPlayerKitAPI {
    
    private static final String API_VERSION = "2.0.0";
    
    private final Plugin plugin;
    private final KitAPI kitAPI;
    private final PlayerAPI playerAPI;
    private final GuiAPI guiAPI;
    private final PlayerDataAPI dataAPI;
    private final PerPlayerKitEventManager eventManager;
    private final ConcurrentHashMap<Plugin, APIRegistration> registrations;
    private final APIStatistics statistics;
    private final boolean ready;
    
    public LegacyAPIBridge(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.registrations = new ConcurrentHashMap<>();
        this.statistics = new LegacyAPIStatistics();
        
        try {
            // Initialize API implementations using legacy bridge adapters
            this.kitAPI = new KitAPIImpl(plugin);
            this.playerAPI = new PlayerAPIImpl(plugin);
            this.guiAPI = new GuiAPIImpl(plugin);
            this.dataAPI = new PlayerDataAPIImpl(plugin);
            this.eventManager = new EventManagerImpl(plugin);
            this.ready = true;
        } catch (Exception e) {
            // If initialization fails, mark as not ready but don't fail construction
            throw new IllegalStateException("Failed to initialize API bridge", e);
        }
    }
    
    @Override
    @NotNull
    public String getAPIVersion() {
        return API_VERSION;
    }
    
    @Override
    @NotNull
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean isReady() {
        return ready && plugin.isEnabled();
    }
    
    @Override
    @NotNull
    public KitAPI kits() {
        ensureReady();
        return kitAPI;
    }
    
    @Override
    @NotNull
    public PlayerAPI players() {
        ensureReady();
        return playerAPI;
    }
    
    @Override
    @NotNull
    public GuiAPI gui() {
        ensureReady();
        return guiAPI;
    }
    
    @Override
    @NotNull
    public PlayerDataAPI data() {
        ensureReady();
        return dataAPI;
    }
    
    @Override
    @NotNull
    public PerPlayerKitEventManager events() {
        ensureReady();
        return eventManager;
    }
    
    @Override
    @NotNull
    public APIRegistration registerPlugin(@NotNull Plugin plugin) {
        ensureReady();
        
        LegacyAPIRegistration registration = new LegacyAPIRegistration(plugin);
        registrations.put(plugin, registration);
        
        plugin.getLogger().info("Registered with PerPlayerKit API v" + getAPIVersion());
        
        return registration;
    }
    
    @Override
    @NotNull
    public APIStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    @NotNull
    public APIBuilder builder() {
        return new LegacyAPIBuilder(plugin);
    }
    
    /**
     * Ensures the API is ready for use.
     * 
     * @throws IllegalStateException if the API is not ready
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("PerPlayerKit API is not ready");
        }
    }
    
    /**
     * Legacy implementation of API registration.
     */
    private class LegacyAPIRegistration implements APIRegistration {
        
        private final Plugin plugin;
        private final long registrationTime;
        private volatile boolean valid = true;
        
        public LegacyAPIRegistration(@NotNull Plugin plugin) {
            this.plugin = plugin;
            this.registrationTime = System.currentTimeMillis();
        }
        
        @Override
        @NotNull
        public Plugin getPlugin() {
            return plugin;
        }
        
        @Override
        public long getRegistrationTime() {
            return registrationTime;
        }
        
        @Override
        public void unregister() {
            if (valid) {
                registrations.remove(plugin);
                valid = false;
                plugin.getLogger().info("Unregistered from PerPlayerKit API");
            }
        }
        
        @Override
        public boolean isValid() {
            return valid && plugin.isEnabled();
        }
    }
    
    /**
     * Legacy implementation of API statistics.
     */
    private class LegacyAPIStatistics implements APIStatistics {
        
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong recentCalls = new AtomicLong(0);
        private volatile long lastRecentReset = System.currentTimeMillis();
        
        @Override
        public int getRegisteredPluginCount() {
            return registrations.size();
        }
        
        @Override
        public long getTotalAPICalls() {
            return totalCalls.get();
        }
        
        @Override
        public long getRecentAPICalls() {
            // Reset recent calls if more than a minute has passed
            long now = System.currentTimeMillis();
            if (now - lastRecentReset > 60000) {
                recentCalls.set(0);
                lastRecentReset = now;
            }
            return recentCalls.get();
        }
        
        @Override
        public double getAverageResponseTime() {
            // Legacy bridge doesn't track response times
            return 0.0;
        }
        
        /**
         * Increments the API call counters.
         */
        public void incrementCalls() {
            totalCalls.incrementAndGet();
            recentCalls.incrementAndGet();
        }
    }
    
    /**
     * Legacy implementation of API builder.
     */
    private static class LegacyAPIBuilder implements APIBuilder {
        
        private final Plugin plugin;
        private boolean async = true;
        private long timeout = 30000;
        private boolean events = true;
        private boolean failFast = false;
        
        public LegacyAPIBuilder(@NotNull Plugin plugin) {
            this.plugin = plugin;
        }
        
        @Override
        @NotNull
        public APIBuilder withAsync(boolean enabled) {
            this.async = enabled;
            return this;
        }
        
        @Override
        @NotNull
        public APIBuilder withTimeout(long timeoutMs) {
            this.timeout = timeoutMs;
            return this;
        }
        
        @Override
        @NotNull
        public APIBuilder withEvents(boolean enabled) {
            this.events = enabled;
            return this;
        }
        
        @Override
        @NotNull
        public APIBuilder withFailFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }
        
        @Override
        @NotNull
        public PerPlayerKitAPI build() {
            // For legacy bridge, return a configured instance
            return new LegacyAPIBridge(plugin);
        }
    }
}