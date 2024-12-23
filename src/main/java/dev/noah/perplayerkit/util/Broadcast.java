package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Broadcast {


    private final int broadcastDistance = 500;
    CooldownManager repairBroadcastCooldown = new CooldownManager(5);
    CooldownManager kitroomBroadcastCooldown = new CooldownManager(15);

    private static Broadcast instance;

    public static Broadcast get() {
        if (instance == null) {
            instance = new Broadcast();
        }
        return instance;
    }

    private void broadcastMessage(Player player, String message) {
        World world = player.getWorld();

        for(Player broadcastPlayer : world.getPlayers()){
            if(broadcastPlayer.getLocation().distance(player.getLocation()) < broadcastDistance){
                broadcastPlayer.sendMessage(PerPlayerKit.prefix + ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    private void broadcastMessage(Player player, String message, CooldownManager cooldownManager) {
        if(cooldownManager.isOnCooldown(player)){
            return;
        }
        broadcastMessage(player, message);
        cooldownManager.setCooldown(player);
    }

    public void broadcastPlayerRepaired(Player player) {
        broadcastMessage(player, "&3" + player.getName() + "&7 repaired!", repairBroadcastCooldown);
    }

    public void broadcastPlayerOpenedKitRoom(Player player) {
        broadcastMessage(player, "&3" + player.getName() + "&7 opened the Kit Room!", kitroomBroadcastCooldown);
    }

    public void broadcastPlayerLoadedPrivateKit(Player player) {
        broadcastMessage(player, "&3" + player.getName() + "&7 loaded a kit!");
    }

    public void broadcastPlayerLoadedPublicKit(Player player) {
        broadcastMessage(player, "&3" + player.getName() + "&7 loaded a public kit!");
    }

    public void broadcastPlayerLoadedEnderChest(Player player) {
        broadcastMessage(player, "&3" + player.getName() + "&7 loaded their enderchest!");
    }

    public void broadcastPlayerCopiedKit(Player player) {
        broadcastMessage(player, "&3" + player.getName() + "&7 copied a kit!");
    }


}
