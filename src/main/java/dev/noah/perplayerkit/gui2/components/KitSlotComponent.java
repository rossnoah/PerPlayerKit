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

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui2.core.GuiManager;
import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A component that displays kit slot information with dynamic states.
 * Changes appearance based on whether the kit exists or not.
 * 
 * Example configuration:
 * type: "kit_slot"
 * slot_number: 1  # Can use {slot_number} placeholder instead
 * states:
 *   exists:
 *     material: KNOWLEDGE_BOOK
 *     name: "&a&lKit {slot_number}"
 *     lore:
 *       - "&7Left click to load"
 *       - "&7Right click to edit"
 *       - "&7Items: {kit_item_count}"
 *   empty:
 *     material: BOOK
 *     name: "&c&lKit {slot_number} - Empty"
 *     lore:
 *       - "&7Click to create this kit"
 * actions:
 *   left_click: "load_kit:{slot_number}"
 *   right_click: "open_gui:kit-editor:{slot_number}"
 */
public class KitSlotComponent extends BaseComponent {
    
    private final GuiManager guiManager;
    private int slotNumber = 1;
    
    // State configurations
    private ComponentState existsState;
    private ComponentState emptyState;
    
    public KitSlotComponent(GuiManager guiManager) {
        super("kit_slot");
        this.guiManager = guiManager;
    }
    
    @Override
    protected void configureSpecific(ConfigurationSection config) {
        // Get slot number (can be dynamic via placeholder)
        if (config.contains("slot_number")) {
            this.slotNumber = config.getInt("slot_number", 1);
        }
        
        // Parse states
        if (config.isConfigurationSection("states")) {
            ConfigurationSection states = config.getConfigurationSection("states");
            
            if (states.isConfigurationSection("exists")) {
                this.existsState = ComponentState.fromConfig(states.getConfigurationSection("exists"));
            }
            
            if (states.isConfigurationSection("empty")) {
                this.emptyState = ComponentState.fromConfig(states.getConfigurationSection("empty"));
            }
        }
        
        // Default states if not configured
        if (existsState == null) {
            existsState = createDefaultExistsState();
        }
        
        if (emptyState == null) {
            emptyState = createDefaultEmptyState();
        }
    }
    
    @Override
    protected ItemStack renderSpecific(ItemStack baseItem, DataContext context) {
        // Resolve slot number from context if needed
        int actualSlot = resolveSlotNumber(context);
        
        // Check if kit exists
        boolean kitExists = KitManager.get().hasKit(context.getPlayer().getUniqueId(), actualSlot);
        
        // Use appropriate state
        ComponentState state = kitExists ? existsState : emptyState;
        return state.apply(baseItem, context, actualSlot);
    }
    
    private int resolveSlotNumber(DataContext context) {
        // If slot_number was configured as a placeholder, resolve it
        String slotStr = context.resolve("{slot_number}");
        if (!slotStr.equals("{slot_number}")) {
            try {
                return Integer.parseInt(slotStr);
            } catch (NumberFormatException e) {
                // Fall back to configured value
            }
        }
        
        return slotNumber;
    }
    
    private ComponentState createDefaultExistsState() {
        ComponentState state = new ComponentState();
        state.material = Material.KNOWLEDGE_BOOK;
        state.name = "&a&lKit {slot_number}";
        state.lore = List.of(
            "&7Left click to load",
            "&7Right click to edit",
            "&7Items: {kit_item_count}"
        );
        return state;
    }
    
    private ComponentState createDefaultEmptyState() {
        ComponentState state = new ComponentState();
        state.material = Material.BOOK;
        state.name = "&c&lKit {slot_number} - Empty";
        state.lore = List.of(
            "&7Click to create this kit"
        );
        return state;
    }
    
    /**
     * Inner class representing a component state with its own material, name, lore, etc.
     */
    private static class ComponentState {
        Material material;
        String name;
        List<String> lore;
        boolean enchanted;
        
        public static ComponentState fromConfig(ConfigurationSection config) {
            ComponentState state = new ComponentState();
            
            if (config.contains("material")) {
                try {
                    state.material = Material.valueOf(config.getString("material").toUpperCase());
                } catch (IllegalArgumentException e) {
                    state.material = Material.STONE;
                }
            }
            
            state.name = config.getString("name", "");
            state.lore = config.getStringList("lore");
            state.enchanted = config.getBoolean("enchanted", false);
            
            return state;
        }
        
        public ItemStack apply(ItemStack item, DataContext context, int slotNumber) {
            // Update material if specified
            if (material != null) {
                item.setType(material);
            }
            
            // Create context with slot number
            DataContext slotContext = context.createChild();
            slotContext.set("slot_number", slotNumber);
            
            // Get kit information for additional placeholders
            if (KitManager.get().hasKit(context.getPlayer().getUniqueId(), slotNumber)) {
                ItemStack[] kit = KitManager.get().getPlayerKit(context.getPlayer().getUniqueId(), slotNumber);
                if (kit != null) {
                    int itemCount = 0;
                    for (ItemStack kitItem : kit) {
                        if (kitItem != null && !kitItem.getType().isAir()) {
                            itemCount++;
                        }
                    }
                    slotContext.set("kit_item_count", itemCount);
                } else {
                    slotContext.set("kit_item_count", 0);
                }
            } else {
                slotContext.set("kit_item_count", 0);
            }
            
            // Apply name and lore with resolved placeholders
            if (item.getItemMeta() != null) {
                var meta = item.getItemMeta();
                
                if (name != null && !name.isEmpty()) {
                    meta.setDisplayName(slotContext.resolve(name));
                }
                
                if (lore != null && !lore.isEmpty()) {
                    List<String> resolvedLore = new ArrayList<>();
                    for (String line : lore) {
                        resolvedLore.add(slotContext.resolve(line));
                    }
                    meta.setLore(resolvedLore);
                }
                
                item.setItemMeta(meta);
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