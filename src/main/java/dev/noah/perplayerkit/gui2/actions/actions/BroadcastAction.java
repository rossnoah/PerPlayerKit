/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.actions.actions;

import dev.noah.perplayerkit.gui2.actions.ActionHandler;
import dev.noah.perplayerkit.gui2.data.DataContext;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Action to broadcast a message to all players
 */
public class BroadcastAction extends ActionHandler {
    private final String message;
    
    public BroadcastAction(String message) {
        this.message = message;
    }
    
    @Override
    public void execute(Player player, DataContext context, Object clickInfo) {
        String resolvedMessage = context.resolve(message);
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', resolvedMessage);
        Bukkit.broadcastMessage(coloredMessage);
    }
}