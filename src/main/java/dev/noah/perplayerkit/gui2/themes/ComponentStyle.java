/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.themes;

import dev.noah.perplayerkit.gui2.data.DataContext;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ComponentStyle {
    private String nameFormat;
    private List<String> loreFormat;
    private String material;
    private boolean enchanted;
    private boolean hideFlags;
    private String hoverEffect;
    
    public ComponentStyle() {
        // Default empty style
    }
    
    public static ComponentStyle fromConfig(ConfigurationSection config, Theme theme) {
        ComponentStyle style = new ComponentStyle();
        
        style.nameFormat = config.getString("name_format");
        style.loreFormat = config.getStringList("lore_format");
        style.material = config.getString("material");
        style.enchanted = config.getBoolean("enchanted", false);
        style.hideFlags = config.getBoolean("hide_flags", false);
        style.hoverEffect = config.getString("hover_effect");
        
        return style;
    }
    
    public ItemStack apply(ItemStack item, DataContext context) {
        if (item == null) return item;
        
        // Apply material change if specified
        if (material != null && !material.isEmpty()) {
            try {
                Material newMaterial = Material.valueOf(material.toUpperCase());
                item.setType(newMaterial);
            } catch (IllegalArgumentException e) {
                // Invalid material, keep original
            }
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        boolean changed = false;
        
        // Apply name formatting
        if (nameFormat != null && !nameFormat.isEmpty()) {
            String currentName = meta.hasDisplayName() ? meta.getDisplayName() : "";
            String formattedName = formatText(nameFormat, context);
            
            // Replace {name} placeholder with current name
            formattedName = formattedName.replace("{name}", stripColors(currentName));
            
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', formattedName));
            changed = true;
        }
        
        // Apply lore formatting
        if (loreFormat != null && !loreFormat.isEmpty()) {
            List<String> currentLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            List<String> newLore = new ArrayList<>();
            
            for (String loreTemplate : loreFormat) {
                String formattedLine = formatText(loreTemplate, context);
                
                // Handle special placeholders
                if (formattedLine.contains("{line}")) {
                    // Replace with current lore lines
                    for (String currentLine : currentLore) {
                        newLore.add(formattedLine.replace("{line}", stripColors(currentLine)));
                    }
                } else {
                    newLore.add(ChatColor.translateAlternateColorCodes('&', formattedLine));
                }
            }
            
            meta.setLore(newLore);
            changed = true;
        }
        
        // Apply flags
        if (hideFlags) {
            meta.addItemFlags(ItemFlag.values());
            changed = true;
        }
        
        if (changed) {
            item.setItemMeta(meta);
        }
        
        // Apply enchantment glow
        if (enchanted) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
            ItemMeta enchantedMeta = item.getItemMeta();
            if (enchantedMeta != null) {
                enchantedMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(enchantedMeta);
            }
        }
        
        return item;
    }
    
    private String formatText(String template, DataContext context) {
        // Apply data context resolution
        String result = context.resolve(template);
        
        // Apply additional formatting
        return result;
    }
    
    private String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }
    
    // Getters
    public String getNameFormat() { return nameFormat; }
    public List<String> getLoreFormat() { return loreFormat; }
    public String getMaterial() { return material; }
    public boolean isEnchanted() { return enchanted; }
    public boolean isHideFlags() { return hideFlags; }
    public String getHoverEffect() { return hoverEffect; }
}