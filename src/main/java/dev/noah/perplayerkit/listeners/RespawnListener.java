package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!e.getPlayer().hasPermission("kit.use")) {
            return;
        }

        KitManager.get().loadLastKit(e.getPlayer());

    }

}
