package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.DisabledCommand;
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

        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (DisabledCommand.isBlockedInWorld(p)) {
                return true;
            }

            UUID uuid = p.getUniqueId();


            if (label.equalsIgnoreCase("k1") || label.equalsIgnoreCase("kit1"))
                KitManager.loadkit(uuid, 1);
            if (label.equalsIgnoreCase("k2") || label.equalsIgnoreCase("kit2"))
                KitManager.loadkit(uuid, 2);
            if (label.equalsIgnoreCase("k3") || label.equalsIgnoreCase("kit3"))
                KitManager.loadkit(uuid, 3);
            if (label.equalsIgnoreCase("k4") || label.equalsIgnoreCase("kit4"))
                KitManager.loadkit(uuid, 4);
            if (label.equalsIgnoreCase("k5") || label.equalsIgnoreCase("kit5"))
                KitManager.loadkit(uuid, 5);
            if (label.equalsIgnoreCase("k6") || label.equalsIgnoreCase("kit6"))
                KitManager.loadkit(uuid, 6);
            if (label.equalsIgnoreCase("k7") || label.equalsIgnoreCase("kit7"))
                KitManager.loadkit(uuid, 7);
            if (label.equalsIgnoreCase("k8") || label.equalsIgnoreCase("kit8"))
                KitManager.loadkit(uuid, 8);
            if (label.equalsIgnoreCase("k9") || label.equalsIgnoreCase("kit9"))
                KitManager.loadkit(uuid, 9);
        } else {
            sender.sendMessage("Only players can use this command");
        }

        return true;
    }
}