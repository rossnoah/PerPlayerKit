package dev.noah.perplayerkit;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class DisabledCommand {


    private static boolean isBlockedInWorld(World world) {
        return PerPlayerKit.getPlugin().getConfig().getStringList("disabled-command-worlds").contains(world.getName());
    }


    public static boolean isBlockedInWorld(Player player) {
        if (isBlockedInWorld(player.getWorld())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', PerPlayerKit.getPlugin().getConfig().getString("disabled-command-message")));
            return true;
        }
        return false;
    }
}
