package net.vanillapractice.perplayerkit.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class JoinListener implements Listener {

    private final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        //  KitManager.loadFromSQL(uuid);

        new BukkitRunnable() {

            @Override
            public void run() {
                KitManager.loadFromSQL(uuid);
            }

        }.runTaskAsynchronously(plugin);


        // Check if MOTD is enabled and send MOTD messages
        if (plugin.getConfig().getBoolean("motd.enabled")) {
            List<Component> motdMessages = new ArrayList<>();
            plugin.getConfig().getStringList("motd.message").forEach(message -> motdMessages.add(MiniMessage.miniMessage().deserialize(message)));

            // Delay for sending the MOTD
            Bukkit.getScheduler().runTaskLater(plugin, () -> motdMessages.forEach(player::sendMessage), plugin.getConfig().getLong("motd.delay") * 20L);

        }
    }


}


