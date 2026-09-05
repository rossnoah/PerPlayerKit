/*
 * Copyright 2022-2026 Noah Ross
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
import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.RekitKitResolver;
import dev.noah.perplayerkit.util.WorldGuardSupport;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoRekitListener implements Listener {

    private final Plugin plugin;
    private final boolean worldGuardInstalled;
    private final Set<String> warnedMissingKits = new HashSet<>();

    public AutoRekitListener(Plugin plugin) {
        this.plugin = plugin;
        this.worldGuardInstalled = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        if (!worldGuardInstalled && RekitKitResolver.hasRegionEntries(getRekitKitsSection())) {
            plugin.getLogger().warning("rekit.kill.kits contains region entries (\"world:region\") "
                    + "but WorldGuard is not installed, so those entries will be ignored.");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {

        if (!plugin.getConfig().getBoolean("rekit.respawn.enabled", true)) {
            return;
        }

        if (!e.getPlayer().hasPermission("perplayerkit.rekitonrespawn")) {
            return;
        }

        long delay = plugin.getConfig().getLong("rekit.respawn.delay-ticks", 0);
        Player player = e.getPlayer();

        new BukkitRunnable() {
            @Override public void run() {
                if (player.isOnline() && player.hasPermission("perplayerkit.rekitonrespawn")
                        && !DisabledCommand.isBlockedInWorld(player.getWorld())) restore(player, "rekit.respawn");
            }
        }.runTaskLater(plugin, Math.max(1, delay));
    }

    private void restore(Player player, String path) {
        if (KitManager.get().isLoading(player.getUniqueId())) return;
        KitManager.KitReference ref = KitManager.get().getLastKitReference(player.getUniqueId());
        if (ref != null && player.hasPermission(ref.publicId() == null ? "perplayerkit.kit" : "perplayerkit.publickit"))
            KitManager.get().loadLastKit(player);
        restoreEnderchest(player, path);
    }

    private void restoreEnderchest(Player player, String path) {
        if (plugin.getConfig().getBoolean(path + ".restore-enderchest", false)
                && player.hasPermission("perplayerkit.enderchest")) KitManager.get().restoreLastEnderchest(player);
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {

        if (!isRekitOnKillEnabled()) {
            return;
        }

        Player killer = e.getEntity().getKiller();
        if (killer == null || killer.isDead() || DisabledCommand.isBlockedInWorld(killer.getWorld())) {
            return;
        }

        if (!killer.hasPermission("perplayerkit.rekitonkill")) {
            return;
        }

        String killerWorld = killer.getWorld().getName();
        if (!isWorldAllowedForRekitOnKill(killerWorld)) {
            return;
        }

        String configuredKit = resolveConfiguredPublicKit(killer);
        if (configuredKit == null || !giveConfiguredPublicKit(killer, configuredKit)) {
            restore(killer, "rekit.kill");
        } else {
            restoreEnderchest(killer, "rekit.kill");
        }
    }

    private ConfigurationSection getRekitKitsSection() {
        return plugin.getConfig().getConfigurationSection("rekit.kill.kits");
    }

    /**
     * Finds the public kit configured for the killer's world (and WorldGuard
     * region, if WorldGuard is installed), or null to keep the default
     * last-used-kit behavior.
     */
    private String resolveConfiguredPublicKit(Player killer) {
        ConfigurationSection kits = getRekitKitsSection();
        if (kits == null) {
            return null;
        }
        List<String> regionIds = Collections.emptyList();
        if (worldGuardInstalled && RekitKitResolver.hasRegionEntries(kits)) {
            try {
                regionIds = WorldGuardSupport.getRegionIdsByPriority(killer);
            } catch (Throwable t) { // an incompatible WorldGuard build can fail linkage at runtime
                plugin.getLogger().warning("Failed to query WorldGuard regions for rekit-on-kill: " + t);
            }
        }
        return RekitKitResolver.resolveKit(kits, killer.getWorld().getName(), regionIds);
    }

    private boolean giveConfiguredPublicKit(Player killer, String configuredKit) {
        String kitId = KitManager.get().getPublicKitList().stream()
                .map(k -> k.id)
                .filter(id -> id.equalsIgnoreCase(configuredKit))
                .findFirst()
                .orElse(configuredKit);
        if (!killer.hasPermission("perplayerkit.publickit")) return false;
        if (KitManager.get().hasPublicKit(kitId) && KitManager.get().loadPublicKitSilent(killer, kitId)) {
            return true;
        }
        if (warnedMissingKits.add(configuredKit)) {
            plugin.getLogger().warning("rekit-on-kill is configured to give public kit \"" + configuredKit
                    + "\" but that kit does not exist or has no saved contents. "
                    + "Giving the killer their last used kit instead.");
        }
        return false;
    }

    /**
     * Checks if rekit-on-kill is enabled, supporting both old boolean format and new section format.
     */
    private boolean isRekitOnKillEnabled() {
        return plugin.getConfig().getBoolean("rekit.kill.enabled", false);
    }

    /**
     * Checks if a world is allowed for rekit-on-kill based on whitelist/blacklist settings.
     */
    private boolean isWorldAllowedForRekitOnKill(String worldName) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rekit.kill");
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
