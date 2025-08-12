/*
 * Copyright 2022-2025 Noah Ross
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
 * Component for enderchest slot display with dynamic states
 */
public class EnderchestSlotComponent extends BaseComponent {
    private final GuiManager guiManager;
    private int slotNumber = 1;
    
    private ComponentState existsState;
    private ComponentState emptyState;
    
    public EnderchestSlotComponent(GuiManager manager) {
        super("enderchest_slot");
        this.guiManager = manager;
    }
    
    @Override
    protected void configureSpecific(ConfigurationSection config) {
        this.slotNumber = config.getInt("slot_number", 1);
        
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
        
        // Default states
        if (existsState == null) {
            existsState = createDefaultExistsState();
        }
        
        if (emptyState == null) {
            emptyState = createDefaultEmptyState();
        }
    }
    
    @Override
    protected ItemStack renderSpecific(ItemStack baseItem, DataContext context) {
        int actualSlot = resolveSlotNumber(context);
        boolean ecExists = KitManager.get().hasEC(context.getPlayer().getUniqueId(), actualSlot);
        
        ComponentState state = ecExists ? existsState : emptyState;
        return state.apply(baseItem, context, actualSlot);
    }
    
    private int resolveSlotNumber(DataContext context) {
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
        state.material = Material.ENDER_CHEST;
        state.name = "&3&lEnderchest {slot_number}";
        state.lore = List.of(
            "&7Left click to load",
            "&7Right click to edit",
            "&7Items: {ec_item_count}"
        );
        return state;
    }
    
    private ComponentState createDefaultEmptyState() {
        ComponentState state = new ComponentState();
        state.material = Material.ENDER_EYE;
        state.name = "&3&lEnderchest {slot_number}";
        state.lore = List.of(
            "&7Click to create"
        );
        return state;
    }
    
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
            if (material != null) {
                item.setType(material);
            }
            
            DataContext slotContext = context.createChild();
            slotContext.set("slot_number", slotNumber);
            
            // Get EC information for placeholders
            if (KitManager.get().hasEC(context.getPlayer().getUniqueId(), slotNumber)) {
                ItemStack[] ec = KitManager.get().getPlayerEC(context.getPlayer().getUniqueId(), slotNumber);
                if (ec != null) {
                    int itemCount = 0;
                    for (ItemStack ecItem : ec) {
                        if (ecItem != null && !ecItem.getType().isAir()) {
                            itemCount++;
                        }
                    }
                    slotContext.set("ec_item_count", itemCount);
                } else {
                    slotContext.set("ec_item_count", 0);
                }
            } else {
                slotContext.set("ec_item_count", 0);
            }
            
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