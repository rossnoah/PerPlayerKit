package net.vanillapractice.perplayerkit.listeners;

import jdk.tools.jmod.Main;
import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import net.vanillapractice.perplayerkit.Serializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.UUID;



public class JoinListener implements Listener {

    private final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

      //  KitManager.loadFromSQL(uuid);

        new BukkitRunnable() {

            @Override
            public void run() {
                KitManager.loadFromSQL(uuid);
            }

        }.runTaskAsynchronously(plugin);


    }

}
