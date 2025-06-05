/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import dev.noah.perplayerkit.ConfigManager;

public class AutoRekitListener implements Listener {

    private final Plugin plugin;

    public AutoRekitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {

        if (!ConfigManager.get().isRekitOnRespawnEnabled()) {
            return;
        }

        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            KitManager.get().loadLastKit(e.getPlayer());
        }, 3L);
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {

        if (!ConfigManager.get().isRekitOnKillEnabled()) {
            return;
        }

        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            if (killer != victim) {
                // Delay kit loading to allow for proper inventory management
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    KitManager.get().loadLastKit(killer);
                }, 3L);
            }
        }
    }

}
