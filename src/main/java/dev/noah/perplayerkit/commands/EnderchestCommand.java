package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.DisabledCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

public class EnderchestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {

            if (DisabledCommand.isBlockedInWorld(player)) {
                return true;
            }
            viewOnlyEC(player);
            return true;
        }

        sender.sendMessage("Only players can use this command");
        return true;
    }

    public void viewOnlyEC(Player p) {
        ItemStack fill = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
//        set name to nothing
        fill.getItemMeta().setDisplayName("");

        Menu menu = ChestMenu.builder(5).title(ChatColor.BLUE + "View Only Enderchest").build();


        for (int i = 0; i < 9; i++) {
            menu.getSlot(i).setItem(fill);
        }
        for (int i = 36; i < 45; i++) {
            menu.getSlot(i).setItem(fill);
        }
//        set the items in the inventory to the items in the enderchest
        ItemStack[] items = p.getEnderChest().getContents();
        for (int i = 0; i < 27; i++) {
            menu.getSlot(i + 9).setItem(items[i]);
        }
        menu.open(p);
    }
}


