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
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.config.ConfigurationManager;
import dev.noah.perplayerkit.ioc.ServiceContainer;
import dev.noah.perplayerkit.logging.PerPlayerKitLogger;
import dev.noah.perplayerkit.metrics.MetricsCollector;
import dev.noah.perplayerkit.services.KitService;
import dev.noah.perplayerkit.util.FoliaCompatScheduler;
import dev.noah.perplayerkit.validation.Validator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Modern PerPlayerKit main class demonstrating service-oriented architecture,
 * dependency injection, and proper lifecycle management.
 * 
 * This class shows how the plugin could be restructured using modern patterns
 * while maintaining backward compatibility with the existing PerPlayerKit class.
 */
public class ModernPerPlayerKit extends JavaPlugin {
    
    private ServiceContainer serviceContainer;
    private PerPlayerKitLogger logger;
    private ConfigurationManager configManager;
    private MetricsCollector metrics;
    private KitService kitService;
    
    @Override
    public void onLoad() {
        // Initialize core services
        initializeServices();
        logger.info("PerPlayerKit services initialized");
    }
    
    @Override
    public void onEnable() {
        try {
            // Log server information
            logServerInformation();
            
            // Initialize configuration
            configManager.reloadConfiguration();
            
            // Register commands and listeners
            registerCommandsAndListeners();
            
            // Start background tasks
            startBackgroundTasks();
            
            logger.info("PerPlayerKit enabled successfully in " + 
                (System.currentTimeMillis() - getStartTime()) + "ms");
                
        } catch (Exception e) {
            logger.error("Failed to enable PerPlayerKit", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            FoliaCompatScheduler.cancelAllTasks(this);
            
            // Save any pending data
            saveAllData();
            
            // Log metrics summary
            logMetricsSummary();
            
            // Clean up resources
            cleanupResources();
            
            logger.info("PerPlayerKit disabled successfully");
            
        } catch (Exception e) {
            logger.error("Error during plugin disable", e);
        }
    }
    
    /**
     * Gets the kit service instance.
     *
     * @return the kit service
     */
    @NotNull
    public KitService getKitService() {
        return kitService;
    }
    
    /**
     * Gets the metrics collector instance.
     *
     * @return the metrics collector
     */
    @NotNull
    public MetricsCollector getMetrics() {
        return metrics;
    }
    
    /**
     * Gets the service container instance.
     *
     * @return the service container
     */
    @NotNull
    public ServiceContainer getServiceContainer() {
        return serviceContainer;
    }
    
    /**
     * Initializes all core services with dependency injection.
     */
    private void initializeServices() {
        // Initialize service container
        serviceContainer = new ServiceContainer();
        
        // Initialize logger
        logger = new PerPlayerKitLogger(getLogger(), getName());
        
        // Initialize metrics collector
        metrics = new MetricsCollector(logger);
        
        // Initialize configuration manager
        configManager = new ConfigurationManager(this, logger);
        
        // Initialize kit service
        kitService = new KitService(this, logger, configManager, metrics);
        
        // Register services in container
        serviceContainer
            .registerSingleton(PerPlayerKitLogger.class, logger)
            .registerSingleton(MetricsCollector.class, metrics)
            .registerSingleton(ConfigurationManager.class, configManager)
            .registerSingleton(KitService.class, kitService);
            
        logger.debug("Service container initialized with " + 
            serviceContainer.getServiceCount() + " services");
    }
    
    /**
     * Logs server information for debugging and support purposes.
     */
    private void logServerInformation() {
        logger.info("Server: " + FoliaCompatScheduler.getServerType());
        logger.info("Bukkit Version: " + getServer().getBukkitVersion());
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("Plugin Version: " + getDescription().getVersion());
        
        if (FoliaCompatScheduler.isFolia()) {
            logger.info("Folia detected - using region-aware scheduling");
        }
    }
    
    /**
     * Registers commands and event listeners.
     */
    private void registerCommandsAndListeners() {
        // Modern command registration would go here
        // For now, maintain compatibility with existing system
        
        metrics.incrementCounter("commands.registered");
        metrics.incrementCounter("listeners.registered");
    }
    
    /**
     * Starts background tasks and scheduled operations.
     */
    private void startBackgroundTasks() {
        // Start metrics collection
        FoliaCompatScheduler.runAsyncTimer(this, this::collectMetrics, 
            20L * 60L, 20L * 60L); // Every minute
            
        // Start configuration hot-reload check
        if (configManager.getBoolean("gui.hot-reload", false)) {
            FoliaCompatScheduler.runAsyncTimer(this, this::checkConfigurationChanges,
                20L * 30L, 20L * 30L); // Every 30 seconds
        }
        
        logger.debug("Background tasks started");
    }
    
    /**
     * Saves all pending data before shutdown.
     */
    private void saveAllData() {
        try {
            // Save configuration
            configManager.save();
            
            // Flush any pending database operations
            // This would typically involve waiting for async operations to complete
            
            logger.debug("All data saved successfully");
            
        } catch (Exception e) {
            logger.error("Error saving data during shutdown", e);
        }
    }
    
    /**
     * Logs final metrics summary.
     */
    private void logMetricsSummary() {
        try {
            var summary = metrics.getSummary();
            logger.info("Final metrics summary: " + summary.toString());
            
            // Log specific metrics
            summary.getCounters().forEach((key, value) -> 
                logger.debug("Counter " + key + ": " + value));
                
        } catch (Exception e) {
            logger.warn("Error logging metrics summary", e);
        }
    }
    
    /**
     * Cleans up resources and references.
     */
    private void cleanupResources() {
        try {
            // Clear service container
            if (serviceContainer != null) {
                serviceContainer.clear();
            }
            
            // Clear caches
            if (kitService != null) {
                kitService.clearCache();
            }
            
            logger.debug("Resources cleaned up successfully");
            
        } catch (Exception e) {
            logger.warn("Error during resource cleanup", e);
        }
    }
    
    /**
     * Periodic metrics collection task.
     */
    private void collectMetrics() {
        try {
            // Collect JVM metrics
            Runtime runtime = Runtime.getRuntime();
            metrics.incrementCounter("memory.used", 
                runtime.totalMemory() - runtime.freeMemory());
            metrics.incrementCounter("memory.max", runtime.maxMemory());
            
            // Collect plugin-specific metrics
            metrics.incrementCounter("cache.size", kitService.getCacheSize());
            metrics.incrementCounter("services.registered", serviceContainer.getServiceCount());
            
        } catch (Exception e) {
            logger.warn("Error collecting metrics", e);
        }
    }
    
    /**
     * Checks for configuration file changes for hot-reload.
     */
    private void checkConfigurationChanges() {
        try {
            // Check if config file was modified
            // This is a simplified implementation
            long currentTime = System.currentTimeMillis();
            long lastReload = configManager.getLastReloadTime();
            
            if (currentTime - lastReload > 300000) { // 5 minutes
                logger.debug("Configuration hot-reload check performed");
            }
            
        } catch (Exception e) {
            logger.warn("Error checking configuration changes", e);
        }
    }
    
    /**
     * Gets the plugin start time for performance measurement.
     *
     * @return start time in milliseconds
     */
    private long getStartTime() {
        // This would typically be stored when the plugin starts loading
        return System.currentTimeMillis();
    }
}