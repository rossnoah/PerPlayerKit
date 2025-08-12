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
package dev.noah.perplayerkit.config;

import dev.noah.perplayerkit.logging.PerPlayerKitLogger;
import dev.noah.perplayerkit.validation.Validator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Modern configuration management system with type safety, caching, and validation.
 * Provides a fluent API for accessing configuration values with defaults and validation.
 */
public class ConfigurationManager {
    
    private final Plugin plugin;
    private final PerPlayerKitLogger logger;
    private final ConcurrentHashMap<String, Object> configCache;
    private volatile long lastReloadTime;
    
    public ConfigurationManager(@NotNull Plugin plugin, @NotNull PerPlayerKitLogger logger) {
        this.plugin = Validator.requireNonNull(plugin, "plugin");
        this.logger = Validator.requireNonNull(logger, "logger");
        this.configCache = new ConcurrentHashMap<>();
        this.lastReloadTime = System.currentTimeMillis();
    }
    
    /**
     * Reloads the configuration from disk and clears the cache.
     */
    public void reloadConfiguration() {
        plugin.reloadConfig();
        configCache.clear();
        lastReloadTime = System.currentTimeMillis();
        logger.info("Configuration reloaded successfully");
    }
    
    /**
     * Gets the last time the configuration was reloaded.
     *
     * @return the reload timestamp
     */
    public long getLastReloadTime() {
        return lastReloadTime;
    }
    
    /**
     * Gets a string value from the configuration.
     *
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value
     */
    @NotNull
    public String getString(@NotNull String path, @NotNull String defaultValue) {
        return getConfigValue(path, defaultValue, String.class, 
            config -> Optional.ofNullable(config.getString(path)).orElse(defaultValue));
    }
    
    /**
     * Gets an integer value from the configuration.
     *
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value
     */
    public int getInt(@NotNull String path, int defaultValue) {
        return getConfigValue(path, defaultValue, Integer.class, 
            config -> config.getInt(path, defaultValue));
    }
    
    /**
     * Gets a boolean value from the configuration.
     *
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value
     */
    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return getConfigValue(path, defaultValue, Boolean.class, 
            config -> config.getBoolean(path, defaultValue));
    }
    
    /**
     * Gets a double value from the configuration.
     *
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value
     */
    public double getDouble(@NotNull String path, double defaultValue) {
        return getConfigValue(path, defaultValue, Double.class, 
            config -> config.getDouble(path, defaultValue));
    }
    
    /**
     * Gets a long value from the configuration.
     *
     * @param path the configuration path
     * @param defaultValue the default value if not found
     * @return the configuration value
     */
    public long getLong(@NotNull String path, long defaultValue) {
        return getConfigValue(path, defaultValue, Long.class, 
            config -> config.getLong(path, defaultValue));
    }
    
    /**
     * Gets a string list from the configuration.
     *
     * @param path the configuration path
     * @return the configuration value, or empty list if not found
     */
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return getConfigValue(path, List.<String>of(), (Class<List<String>>) (Class<?>) List.class,
            config -> Optional.ofNullable(config.getStringList(path)).orElse(List.of()));
    }
    
    /**
     * Gets a configuration section.
     *
     * @param path the configuration path
     * @return the configuration section, or null if not found
     */
    @Nullable
    public ConfigurationSection getSection(@NotNull String path) {
        return plugin.getConfig().getConfigurationSection(path);
    }
    
    /**
     * Checks if a configuration path exists.
     *
     * @param path the configuration path
     * @return true if the path exists
     */
    public boolean contains(@NotNull String path) {
        return plugin.getConfig().contains(path);
    }
    
    /**
     * Gets all keys in a configuration section.
     *
     * @param path the configuration section path
     * @param deep whether to get nested keys
     * @return the set of keys, or empty set if section not found
     */
    @NotNull
    public Set<String> getKeys(@NotNull String path, boolean deep) {
        ConfigurationSection section = getSection(path);
        return section != null ? section.getKeys(deep) : Set.of();
    }
    
    /**
     * Gets a configuration value with validation.
     *
     * @param path the configuration path
     * @param defaultValue the default value
     * @param validator the validator function
     * @param <T> the type of the value
     * @return the validated configuration value
     * @throws IllegalArgumentException if validation fails
     */
    @NotNull
    public <T> T getValidated(@NotNull String path, @NotNull T defaultValue, 
                             @NotNull Function<T, T> validator) {
        T value = getConfigValue(path, defaultValue, (Class<T>) defaultValue.getClass(),
            config -> {
                Object rawValue = config.get(path, defaultValue);
                return (T) rawValue;
            });
        
        try {
            return validator.apply(value);
        } catch (Exception e) {
            logger.warn(String.format("Invalid configuration value at '%s': %s. Using default: %s", 
                path, value, defaultValue));
            return defaultValue;
        }
    }
    
    /**
     * Sets a configuration value.
     *
     * @param path the configuration path
     * @param value the value to set
     */
    public void set(@NotNull String path, @Nullable Object value) {
        plugin.getConfig().set(path, value);
        configCache.remove(path); // Invalidate cache
    }
    
    /**
     * Saves the configuration to disk.
     */
    public void save() {
        plugin.saveConfig();
        logger.debug("Configuration saved to disk");
    }
    
    /**
     * Gets a configuration value with caching support.
     *
     * @param path the configuration path
     * @param defaultValue the default value
     * @param type the expected type
     * @param supplier the function to get the value from config
     * @param <T> the type of the value
     * @return the configuration value
     */
    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(@NotNull String path, @NotNull T defaultValue, 
                                @NotNull Class<T> type, @NotNull Function<FileConfiguration, T> supplier) {
        Validator.requireNonNull(path, "path");
        Validator.requireNonNull(defaultValue, "defaultValue");
        
        // Check cache first
        Object cached = configCache.get(path);
        if (cached != null && type.isInstance(cached)) {
            return (T) cached;
        }
        
        try {
            T value = supplier.apply(plugin.getConfig());
            if (value != null) {
                configCache.put(path, value);
                return value;
            }
        } catch (Exception e) {
            logger.warn(String.format("Error reading configuration value at '%s': %s. Using default: %s", 
                path, e.getMessage(), defaultValue));
        }
        
        return defaultValue;
    }
    
    /**
     * Configuration builder for complex configurations.
     */
    public static final class ConfigBuilder {
        private final ConfigurationManager manager;
        private final String basePath;
        
        private ConfigBuilder(@NotNull ConfigurationManager manager, @NotNull String basePath) {
            this.manager = manager;
            this.basePath = basePath;
        }
        
        /**
         * Gets a string value relative to the base path.
         *
         * @param subPath the sub-path
         * @param defaultValue the default value
         * @return the configuration value
         */
        @NotNull
        public String getString(@NotNull String subPath, @NotNull String defaultValue) {
            return manager.getString(basePath + "." + subPath, defaultValue);
        }
        
        /**
         * Gets an integer value relative to the base path.
         *
         * @param subPath the sub-path
         * @param defaultValue the default value
         * @return the configuration value
         */
        public int getInt(@NotNull String subPath, int defaultValue) {
            return manager.getInt(basePath + "." + subPath, defaultValue);
        }
        
        /**
         * Gets a boolean value relative to the base path.
         *
         * @param subPath the sub-path
         * @param defaultValue the default value
         * @return the configuration value
         */
        public boolean getBoolean(@NotNull String subPath, boolean defaultValue) {
            return manager.getBoolean(basePath + "." + subPath, defaultValue);
        }
        
        /**
         * Creates a nested builder for the specified sub-path.
         *
         * @param subPath the sub-path
         * @return a new config builder
         */
        @NotNull
        public ConfigBuilder section(@NotNull String subPath) {
            return new ConfigBuilder(manager, basePath + "." + subPath);
        }
    }
    
    /**
     * Creates a configuration builder for the specified base path.
     *
     * @param basePath the base path
     * @return a new config builder
     */
    @NotNull
    public ConfigBuilder section(@NotNull String basePath) {
        return new ConfigBuilder(this, basePath);
    }
}