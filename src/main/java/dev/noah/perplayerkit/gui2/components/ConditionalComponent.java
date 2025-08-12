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

import dev.noah.perplayerkit.gui2.data.ConditionEvaluator;
import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A component that renders different items based on conditions.
 * This is extremely powerful for creating dynamic interfaces.
 * 
 * Example configuration:
 * type: "conditional"
 * condition:
 *   type: "has_permission"
 *   permission: "perplayerkit.admin"
 * true:
 *   material: COMMAND_BLOCK
 *   name: "&c&lAdmin Panel"
 *   lore:
 *     - "&7You have admin access"
 *   actions:
 *     click: "open_gui:admin-panel"
 * false:
 *   material: BARRIER
 *   name: "&c&lNo Access"
 *   lore:
 *     - "&7You need admin permissions"
 * 
 * Advanced example with multiple conditions:
 * type: "conditional"
 * conditions:
 *   - condition:
 *       type: "has_kit"
 *       slot: "{slot_number}"
 *     true:
 *       material: KNOWLEDGE_BOOK
 *       name: "&a&lKit Exists"
 *   - condition:
 *       type: "player_online"
 *       player: "{target_player}"
 *     true:
 *       enchanted: true
 *       lore:
 *         - "&aPlayer is online"
 *     false:
 *       lore:
 *         - "&cPlayer is offline"
 * default:
 *   material: BOOK
 *   name: "&7Default State"
 */
public class ConditionalComponent extends BaseComponent {
    
    private List<ConditionalBranch> branches = new ArrayList<>();
    private ComponentState defaultState;
    
    public ConditionalComponent() {
        super("conditional");
    }
    
    @Override
    protected void configureSpecific(ConfigurationSection config) {
        // Single condition with true/false branches
        if (config.isConfigurationSection("condition")) {
            ConfigurationSection conditionSection = config.getConfigurationSection("condition");
            ComponentCondition condition = ComponentCondition.create(conditionSection.getValues(false));
            
            if (condition != null) {
                ConditionalBranch branch = new ConditionalBranch();
                branch.condition = condition;
                
                if (config.isConfigurationSection("true")) {
                    branch.trueState = ComponentState.fromConfig(config.getConfigurationSection("true"));
                }
                
                if (config.isConfigurationSection("false")) {
                    branch.falseState = ComponentState.fromConfig(config.getConfigurationSection("false"));
                }
                
                branches.add(branch);
            }
        }
        
        // Multiple conditions
        if (config.isList("conditions")) {
            List<Map<?, ?>> conditionMaps = config.getMapList("conditions");
            
            for (Map<?, ?> conditionMap : conditionMaps) {
                ConfigurationSection conditionSection = config.createSection("temp");
                Map<String, Object> stringObjectMap = new HashMap<>();
                conditionMap.forEach((key, value) -> stringObjectMap.put(String.valueOf(key), value));
                conditionSection.getRoot().createSection("temp", stringObjectMap);
                conditionSection = conditionSection.getRoot().getConfigurationSection("temp");
                
                ConditionalBranch branch = parseConditionalBranch(conditionSection);
                if (branch != null) {
                    branches.add(branch);
                }
            }
        }
        
        // Default state
        if (config.isConfigurationSection("default")) {
            this.defaultState = ComponentState.fromConfig(config.getConfigurationSection("default"));
        }
    }
    
    private ConditionalBranch parseConditionalBranch(ConfigurationSection config) {
        if (!config.isConfigurationSection("condition")) {
            return null;
        }
        
        ComponentCondition condition = ComponentCondition.create(
            config.getConfigurationSection("condition").getValues(false)
        );
        
        if (condition == null) {
            return null;
        }
        
        ConditionalBranch branch = new ConditionalBranch();
        branch.condition = condition;
        
        if (config.isConfigurationSection("true")) {
            branch.trueState = ComponentState.fromConfig(config.getConfigurationSection("true"));
        }
        
        if (config.isConfigurationSection("false")) {
            branch.falseState = ComponentState.fromConfig(config.getConfigurationSection("false"));
        }
        
        return branch;
    }
    
    @Override
    protected ItemStack renderSpecific(ItemStack baseItem, DataContext context) {
        // Evaluate conditions and apply the first matching state
        for (ConditionalBranch branch : branches) {
            if (branch.condition.evaluate(context)) {
                if (branch.trueState != null) {
                    return branch.trueState.apply(baseItem, context);
                }
            } else {
                if (branch.falseState != null) {
                    return branch.falseState.apply(baseItem, context);
                }
            }
        }
        
        // No conditions matched, use default state
        if (defaultState != null) {
            return defaultState.apply(baseItem, context);
        }
        
        // No default state, return base item
        return baseItem;
    }
    
    /**
     * Represents a conditional branch with a condition and true/false states
     */
    private static class ConditionalBranch {
        ComponentCondition condition;
        ComponentState trueState;
        ComponentState falseState;
    }
    
    /**
     * Represents a component state that can be applied to modify an ItemStack
     */
    private static class ComponentState {
        Material material;
        String name;
        List<String> lore;
        boolean enchanted;
        int amount = 1;
        
        public static ComponentState fromConfig(ConfigurationSection config) {
            ComponentState state = new ComponentState();
            
            if (config.contains("material")) {
                try {
                    state.material = Material.valueOf(config.getString("material").toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Keep null to not override
                }
            }
            
            state.name = config.getString("name");
            
            if (config.isList("lore")) {
                state.lore = config.getStringList("lore");
            }
            
            state.enchanted = config.getBoolean("enchanted", false);
            state.amount = config.getInt("amount", 1);
            
            return state;
        }
        
        public ItemStack apply(ItemStack item, DataContext context) {
            // Apply material change
            if (material != null) {
                item.setType(material);
            }
            
            // Apply amount change
            if (amount != 1) {
                item.setAmount(amount);
            }
            
            // Apply metadata changes
            if (item.getItemMeta() != null) {
                var meta = item.getItemMeta();
                boolean changed = false;
                
                if (name != null) {
                    meta.setDisplayName(context.resolve(name));
                    changed = true;
                }
                
                if (lore != null) {
                    List<String> resolvedLore = new ArrayList<>();
                    for (String line : lore) {
                        resolvedLore.add(context.resolve(line));
                    }
                    meta.setLore(resolvedLore);
                    changed = true;
                }
                
                if (changed) {
                    item.setItemMeta(meta);
                }
            }
            
            // Apply enchantment glow
            if (enchanted) {
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
                if (item.getItemMeta() != null) {
                    var meta = item.getItemMeta();
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
            }
            
            return item;
        }
    }
}