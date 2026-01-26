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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

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

        if (!e.getPlayer().hasPermission("perplayerkit.rekitonrespawn")) {
            return;
        }

        KitManager.get().loadLastKit(e.getPlayer());
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {

        if (!isRekitOnKillEnabled()) {
            return;
        }

        Player killer = e.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (!killer.hasPermission("perplayerkit.rekitonkill")) {
            return;
        }

        String killerWorld = killer.getWorld().getName();
        if (isWorldAllowedForRekitOnKill(killerWorld)) {
            KitManager.get().loadLastKit(killer);
        }
    }

    /**
     * Checks if rekit-on-kill is enabled, supporting both old boolean format and new section format.
     */
    private boolean isRekitOnKillEnabled() {
        // Check if it's a section (new format)
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("feature.rekit-on-kill");
        if (section != null) {
            return section.getBoolean("enabled", false);
        }
        // Fall back to old boolean format for backwards compatibility
        return plugin.getConfig().getBoolean("feature.rekit-on-kill", false);
    }

    /**
     * Checks if a world is allowed for rekit-on-kill based on whitelist/blacklist settings.
     */
    private boolean isWorldAllowedForRekitOnKill(String worldName) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("feature.rekit-on-kill");
        if (section == null) {
            // Old format - no world filtering, allow all
            return true;
        }

        List<String> whitelist = section.getStringList("world-whitelist");
        List<String> blacklist = section.getStringList("world-blacklist");

        // If whitelist is not empty, only allow worlds in the whitelist
        if (whitelist != null && !whitelist.isEmpty()) {
            return whitelist.contains(worldName);
        }

        // If blacklist is not empty, allow all worlds except those in the blacklist
        if (blacklist != null && !blacklist.isEmpty()) {
            return !blacklist.contains(worldName);
        }

        // Both empty - allow all worlds
        return true;
    }

}
