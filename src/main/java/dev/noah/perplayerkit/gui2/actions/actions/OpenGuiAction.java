/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.actions.actions;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.gui2.actions.ActionHandler;
import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.entity.Player;

/**
 * Action to open another GUI
 */
public class OpenGuiAction extends ActionHandler {
    private final String guiName;
    
    public OpenGuiAction(String guiName) {
        this.guiName = guiName;
    }
    
    @Override
    public void execute(Player player, DataContext context, Object clickInfo) {
        String resolvedGuiName = context.resolve(guiName);
        
        // Create new context with current data
        DataContext newContext = context.createChild();
        
        // Open the GUI
        PerPlayerKit.guiManager.openGui(player, resolvedGuiName, newContext);
    }
}