package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.DisabledCommand;
import dev.noah.perplayerkit.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ShortECCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (label.matches("ec[1-9]")) {
            int ecNumber = Integer.parseInt(label.substring(2)); // Extract the number from the label
            KitManager.get().loadEC(uuid, ecNumber);
        } else {
            player.sendMessage("Invalid command label.");
        }

        return true;
    }
}
