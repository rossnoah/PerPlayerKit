package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class JoinListener implements Listener {

    private final Plugin plugin;


    public JoinListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        //  KitManager.loadFromSQL(uuid);

        new BukkitRunnable() {

            @Override
            public void run() {
                KitManager.get().loadPlayerDataFromDB(uuid);
            }

        }.runTaskAsynchronously(plugin);


        // Check if MOTD is enabled and send MOTD messages
        if (plugin.getConfig().getBoolean("motd.enabled")) {
            List<Component> motdMessages = new ArrayList<>();
            plugin.getConfig().getStringList("motd.message").forEach(message -> motdMessages.add(MiniMessage.miniMessage().deserialize(message)));

            // Delay for sending the MOTD
            Bukkit.getScheduler().runTaskLater(plugin, () -> motdMessages.forEach(message -> BroadcastManager.get().sendComponentMessage(player,message)), plugin.getConfig().getLong("motd.delay") * 20L);
        }
    }


}


