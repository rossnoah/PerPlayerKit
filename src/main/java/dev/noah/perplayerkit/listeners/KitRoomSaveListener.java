package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitRoomDataManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class KitRoomSaveListener implements Listener {

    @EventHandler
    public void onSaveButtonClick(InventoryClickEvent e) {
        if (e.getClick().isShiftClick() && e.getClick().isRightClick()) {
            Inventory inv = e.getInventory();
            if (inv.getSize() == 54) {
                if (inv.getLocation() == null) {
                    InventoryView view = e.getView();
                    Player p = (Player) e.getWhoClicked();

                    if (view.getTitle().contains(ChatColor.BLUE + p.getName() + "'s Kits")) {
                        if (e.getInventory().getItem(53) != null) {
                            if (e.getInventory().getItem(53).getType() == Material.BARRIER) {
                                if (e.getSlot() == 53) {
                                    if (p.hasPermission("perplayerkit.editkitroom") || p.isOp()) {

                                        int page = e.getInventory().getItem(53).getAmount() - 1;
                                        UUID uuid = p.getUniqueId();
                                        ItemStack[] kitroom = new ItemStack[45];

                                        for (int i = 0; i < 45; i++) {
                                            if (e.getInventory().getItem(i) != null) {
                                                kitroom[i] = e.getInventory().getItem(i).clone();
                                            } else {
                                                kitroom[i] = null;
                                            }

                                        }
                                        KitRoomDataManager.setKitRoom(page, kitroom);
                                        KitRoomDataManager.saveToSQL();
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
}
