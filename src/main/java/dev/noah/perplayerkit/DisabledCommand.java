package dev.noah.perplayerkit;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DisabledCommand {

    public static boolean isBlockedInWorld(Player player) {
        if (PerPlayerKit.getPlugin().getConfig().getStringList("disabled-command-worlds").contains(player.getWorld().getName())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', PerPlayerKit.getPlugin().getConfig().getString("disabled-command-message")));
            return true;
        }
        return false;
    }
}
