/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.themes;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private final Plugin plugin;
    private final Map<String, Theme> themes = new HashMap<>();
    
    public ThemeManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadThemes() {
        File themesDir = new File(plugin.getDataFolder(), "gui/themes");
        if (!themesDir.exists()) {
            themesDir.mkdirs();
            createDefaultTheme();
            return;
        }
        
        File[] themeFiles = themesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (themeFiles == null) return;
        
        for (File themeFile : themeFiles) {
            try {
                String themeName = themeFile.getName().replace(".yml", "");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(themeFile);
                
                Theme theme = Theme.fromConfig(config, plugin);
                themes.put(themeName, theme);
                
                plugin.getLogger().info("Loaded theme: " + themeName);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load theme from " + themeFile.getName() + ": " + e.getMessage());
            }
        }
    }
    
    public void reloadThemes() {
        themes.clear();
        loadThemes();
    }
    
    public Theme getTheme(String name) {
        return themes.getOrDefault(name, getDefaultTheme());
    }
    
    private Theme getDefaultTheme() {
        return themes.getOrDefault("default", new Theme()); // Basic theme
    }
    
    private void createDefaultTheme() {
        // The default theme file is already created in resources/gui/themes/default.yml
        // This method could copy it from resources if needed
    }
    
    public String[] getLoadedThemes() {
        return themes.keySet().toArray(new String[0]);
    }
    
    public boolean hasTheme(String name) {
        return themes.containsKey(name);
    }
}