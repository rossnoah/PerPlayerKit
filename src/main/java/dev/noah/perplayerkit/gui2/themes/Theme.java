/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.themes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class Theme {
    private String name;
    private String description;
    private Map<String, String> colors = new HashMap<>();
    private Map<String, String> materials = new HashMap<>();
    private Map<String, ComponentStyle> componentStyles = new HashMap<>();
    private Map<String, Object> data = new HashMap<>();
    
    public Theme() {
        // Default theme
        this.name = "Default";
        this.description = "Built-in default theme";
        setupDefaults();
    }
    
    private void setupDefaults() {
        // Default colors
        colors.put("primary", "&9");
        colors.put("secondary", "&3");
        colors.put("accent", "&a");
        colors.put("success", "&a");
        colors.put("warning", "&6");
        colors.put("error", "&c");
        colors.put("muted", "&7");
        colors.put("highlight", "&f");
        
        // Default materials
        materials.put("border", "BLUE_STAINED_GLASS_PANE");
        materials.put("background", "GRAY_STAINED_GLASS_PANE");
        materials.put("button", "STONE_BUTTON");
        materials.put("navigation", "ARROW");
        materials.put("confirm", "LIME_CONCRETE");
        materials.put("cancel", "RED_CONCRETE");
        materials.put("info", "OAK_SIGN");
    }
    
    public static Theme fromConfig(YamlConfiguration config, Plugin plugin) {
        Theme theme = new Theme();
        
        theme.name = config.getString("name", "Unnamed Theme");
        theme.description = config.getString("description", "");
        
        // Load colors
        if (config.isConfigurationSection("colors")) {
            ConfigurationSection colorsSection = config.getConfigurationSection("colors");
            for (String key : colorsSection.getKeys(false)) {
                theme.colors.put(key, colorsSection.getString(key));
            }
        }
        
        // Load materials
        if (config.isConfigurationSection("materials")) {
            ConfigurationSection materialsSection = config.getConfigurationSection("materials");
            for (String key : materialsSection.getKeys(false)) {
                theme.materials.put(key, materialsSection.getString(key));
            }
        }
        
        // Load component styles
        if (config.isConfigurationSection("component_styles")) {
            ConfigurationSection stylesSection = config.getConfigurationSection("component_styles");
            for (String componentType : stylesSection.getKeys(false)) {
                ConfigurationSection styleConfig = stylesSection.getConfigurationSection(componentType);
                if (styleConfig != null) {
                    ComponentStyle style = ComponentStyle.fromConfig(styleConfig, theme);
                    theme.componentStyles.put(componentType, style);
                }
            }
        }
        
        // Load theme data
        if (config.isConfigurationSection("data")) {
            ConfigurationSection dataSection = config.getConfigurationSection("data");
            theme.data.putAll(dataSection.getValues(true));
        }
        
        return theme;
    }
    
    public ComponentStyle getComponentStyle(String componentType) {
        return componentStyles.get(componentType);
    }
    
    public String getColor(String colorName) {
        return colors.getOrDefault(colorName, "&7");
    }
    
    public String getMaterial(String materialName) {
        return materials.getOrDefault(materialName, "STONE");
    }
    
    public Material getMaterialEnum(String materialName) {
        String materialString = getMaterial(materialName);
        try {
            return Material.valueOf(materialString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
    
    public String resolveThemePlaceholder(String text) {
        String result = text;
        
        // Replace color placeholders: {colors.primary}
        for (Map.Entry<String, String> entry : colors.entrySet()) {
            result = result.replace("{colors." + entry.getKey() + "}", entry.getValue());
        }
        
        // Replace material placeholders: {materials.border}
        for (Map.Entry<String, String> entry : materials.entrySet()) {
            result = result.replace("{materials." + entry.getKey() + "}", entry.getValue());
        }
        
        // Replace data placeholders
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        
        return result;
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, String> getColors() { return new HashMap<>(colors); }
    public Map<String, String> getMaterials() { return new HashMap<>(materials); }
    public Map<String, Object> getData() { return new HashMap<>(data); }
}