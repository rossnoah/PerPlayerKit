package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;


public class QuitListener implements Listener {


    private Plugin plugin;
    public QuitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {

            @Override
            public void run() {
                KitManager.get().savePlayerKitsToDB(uuid);
            }

        }.runTaskAsynchronously(plugin);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            KitManager.get().savePlayerKitsToDB(uuid);
        });


    }


}
