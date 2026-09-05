/*
 * Copyright 2022-2026 Noah Ross
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
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.gui.EditorSaver;
import dev.noah.perplayerkit.gui.GUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.ipvp.canvas.type.MenuHolder;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class KitMenuCloseListener implements Listener {

    // Canvas handles clicks at HIGH and ignores cancelled events. Check every interaction,
    // including shift-click, number-key swaps, drags, and taking items from the kit room.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder holder
                && event.getWhoClicked() instanceof Player player && !GUI.canUseMenu(player, holder.getMenu()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder holder
                && event.getWhoClicked() instanceof Player player && !GUI.canUseMenu(player, holder.getMenu()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() != 54 || inv.getLocation() != null) {
            return;
        }

        Player player = (Player) e.getPlayer();
        GUI.EditorContext context = GUI.getAndRemoveEditorContext(player.getUniqueId());
        if (context == null) {
            return;
        }

        EditorSaver.save(player, context, inv);
    }
}
