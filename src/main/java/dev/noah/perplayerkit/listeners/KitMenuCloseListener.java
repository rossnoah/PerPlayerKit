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

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.util.StyleManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class KitMenuCloseListener implements Listener {

    @EventHandler
    public void onKitEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54) {
            if (inv.getLocation() == null) {
                InventoryView view = e.getView();
                if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + "Kit: ")) {
                    Player p = (Player) e.getPlayer();
                    UUID uuid = p.getUniqueId();
                    int slot = Integer.parseInt(view.getTitle().replace(StyleManager.get().getPrimaryColor() + "Kit: ", ""));
                    ItemStack[] kit = new ItemStack[41];
                    ItemStack[] chestitems = e.getInventory().getContents();

                    for (int i = 0; i < 41; i++) {
                        if (chestitems[i] == null) {
                            kit[i] = null;
                        } else {
                            kit[i] = chestitems[i].clone();
                        }
                    }
                    KitManager.get().savekit(uuid, slot, kit);
                }
            }
        }
    }

    @EventHandler
    public void onPublicKitEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54) {
            if (inv.getLocation() == null) {
                InventoryView view = e.getView();
                if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + "Public Kit: ")) {
                    Player player = (Player) e.getPlayer();
                    String publickit = view.getTitle().replace(StyleManager.get().getPrimaryColor() + "Public Kit: ", "");
                    ItemStack[] kit = new ItemStack[41];
                    ItemStack[] chestitems = e.getInventory().getContents();

                    for (int i = 0; i < 41; i++) {
                        if (chestitems[i] == null) {
                            kit[i] = null;
                        } else {
                            kit[i] = chestitems[i].clone();
                        }
                    }
                    KitManager.get().savePublicKit(player, publickit, kit);
                }
            }
        }
    }

    @EventHandler
    public void onEnderchestEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54) {
            if (inv.getLocation() == null) {
                InventoryView view = e.getView();
                if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + "Enderchest: ")) {
                    Player p = (Player) e.getPlayer();
                    UUID uuid = p.getUniqueId();
                    int slot = Integer.parseInt(view.getTitle().replace(StyleManager.get().getPrimaryColor() + "Enderchest: ", ""));
                    ItemStack[] kit = new ItemStack[27];
                    ItemStack[] chestitems = e.getInventory().getContents();

                    for (int i = 0; i < 27; i++) {
                        if (chestitems[i + 9] == null) {
                            kit[i] = null;
                        } else {
                            kit[i] = chestitems[i + 9].clone();
                        }
                    }
                    KitManager.get().saveEC(uuid, slot, kit);
                }
            }
        }
    }

    @EventHandler
    public void onInspectKitEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54) {
            if (inv.getLocation() == null) {
                InventoryView view = e.getView();
                if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + "Inspecting ") && view.getTitle().contains("'s kit ")) {
                    Player p = (Player) e.getPlayer();
                    if (!p.hasPermission("perplayerkit.admin")) {
                        return;
                    }

                    UUID targetUuid = GUI.getAndRemoveInspectTarget(p.getUniqueId());
                    if (targetUuid == null) {
                        return;
                    }

                    String title = view.getTitle();
                    String[] parts = title.replace(StyleManager.get().getPrimaryColor() + "Inspecting ", "").split("'s kit ");
                    if (parts.length != 2) {
                        return;
                    }
                    String playerName = parts[0];
                    int slot;
                    try {
                        slot = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ex) {
                        return;
                    }

                    if (GUI.removeKitDeletionFlag(p)) {
                        return;
                    }

                    ItemStack[] kit = new ItemStack[41];
                    ItemStack[] chestitems = e.getInventory().getContents();

                    for (int i = 0; i < 41; i++) {
                        if (chestitems[i] == null) {
                            kit[i] = null;
                        } else {
                            kit[i] = chestitems[i].clone();
                        }
                    }

                    if (KitManager.get().savekit(targetUuid, slot, kit, true)) {
                        p.sendMessage(ChatColor.GREEN + "Kit " + slot + " updated for player " + playerName + "!");
                    } else {
                        p.sendMessage(ChatColor.RED + "Failed to update kit for player " + playerName + "!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInspectEnderchestEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54) {
            if (inv.getLocation() == null) {
                InventoryView view = e.getView();
                if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + "Inspecting ") && view.getTitle().contains("'s enderchest ")) {
                    Player p = (Player) e.getPlayer();
                    if (!p.hasPermission("perplayerkit.admin")) {
                        return;
                    }

                    UUID targetUuid = GUI.getAndRemoveInspectTarget(p.getUniqueId());
                    if (targetUuid == null) {
                        return;
                    }

                    String title = view.getTitle();
                    String[] parts = title.replace(StyleManager.get().getPrimaryColor() + "Inspecting ", "").split("'s enderchest ");
                    if (parts.length != 2) {
                        return;
                    }
                    String playerName = parts[0];
                    int slot;
                    try {
                        slot = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ex) {
                        return;
                    }

                    if (GUI.removeKitDeletionFlag(p)) {
                        return;
                    }

                    ItemStack[] kit = new ItemStack[27];
                    ItemStack[] chestitems = e.getInventory().getContents();

                    for (int i = 0; i < 27; i++) {
                        if (chestitems[i + 9] == null) {
                            kit[i] = null;
                        } else {
                            kit[i] = chestitems[i + 9].clone();
                        }
                    }

                    if (KitManager.get().saveECSilent(targetUuid, slot, kit)) {
                        p.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " updated for player " + playerName + "!");
                    } else {
                        p.sendMessage(ChatColor.RED + "Failed to update enderchest for player " + playerName + "!");
                    }
                }
            }
        }
    }
}