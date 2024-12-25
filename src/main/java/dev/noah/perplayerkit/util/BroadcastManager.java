package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BroadcastManager {


    private final int broadcastDistance = 500;
    private CooldownManager repairBroadcastCooldown = new CooldownManager(5);
    private CooldownManager kitroomBroadcastCooldown = new CooldownManager(15);
    private BukkitAudiences audience;

    private static BroadcastManager instance;
    private final Plugin plugin;

    public BroadcastManager(Plugin plugin) {
        this.plugin = plugin;
        audience = BukkitAudiences.create(plugin);
        instance = this;
    }

    public static BroadcastManager get() {
        if (instance == null) {
            throw new IllegalStateException("Broadcast has not been initialized yet!");
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

    public void startScheduledBroadcast() {

        List<Component> messages = new ArrayList<>();
        plugin.getConfig().getStringList("scheduled-broadcast.messages").forEach(message -> messages.add(MiniMessage.miniMessage().deserialize(message)));

        if (plugin.getConfig().getBoolean("scheduled-broadcast.enabled")) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Component message : messages) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        audience.player(player).sendMessage(message);
                    }
                }
            }, 0, plugin.getConfig().getInt("scheduled-broadcast.period") * 20L);
        }

    }

    public void sendComponentMessage(Player player, Component message) {
        audience.player(player).sendMessage(message);
    }

}
