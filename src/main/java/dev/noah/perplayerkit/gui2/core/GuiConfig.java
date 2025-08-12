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

import dev.noah.perplayerkit.gui2.components.BaseComponent;
import dev.noah.perplayerkit.gui2.components.ComponentFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a complete GUI configuration loaded from YAML.
 * Handles parsing of all GUI elements including layout, components, themes, and templates.
 */
public class GuiConfig {
    private final String name;
    private final String title;
    private final int size;
    private final String theme;
    private final List<String> templates;
    private final Map<String, Object> settings;
    private final Map<String, BaseComponent> components;
    private final Map<Integer, String> slotMappings;
    private final Map<String, Object> data;
    private final File sourceFile;
    private final long lastModified;
    
    // Slot expression patterns
    private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
    private static final Pattern GRID_PATTERN = Pattern.compile("grid_(\\d+)x(\\d+)(?::(\\d+),(\\d+))?");
    
    private GuiConfig(Builder builder) {
        this.name = builder.name;
        this.title = builder.title;
        this.size = builder.size;
        this.theme = builder.theme;
        this.templates = new ArrayList<>(builder.templates);
        this.settings = new HashMap<>(builder.settings);
        this.components = new HashMap<>(builder.components);
        this.slotMappings = new HashMap<>(builder.slotMappings);
        this.data = new HashMap<>(builder.data);
        this.sourceFile = builder.sourceFile;
        this.lastModified = builder.lastModified;
    }
    
    /**
     * Load a GUI configuration from a YAML file
     */
    public static GuiConfig load(File file, GuiManager guiManager) throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = file.getName().replace(".yml", "");
        
        Builder builder = new Builder(name, file);
        
        // Basic properties
        builder.title(config.getString("title", "&9GUI"));
        builder.size(config.getInt("size", 54));
        builder.theme(config.getString("theme", "default"));
        
        // Templates
        if (config.isList("templates")) {
            builder.templates(config.getStringList("templates"));
        }
        
        // Settings
        if (config.isConfigurationSection("settings")) {
            ConfigurationSection settingsSection = config.getConfigurationSection("settings");
            builder.settings(settingsSection.getValues(true));
        }
        
        // Data (default values for placeholders)
        if (config.isConfigurationSection("data")) {
            ConfigurationSection dataSection = config.getConfigurationSection("data");
            builder.data(dataSection.getValues(true));
        }
        
        // Parse layout if present
        if (config.isConfigurationSection("layout")) {
            parseLayout(config.getConfigurationSection("layout"), builder);
        }
        
        // Parse slots configuration
        if (config.isConfigurationSection("slots")) {
            parseSlots(config.getConfigurationSection("slots"), builder, guiManager);
        }
        
        // Parse components
        if (config.isConfigurationSection("components")) {
            parseComponents(config.getConfigurationSection("components"), builder, guiManager);
        }
        
        return builder.build();
    }
    
    /**
     * Parse layout section - handles visual grid layouts
     */
    private static void parseLayout(ConfigurationSection layout, Builder builder) {
        String type = layout.getString("type", "grid");
        
        if ("grid".equals(type) && layout.isList("pattern")) {
            List<String> pattern = layout.getStringList("pattern");
            ConfigurationSection definitions = layout.getConfigurationSection("definitions");
            
            if (definitions != null) {
                parseGridPattern(pattern, definitions, builder);
            }
        }
    }
    
    /**
     * Parse visual grid pattern like:
     * pattern:
     *   - "xxxxxxxxx"
     *   - "x1234567x"  
     *   - "xabcdefgx"
     * definitions:
     *   x: "border"
     *   1-7: "kit_slots"
     */
    private static void parseGridPattern(List<String> pattern, ConfigurationSection definitions, Builder builder) {
        for (int row = 0; row < pattern.size(); row++) {
            String line = pattern.get(row);
            for (int col = 0; col < line.length(); col++) {
                char symbol = line.charAt(col);
                int slot = row * 9 + col;
                
                String componentName = definitions.getString(String.valueOf(symbol));
                if (componentName != null) {
                    builder.slotMapping(slot, componentName);
                }
            }
        }
    }
    
    /**
     * Parse slots section - handles flexible slot expressions
     */
    private static void parseSlots(ConfigurationSection slots, Builder builder, GuiManager guiManager) {
        for (String slotExpr : slots.getKeys(false)) {
            ConfigurationSection slotConfig = slots.getConfigurationSection(slotExpr);
            if (slotConfig == null) continue;
            
            // Parse slot expression into actual slot numbers
            Set<Integer> slotNumbers = parseSlotExpression(slotExpr, builder.size);
            
            // Create component for these slots
            BaseComponent component = ComponentFactory.createComponent(slotConfig, guiManager);
            if (component != null) {
                String componentName = "slot_" + slotExpr.replaceAll("[^a-zA-Z0-9]", "_");
                builder.component(componentName, component);
                
                // Map slots to this component
                for (int slot : slotNumbers) {
                    builder.slotMapping(slot, componentName);
                }
            }
        }
    }
    
    /**
     * Parse components section - handles named reusable components
     */
    private static void parseComponents(ConfigurationSection components, Builder builder, GuiManager guiManager) {
        for (String componentName : components.getKeys(false)) {
            ConfigurationSection componentConfig = components.getConfigurationSection(componentName);
            if (componentConfig == null) continue;
            
            BaseComponent component = ComponentFactory.createComponent(componentConfig, guiManager);
            if (component != null) {
                builder.component(componentName, component);
            }
        }
    }
    
    /**
     * Parse slot expressions into actual slot numbers.
     * Supports:
     * - Single numbers: "10"
     * - Ranges: "10-16"
     * - Lists: "10,12,14"
     * - Special expressions: "center_3x3", "edges", "corners"
     * - Grid expressions: "grid_3x3", "grid_3x3:1,1"
     */
    public static Set<Integer> parseSlotExpression(String expression, int guiSize) {
        Set<Integer> slots = new HashSet<>();
        
        // Handle special expressions
        switch (expression.toLowerCase()) {
            case "all":
                for (int i = 0; i < guiSize; i++) slots.add(i);
                return slots;
                
            case "edges":
                return getEdgeSlots(guiSize);
                
            case "corners":
                return getCornerSlots(guiSize);
                
            case "center":
                slots.add(guiSize / 2);
                return slots;
                
            case "center_3x3":
                return getCenterGrid(guiSize, 3, 3);
        }
        
        // Handle grid expressions
        Matcher gridMatcher = GRID_PATTERN.matcher(expression);
        if (gridMatcher.matches()) {
            int width = Integer.parseInt(gridMatcher.group(1));
            int height = Integer.parseInt(gridMatcher.group(2));
            int startX = gridMatcher.group(3) != null ? Integer.parseInt(gridMatcher.group(3)) : 0;
            int startY = gridMatcher.group(4) != null ? Integer.parseInt(gridMatcher.group(4)) : 0;
            return getGridSlots(width, height, startX, startY);
        }
        
        // Handle comma-separated list
        if (expression.contains(",")) {
            for (String part : expression.split(",")) {
                slots.addAll(parseSlotExpression(part.trim(), guiSize));
            }
            return slots;
        }
        
        // Handle ranges
        Matcher rangeMatcher = RANGE_PATTERN.matcher(expression);
        if (rangeMatcher.matches()) {
            int start = Integer.parseInt(rangeMatcher.group(1));
            int end = Integer.parseInt(rangeMatcher.group(2));
            for (int i = start; i <= end; i++) {
                if (i >= 0 && i < guiSize) slots.add(i);
            }
            return slots;
        }
        
        // Handle single number
        try {
            int slot = Integer.parseInt(expression);
            if (slot >= 0 && slot < guiSize) {
                slots.add(slot);
            }
        } catch (NumberFormatException e) {
            // Invalid expression, return empty set
        }
        
        return slots;
    }
    
    // Helper methods for special slot calculations
    private static Set<Integer> getEdgeSlots(int size) {
        Set<Integer> edges = new HashSet<>();
        int rows = size / 9;
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            edges.add(i); // Top row
            edges.add((rows - 1) * 9 + i); // Bottom row
        }
        
        // Left and right columns (excluding corners already added)
        for (int row = 1; row < rows - 1; row++) {
            edges.add(row * 9); // Left column
            edges.add(row * 9 + 8); // Right column
        }
        
        return edges;
    }
    
    private static Set<Integer> getCornerSlots(int size) {
        Set<Integer> corners = new HashSet<>();
        int rows = size / 9;
        corners.add(0); // Top-left
        corners.add(8); // Top-right
        corners.add((rows - 1) * 9); // Bottom-left
        corners.add((rows - 1) * 9 + 8); // Bottom-right
        return corners;
    }
    
    private static Set<Integer> getCenterGrid(int size, int width, int height) {
        Set<Integer> center = new HashSet<>();
        int rows = size / 9;
        int centerRow = rows / 2;
        int centerCol = 4; // Center column of 9-wide inventory
        
        int startRow = centerRow - height / 2;
        int startCol = centerCol - width / 2;
        
        return getGridSlots(width, height, startCol, startRow);
    }
    
    private static Set<Integer> getGridSlots(int width, int height, int startX, int startY) {
        Set<Integer> grid = new HashSet<>();
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int slot = (startY + row) * 9 + (startX + col);
                grid.add(slot);
            }
        }
        
        return grid;
    }
    
    // Getters
    public String getName() { return name; }
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public String getTheme() { return theme; }
    public List<String> getTemplates() { return templates; }
    public Map<String, Object> getSettings() { return settings; }
    public Map<String, BaseComponent> getComponents() { return components; }
    public Map<Integer, String> getSlotMappings() { return slotMappings; }
    public Map<String, Object> getData() { return data; }
    public File getSourceFile() { return sourceFile; }
    public long getLastModified() { return lastModified; }
    
    public BaseComponent getComponentForSlot(int slot) {
        String componentName = slotMappings.get(slot);
        return componentName != null ? components.get(componentName) : null;
    }
    
    public Object getSetting(String key, Object defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
    
    // Builder pattern
    public static class Builder {
        private final String name;
        private final File sourceFile;
        private final long lastModified;
        private String title = "&9GUI";
        private int size = 54;
        private String theme = "default";
        private List<String> templates = new ArrayList<>();
        private Map<String, Object> settings = new HashMap<>();
        private Map<String, BaseComponent> components = new HashMap<>();
        private Map<Integer, String> slotMappings = new HashMap<>();
        private Map<String, Object> data = new HashMap<>();
        
        public Builder(String name, File sourceFile) {
            this.name = name;
            this.sourceFile = sourceFile;
            this.lastModified = sourceFile.lastModified();
        }
        
        public Builder title(String title) { this.title = title; return this; }
        public Builder size(int size) { this.size = size; return this; }
        public Builder theme(String theme) { this.theme = theme; return this; }
        public Builder templates(List<String> templates) { this.templates = templates; return this; }
        public Builder settings(Map<String, Object> settings) { this.settings = settings; return this; }
        public Builder component(String name, BaseComponent component) { this.components.put(name, component); return this; }
        public Builder slotMapping(int slot, String componentName) { this.slotMappings.put(slot, componentName); return this; }
        public Builder data(Map<String, Object> data) { this.data = data; return this; }
        
        public GuiConfig build() {
            return new GuiConfig(this);
        }
    }
}