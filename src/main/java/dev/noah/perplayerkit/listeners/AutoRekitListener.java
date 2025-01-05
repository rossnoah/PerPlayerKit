package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class AutoRekitListener implements Listener {

    private final Plugin plugin;

    public AutoRekitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {


        if (!plugin.getConfig().getBoolean("feature.rekit-on-respawn", true)) {
            return;
        }

        if (!e.getPlayer().hasPermission("kit.use")) {
            return;
        }

        KitManager.get().loadLastKit(e.getPlayer());

    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {


        if (!plugin.getConfig().getBoolean("feature.rekit-on-kill", false)) {
            return;
        }

        Player killer = e.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (!killer.hasPermission("kit.use")) {
            return;
        }

        KitManager.get().loadLastKit(killer);

    }

}
