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
package dev.noah.perplayerkit.gui2.core;

import dev.noah.perplayerkit.gui2.actions.ActionRegistry;
import dev.noah.perplayerkit.gui2.data.DataContext;
import dev.noah.perplayerkit.gui2.templates.TemplateManager;
import dev.noah.perplayerkit.gui2.themes.ThemeManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.ipvp.canvas.Menu;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Central manager for the advanced GUI system.
 * Handles loading, caching, and rendering of config-driven GUIs.
 */
public class GuiManager {
    private static GuiManager instance;
    
    private final Plugin plugin;
    private final Map<String, GuiConfig> loadedGuis = new HashMap<>();
    private final GuiRenderer renderer;
    private final GuiValidator validator;
    private final ThemeManager themeManager;
    private final TemplateManager templateManager;
    private final ActionRegistry actionRegistry;
    
    private boolean hotReloadEnabled = true;
    private long lastReloadCheck = 0;
    
    public GuiManager(Plugin plugin) {
        this.plugin = plugin;
        this.renderer = new GuiRenderer(this);
        this.validator = new GuiValidator();
        this.themeManager = new ThemeManager(plugin);
        this.templateManager = new TemplateManager(plugin);
        this.actionRegistry = new ActionRegistry(plugin);
        
        instance = this;
        initialize();
    }
    
    public static GuiManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GuiManager not initialized");
        }
        return instance;
    }
    
    /**
     * Initialize the GUI system - load all configs, themes, and templates
     */
    public void initialize() {
        plugin.getLogger().info("Initializing Advanced GUI System...");
        
        // Create default directories if they don't exist
        createDefaultDirectories();
        
        // Load system components
        themeManager.loadThemes();
        templateManager.loadTemplates();
        actionRegistry.registerDefaultActions();
        
        // Load GUI configurations
        loadAllGuis();
        
        plugin.getLogger().info("Advanced GUI System initialized with " + loadedGuis.size() + " GUIs");
    }
    
    /**
     * Open a GUI for a player with dynamic data context
     */
    public CompletableFuture<Menu> openGui(Player player, String guiName, DataContext context) {
        // Validate input parameters
        if (player == null) {
            plugin.getLogger().severe("Cannot open GUI: player is null");
            return CompletableFuture.completedFuture(null);
        }
        
        if (guiName == null || guiName.trim().isEmpty()) {
            plugin.getLogger().severe("Cannot open GUI: guiName is null or empty");
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check for hot reload
                if (hotReloadEnabled && shouldCheckReload()) {
                    checkForConfigChanges();
                }
                
                GuiConfig config = loadedGuis.get(guiName);
                if (config == null) {
                    plugin.getLogger().warning("GUI '" + guiName + "' not found. Available GUIs: " + 
                        String.join(", ", loadedGuis.keySet()));
                    return null;
                }
                
                // Create data context with player info
                DataContext finalContext = context != null ? context : new DataContext(player);
                finalContext.setPlayer(player);
                
                // Render the GUI
                Menu menu = renderer.render(config, finalContext);
                if (menu != null) {
                    menu.open(player);
                }
                
                return menu;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error opening GUI '" + guiName + "': " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }
    
    /**
     * Quick open without custom context
     */
    public CompletableFuture<Menu> openGui(Player player, String guiName) {
        return openGui(player, guiName, null);
    }
    
    /**
     * Reload all GUIs from disk
     */
    public void reloadAll() {
        plugin.getLogger().info("Reloading all GUI configurations...");
        
        loadedGuis.clear();
        themeManager.reloadThemes();
        templateManager.reloadTemplates();
        loadAllGuis();
        
        plugin.getLogger().info("Reloaded " + loadedGuis.size() + " GUI configurations");
    }
    
    /**
     * Reload a specific GUI
     */
    public boolean reloadGui(String guiName) {
        File configFile = new File(plugin.getDataFolder(), "gui/configs/" + guiName + ".yml");
        if (!configFile.exists()) {
            return false;
        }
        
        try {
            GuiConfig config = GuiConfig.load(configFile, this);
            if (validator.validate(config)) {
                loadedGuis.put(guiName, config);
                plugin.getLogger().info("Reloaded GUI: " + guiName);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading GUI '" + guiName + "': " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get a loaded GUI config
     */
    public GuiConfig getGuiConfig(String name) {
        return loadedGuis.get(name);
    }
    
    /**
     * Check if a GUI exists
     */
    public boolean hasGui(String name) {
        return loadedGuis.containsKey(name);
    }
    
    /**
     * Get all loaded GUI names
     */
    public String[] getLoadedGuiNames() {
        return loadedGuis.keySet().toArray(new String[0]);
    }
    
    // Getters for system components
    public Plugin getPlugin() { return plugin; }
    public ThemeManager getThemeManager() { return themeManager; }
    public TemplateManager getTemplateManager() { return templateManager; }
    public ActionRegistry getActionRegistry() { return actionRegistry; }
    public GuiValidator getValidator() { return validator; }
    public GuiRenderer getRenderer() { return renderer; }
    
    // Private methods
    
    private void createDefaultDirectories() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        new File(guiDir, "configs").mkdirs();
        new File(guiDir, "themes").mkdirs();
        new File(guiDir, "templates").mkdirs();
        new File(guiDir, "examples").mkdirs();
    }
    
    private void loadAllGuis() {
        File configsDir = new File(plugin.getDataFolder(), "gui/configs");
        if (!configsDir.exists()) {
            return;
        }
        
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                String guiName = file.getName().replace(".yml", "");
                GuiConfig config = GuiConfig.load(file, this);
                
                if (validator.validate(config)) {
                    loadedGuis.put(guiName, config);
                    plugin.getLogger().fine("Loaded GUI: " + guiName);
                } else {
                    plugin.getLogger().warning("Validation failed for GUI: " + guiName);
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading GUI from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private boolean shouldCheckReload() {
        long now = System.currentTimeMillis();
        if (now - lastReloadCheck > 5000) { // Check every 5 seconds
            lastReloadCheck = now;
            return true;
        }
        return false;
    }
    
    private void checkForConfigChanges() {
        // Implementation would check file modification times and reload changed configs
        // This is a simplified version - in production you'd want more sophisticated file watching
    }
    
    public void setHotReloadEnabled(boolean enabled) {
        this.hotReloadEnabled = enabled;
    }
    
    public boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }
}