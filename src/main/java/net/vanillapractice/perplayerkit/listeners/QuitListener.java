package net.vanillapractice.perplayerkit.listeners;

import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import net.vanillapractice.perplayerkit.Serializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;


public class QuitListener implements Listener {



    private final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

      //  KitManager.saveToSQL(uuid);

        new BukkitRunnable() {

            @Override
            public void run() {
                KitManager.saveToSQL(uuid);
            }

        }.runTaskAsynchronously(plugin);


    }



}
