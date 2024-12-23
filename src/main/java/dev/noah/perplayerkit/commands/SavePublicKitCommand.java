package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.DisabledCommand;
import dev.noah.perplayerkit.Filter;
import dev.noah.perplayerkit.KitManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SavePublicKitCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //if not player
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (DisabledCommand.isBlockedInWorld(p)) {
            return true;
        }

        //if not enough arguments

        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "You need to specify a kit id");
            p.sendMessage(ChatColor.RED + "Usage: /" + label + " <kitid>");
            return true;
        }

        String kidId = args[0];

        if (KitManager.get().getPublicKitList().stream().noneMatch(kit -> kit.id.equals(kidId))) {
            p.sendMessage(ChatColor.RED + "Public kit " + kidId + " does not exist");
            p.sendMessage(ChatColor.RED + "You may need to add a public kit in the config");
            return true;
        }

        Inventory inv = p.getInventory();

        ItemStack[] data = new ItemStack[41];
//        copy inventory into data
        for (int i = 0; i < 41; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                data[i] = item.clone();
            }
        }


        data = Filter.filterItemStack(data);

        KitManager kitManager = KitManager.get();
        //save kit
        boolean success = kitManager.savePublicKit(kidId, data);
        if (success) {
            kitManager.savePublicKitToDB(kidId);
            p.sendMessage("Saved kit " + kidId);
        } else {
            p.sendMessage("Error saving kit " + kidId);

        }

        return true;


    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return KitManager.get().getPublicKitList().stream().map(kit -> kit.id).toList();
    }
}
