package net.vanillapractice.perplayerkit;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Broadcast {

    public static void bcRepair(Player player) {
        World w = player.getWorld();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) < PerPlayerKit.bcDistance) {
                p.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes
                        ('&', "&3" + player.getName() + "&7 repaired!"));
            }
        }

    }


    public static void bcKit(Player player) {
        World w = player.getWorld();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) < PerPlayerKit.bcDistance) {
                p.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes
                        ('&', "&3" + player.getName() + "&7 loaded a kit!"));
            }
        }


    }

    public static void bcPublicKit(Player player) {
        World w = player.getWorld();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) < PerPlayerKit.bcDistance) {
                p.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes
                        ('&', "&3" + player.getName() + "&7 loaded a public kit!"));
            }
        }


    }


    public static void bcEC(Player player) {
        World w = player.getWorld();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) < PerPlayerKit.bcDistance) {
                p.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes
                        ('&', "&3" + player.getName() + "&7 loaded their enderchest!"));
            }
        }


    }


    public static void bcKitCopy(Player player) {
        World w = player.getWorld();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) < PerPlayerKit.bcDistance) {
                p.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes
                        ('&', "&3" + player.getName() + "&7 copied a kit!"));
            }
        }


    }


    public static void bcKitRoom(Player player) {
        if (!Cooldown.isOnKitroomCooldown(player.getUniqueId())) {
            Cooldown.updateKitroomCooldown(player.getUniqueId());
            World w = player.getWorld();
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distance(player.getLocation()) < PerPlayerKit.bcDistance) {
                    p.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes
                            ('&', "&3" + player.getName() + "&7 opened the Kit Room!"));
                }
            }


        }
    }
}
