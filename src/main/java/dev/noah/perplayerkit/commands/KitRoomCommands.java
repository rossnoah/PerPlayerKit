package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitRoomDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class KitRoomCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("load")) {
                KitRoomDataManager.get().loadFromDB();
                sender.sendMessage(ChatColor.GREEN + "Kit Room loaded from SQL");
            } else if (args[0].equalsIgnoreCase("save")) {
                KitRoomDataManager.get().saveToDBAsync();
                sender.sendMessage(ChatColor.GREEN + "Kit Room saved to SQL");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Incorrect Usage!");
                sender.sendMessage("/kitroom <load/save>");
            }
        } else {
            sender.sendMessage(ChatColor.GREEN + "Incorrect Usage!");
            sender.sendMessage("/kitroom <load/save>");
        }


        return true;
    }
}
