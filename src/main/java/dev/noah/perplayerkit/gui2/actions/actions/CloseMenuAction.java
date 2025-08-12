/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.actions.actions;

import dev.noah.perplayerkit.gui2.actions.ActionHandler;
import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.entity.Player;

/**
 * Action to close the current menu
 */
public class CloseMenuAction extends ActionHandler {
    
    @Override
    public void execute(Player player, DataContext context, Object clickInfo) {
        player.closeInventory();
    }
}