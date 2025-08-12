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
package dev.noah.perplayerkit.gui2.actions.actions;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui2.actions.ActionHandler;
import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.entity.Player;

/**
 * Action to load a player's kit from a specific slot
 */
public class LoadKitAction extends ActionHandler {
    private final int slot;
    
    public LoadKitAction(int slot) {
        this.slot = slot;
    }
    
    @Override
    public void execute(Player player, DataContext context, Object clickInfo) {
        // Resolve slot from context if needed
        int actualSlot = slot;
        if (slot == -1) {
            actualSlot = context.getInt("slot_number", 1);
        }
        
        boolean success = KitManager.get().loadKit(player, actualSlot);
        
        if (success) {
            context.set("last_action", "load_kit");
            context.set("last_slot", actualSlot);
        }
    }
}