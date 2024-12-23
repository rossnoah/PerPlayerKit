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
