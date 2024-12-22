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

        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (DisabledCommand.isBlockedInWorld(p)) {
                return true;
            }

            UUID uuid = p.getUniqueId();


            if (label.equalsIgnoreCase("ec1"))
                KitManager.loadEC(uuid, 1);
            if (label.equalsIgnoreCase("ec2"))
                KitManager.loadEC(uuid, 2);
            if (label.equalsIgnoreCase("ec3"))
                KitManager.loadEC(uuid, 3);
            if (label.equalsIgnoreCase("ec4"))
                KitManager.loadEC(uuid, 4);
            if (label.equalsIgnoreCase("ec5"))
                KitManager.loadEC(uuid, 5);
            if (label.equalsIgnoreCase("ec6"))
                KitManager.loadEC(uuid, 6);
            if (label.equalsIgnoreCase("ec7"))
                KitManager.loadEC(uuid, 7);
            if (label.equalsIgnoreCase("ec8"))
                KitManager.loadEC(uuid, 8);
            if (label.equalsIgnoreCase("ec9"))
                KitManager.loadEC(uuid, 9);
        } else {
            sender.sendMessage("Only players can use this command");
        }

        return true;
    }
}