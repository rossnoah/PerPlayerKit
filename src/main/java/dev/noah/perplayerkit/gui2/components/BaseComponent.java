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
package dev.noah.perplayerkit.gui2.components;

import dev.noah.perplayerkit.gui2.actions.ActionHandler;
import dev.noah.perplayerkit.gui2.data.ConditionEvaluator;
import dev.noah.perplayerkit.gui2.data.DataContext;
import dev.noah.perplayerkit.gui2.data.PlaceholderResolver;
import dev.noah.perplayerkit.gui2.themes.ComponentStyle;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.slot.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all GUI components.
 * Components are reusable GUI elements that can be configured through YAML.
 */
public abstract class BaseComponent {
    
    protected String type;
    protected Material material;
    protected int amount;
    protected String name;
    protected List<String> lore;
    protected Map<String, Object> properties;
    protected Map<String, List<ActionHandler>> actions;
    protected List<ComponentCondition> conditions;
    protected boolean hideFlags;
    protected boolean enchanted;
    
    // Animation properties
    protected Map<String, Object> animations;
    
    // Style properties  
    protected String style;
    
    public BaseComponent(String type) {
        this.type = type;
        this.material = Material.STONE;
        this.amount = 1;
        this.name = "";
        this.lore = new ArrayList<>();
        this.properties = new HashMap<>();
        this.actions = new HashMap<>();
        this.conditions = new ArrayList<>();
        this.hideFlags = false;
        this.enchanted = false;
        this.animations = new HashMap<>();
    }
    
    /**
     * Configure this component from a YAML configuration section
     */
    public void configure(ConfigurationSection config) {
        // Basic item properties
        if (config.contains("material")) {
            try {
                this.material = Material.valueOf(config.getString("material").toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid material, keep default
            }
        }
        
        this.amount = config.getInt("amount", 1);
        this.name = config.getString("name", "");
        this.lore = config.getStringList("lore");
        this.hideFlags = config.getBoolean("hide_flags", false);
        this.enchanted = config.getBoolean("enchanted", false);
        this.style = config.getString("style");
        
        // Properties (custom data for specific component types)
        if (config.isConfigurationSection("properties")) {
            this.properties.putAll(config.getConfigurationSection("properties").getValues(false));
        }
        
        // Actions
        if (config.isConfigurationSection("actions")) {
            parseActions(config.getConfigurationSection("actions"));
        }
        
        // Conditions
        if (config.isList("conditions")) {
            parseConditions(config.getMapList("conditions"));
        }
        
        // Animations
        if (config.isConfigurationSection("animations")) {
            this.animations.putAll(config.getConfigurationSection("animations").getValues(true));
        }
        
        // Component-specific configuration
        configureSpecific(config);
    }
    
    /**
     * Override this method to handle component-specific configuration
     */
    protected void configureSpecific(ConfigurationSection config) {
        // Default implementation does nothing
    }
    
    /**
     * Render this component to an ItemStack with the given data context
     */
    public ItemStack render(DataContext context) {
        // Check conditions first
        if (!evaluateConditions(context)) {
            return null; // Don't render if conditions fail
        }
        
        // Create the base item
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        // Apply name with placeholder resolution
        String resolvedName = PlaceholderResolver.resolve(name, context);
        if (!resolvedName.isEmpty()) {
            meta.setDisplayName(resolvedName);
        }
        
        // Apply lore with placeholder resolution
        if (!lore.isEmpty()) {
            List<String> resolvedLore = new ArrayList<>();
            for (String line : lore) {
                resolvedLore.add(PlaceholderResolver.resolve(line, context));
            }
            meta.setLore(resolvedLore);
        }
        
        // Apply flags
        if (hideFlags) {
            meta.addItemFlags(ItemFlag.values());
        }
        
        item.setItemMeta(meta);
        
        // Apply enchantment glow
        if (enchanted) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
            ItemMeta enchantedMeta = item.getItemMeta();
            if (enchantedMeta != null) {
                enchantedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(enchantedMeta);
            }
        }
        
        // Component-specific rendering
        return renderSpecific(item, context);
    }
    
    /**
     * Override this method to handle component-specific rendering
     */
    protected ItemStack renderSpecific(ItemStack baseItem, DataContext context) {
        return baseItem;
    }
    
    /**
     * Apply this component to a menu slot
     */
    public void applyToSlot(Slot slot, DataContext context, ComponentStyle style) {
        ItemStack item = render(context);
        if (item != null) {
            slot.setItem(item);
            
            // Apply style if provided
            if (style != null) {
                item = style.apply(item, context);
                slot.setItem(item);
            }
            
            // Set up click handler
            if (!actions.isEmpty()) {
                slot.setClickHandler((player, info) -> handleClick(player, info, context));
            }
            
            // Apply slot-specific configuration
            applySlotSpecific(slot, context);
        }
    }
    
    /**
     * Override this method to handle slot-specific configuration
     */
    protected void applySlotSpecific(Slot slot, DataContext context) {
        // Default implementation does nothing
    }
    
    /**
     * Handle click events on this component
     */
    protected void handleClick(Player player, Object info, DataContext context) {
        // String clickType = info.getClickType().toString().toLowerCase();
        String clickType = "click"; // Simplified for now
        List<ActionHandler> clickActions = actions.get(clickType);
        
        if (clickActions != null) {
            for (ActionHandler action : clickActions) {
                action.execute(player, context, info);
            }
        }
        
        // Try generic "click" actions if no specific ones found
        if (clickActions == null) {
            clickActions = actions.get("click");
            if (clickActions != null) {
                for (ActionHandler action : clickActions) {
                    action.execute(player, context, info);
                }
            }
        }
    }
    
    /**
     * Check if this component should be rendered based on its conditions
     */
    protected boolean evaluateConditions(DataContext context) {
        if (conditions.isEmpty()) {
            return true;
        }
        
        for (ComponentCondition condition : conditions) {
            if (!condition.evaluate(context)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Update this component (called periodically for dynamic components)
     */
    public void update(DataContext context) {
        // Default implementation does nothing
        // Override in dynamic components
    }
    
    // Parse helper methods
    
    private void parseActions(ConfigurationSection actionsConfig) {
        for (String actionType : actionsConfig.getKeys(false)) {
            if (actionsConfig.isList(actionType)) {
                // Multiple actions for this type
                List<Map<?, ?>> actionList = actionsConfig.getMapList(actionType);
                List<ActionHandler> handlers = new ArrayList<>();
                
                for (Map<?, ?> actionData : actionList) {
                    ActionHandler handler = ActionHandler.create(actionData);
                    if (handler != null) {
                        handlers.add(handler);
                    }
                }
                
                if (!handlers.isEmpty()) {
                    actions.put(actionType, handlers);
                }
            } else if (actionsConfig.isString(actionType)) {
                // Simple string action
                String actionString = actionsConfig.getString(actionType);
                ActionHandler handler = ActionHandler.createFromString(actionString);
                if (handler != null) {
                    List<ActionHandler> handlers = new ArrayList<>();
                    handlers.add(handler);
                    actions.put(actionType, handlers);
                }
            } else {
                // Single action object
                Map<String, Object> actionData = actionsConfig.getConfigurationSection(actionType).getValues(false);
                ActionHandler handler = ActionHandler.create(actionData);
                if (handler != null) {
                    List<ActionHandler> handlers = new ArrayList<>();
                    handlers.add(handler);
                    actions.put(actionType, handlers);
                }
            }
        }
    }
    
    private void parseConditions(List<Map<?, ?>> conditionList) {
        for (Map<?, ?> conditionData : conditionList) {
            ComponentCondition condition = ComponentCondition.create(conditionData);
            if (condition != null) {
                conditions.add(condition);
            }
        }
    }
    
    // Getters
    public String getType() { return type; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public Map<String, Object> getProperties() { return properties; }
    public Object getProperty(String key) { return properties.get(key); }
    public Object getProperty(String key, Object defaultValue) { return properties.getOrDefault(key, defaultValue); }
    
    // Setters for dynamic updates
    public void setMaterial(Material material) { this.material = material; }
    public void setAmount(int amount) { this.amount = amount; }
    public void setName(String name) { this.name = name; }
    public void setLore(List<String> lore) { this.lore = lore; }
    
    /**
     * Inner class representing a condition for component rendering
     */
    public static class ComponentCondition {
        private String type;
        private Map<String, Object> parameters;
        
        public ComponentCondition(String type, Map<String, Object> parameters) {
            this.type = type;
            this.parameters = parameters;
        }
        
        public boolean evaluate(DataContext context) {
            return ConditionEvaluator.evaluate(type, parameters, context);
        }
        
        public static ComponentCondition create(Map<?, ?> data) {
            Object typeObj = data.get("type");
            if (typeObj == null) return null;
            
            String type = typeObj.toString();
            Map<String, Object> params = new HashMap<>();
            
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                if (!"type".equals(entry.getKey().toString())) {
                    params.put(entry.getKey().toString(), entry.getValue());
                }
            }
            
            return new ComponentCondition(type, params);
        }
    }
}