package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.KitShareManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CopyKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {

            if (DisabledCommand.isBlockedInWorld(player)) {
                return true;
            }


            if (args.length > 0) {
                KitShareManager.get().copyKit(player, args[0]);
            } else {
                player.sendMessage(ChatColor.RED + "Error, you must select a kit to copy");
            }
        } else {
            sender.sendMessage("Only players can use this command");
        }

        return true;
    }
}