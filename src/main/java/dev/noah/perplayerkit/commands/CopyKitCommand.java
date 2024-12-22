package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.DisabledCommand;
import dev.noah.perplayerkit.kitsharing.KitShareManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CopyKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (DisabledCommand.isBlockedInWorld(p)) {
                return true;
            }


            if (args.length > 0) {
                KitShareManager.copyKit(p, args[0]);
            } else {
                p.sendMessage(ChatColor.RED + "Error, you must select a kit to copy");
            }
        } else {
            sender.sendMessage("Only players can use this command");
        }

        return true;
    }
}