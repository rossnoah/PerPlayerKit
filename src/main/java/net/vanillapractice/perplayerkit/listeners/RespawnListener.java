package net.vanillapractice.perplayerkit.listeners;

import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!e.getPlayer().hasPermission("kit.use")) {
            return;
        }

        if (PerPlayerKit.lastKit.containsKey(e.getPlayer().getUniqueId())) {
            KitManager.respawnKitLoad(e.getPlayer().getUniqueId(), PerPlayerKit.lastKit.get(e.getPlayer().getUniqueId()));
        }

    }

}
