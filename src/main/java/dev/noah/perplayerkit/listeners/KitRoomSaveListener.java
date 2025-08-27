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
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.util.StyleManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class KitRoomSaveListener implements Listener {

    @EventHandler
    public void onSaveButtonClick(InventoryClickEvent e) {
        if (e.getClick().isShiftClick() && e.getClick().isRightClick()) {
            Inventory inv = e.getInventory();
            if (inv.getSize() == 54) {
                if (inv.getLocation() == null) {
                    InventoryView view = e.getView();
                    Player p = (Player) e.getWhoClicked();

                    if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + p.getName() + "'s Kits")) {
                        ItemStack saveButton = e.getInventory().getItem(53);
                        if (saveButton != null && saveButton.getType() == Material.BARRIER) {
                            if (e.getSlot() == 53) {
                                if (p.hasPermission("perplayerkit.editkitroom") || p.isOp()) {

                                    // Safely get the page number
                                    int page = (saveButton.getAmount() > 0) ? saveButton.getAmount() - 1 : 0;

                                    // Initialize kitroom array
                                    ItemStack[] kitroom = new ItemStack[45];

                                    for (int i = 0; i < 45; i++) {
                                        ItemStack item = e.getInventory().getItem(i);
                                        kitroom[i] = (item != null) ? item.clone() : null;
                                    }

                                    // Save kitroom data
                                    KitRoomDataManager.get().setKitRoom(page, kitroom);
                                    KitRoomDataManager.get().saveToDBAsync();
                                    p.sendMessage(ChatColor.GREEN + "Saved kitroom page: " + (page + 1));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
