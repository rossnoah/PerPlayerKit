package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;


public class QuitListener implements Listener {


    private final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        //  KitManager.saveToSQL(uuid);

        new BukkitRunnable() {

            @Override
            public void run() {
                KitManager.get().savePlayerKitsToDB(uuid);
            }

        }.runTaskAsynchronously(plugin);


    }


}
