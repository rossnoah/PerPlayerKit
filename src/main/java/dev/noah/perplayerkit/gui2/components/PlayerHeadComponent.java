/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.components;

import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Component that displays player heads with configurable owners
 */
public class PlayerHeadComponent extends BaseComponent {
    private String targetPlayer = "{player_name}"; // Default to current player
    
    public PlayerHeadComponent() {
        super("player_head");
        this.material = Material.PLAYER_HEAD;
    }
    
    @Override
    protected void configureSpecific(ConfigurationSection config) {
        this.targetPlayer = config.getString("player", "{player_name}");
    }
    
    @Override
    protected ItemStack renderSpecific(ItemStack baseItem, DataContext context) {
        if (baseItem.getType() != Material.PLAYER_HEAD) {
            baseItem.setType(Material.PLAYER_HEAD);
        }
        
        String resolvedPlayerName = context.resolve(targetPlayer);
        
        SkullMeta meta = (SkullMeta) baseItem.getItemMeta();
        if (meta != null) {
            // Try to get online player first
            Player onlinePlayer = Bukkit.getPlayer(resolvedPlayerName);
            if (onlinePlayer != null) {
                meta.setOwningPlayer(onlinePlayer);
            } else {
                // Try to get offline player by UUID if possible, otherwise by name
                try {
                    java.util.UUID playerUUID = java.util.UUID.fromString(resolvedPlayerName);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                    meta.setOwningPlayer(offlinePlayer);
                } catch (IllegalArgumentException e) {
                    // If not a UUID, treat as name - this is deprecated but necessary fallback
                    @SuppressWarnings("deprecation")
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(resolvedPlayerName);
                    meta.setOwningPlayer(offlinePlayer);
                }
            }
            
            baseItem.setItemMeta(meta);
        }
        
        return baseItem;
    }
}