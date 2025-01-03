package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ShortKitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }

        UUID uuid = player.getUniqueId();

        // Check if the label matches "kX" or "kitX" where X is a number between 1 and 9
        if (label.matches("k[1-9]")) {
            int kitNumber = Integer.parseInt(label.substring(1)); // Extract the number for "kX"
            KitManager.get().loadKit(player, kitNumber);
        } else if (label.matches("kit[1-9]")) {
            int kitNumber = Integer.parseInt(label.substring(3)); // Extract the number for "kitX"
            KitManager.get().loadKit(player, kitNumber);
        } else {
            player.sendMessage("Invalid command label.");
        }

        return true;
    }
}
