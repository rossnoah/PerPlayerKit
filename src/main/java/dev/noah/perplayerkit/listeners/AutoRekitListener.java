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
import dev.noah.perplayerkit.util.LocationAccess;
import dev.noah.perplayerkit.util.LocationFeature;
import dev.noah.perplayerkit.util.RekitKitResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoRekitListener implements Listener {
    private final Plugin plugin;
    private final Map<UUID, BukkitTask> pendingRespawns = new HashMap<>();
    private final Set<String> warnedMissingKits = new HashSet<>();
    public AutoRekitListener(Plugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("rekit.respawn.enabled", true)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("perplayerkit.rekitonrespawn")) return;
        long delay = Math.max(1, plugin.getConfig().getLong("rekit.respawn.delay-ticks", 0));
        // Check the actual destination after respawning, including later teleports during the delay.
        cancelRespawn(player);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingRespawns.remove(player.getUniqueId());
            if (plugin.getServer().getPlayer(player.getUniqueId()) == player)
                restore(player, "rekit.respawn", "perplayerkit.rekitonrespawn", LocationFeature.REKIT_RESPAWN);
        }, delay);
        pendingRespawns.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { cancelRespawn(event.getPlayer()); }

    private void cancelRespawn(Player player) {
        BukkitTask task = pendingRespawns.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("rekit.kill.enabled", false)) return;
        Player killer = event.getEntity().getKiller();
        if (killer != null) restore(killer, "rekit.kill", "perplayerkit.rekitonkill", LocationFeature.REKIT_KILL);
    }

    private void restore(Player player, String path, String permission, LocationFeature feature) {
        if (!player.isOnline() || player.isDead() || !player.hasPermission(permission)
                || KitManager.get().isLoading(player.getUniqueId()) || !LocationAccess.get().check(player, feature).allowed()) return;
        ConfigurationSection mappings = plugin.getConfig().getConfigurationSection(path + ".kits");
        List<String> regions = List.of();
        if (RekitKitResolver.hasRegionEntries(mappings, player.getWorld().getName())) {
            try { regions = LocationAccess.get().regions(player); }
            catch (LocationAccess.RegionUnavailableException e) {
                LocationAccess.get().warn(path + ".kits", player, e);
                return;
            }
        }
        String configured = RekitKitResolver.resolveKit(mappings, player.getWorld().getName(), regions);
        if (configured == null || !giveConfiguredPublicKit(player, configured, path)) {
            KitManager.KitReference ref = KitManager.get().getLastKitReference(player.getUniqueId());
            if (ref != null && player.hasPermission(ref.publicId() == null ? "perplayerkit.kit" : "perplayerkit.publickit"))
                KitManager.get().loadLastKit(player);
        }
        if (plugin.getConfig().getBoolean(path + ".restore-enderchest", false)
                && player.hasPermission("perplayerkit.enderchest")) KitManager.get().restoreLastEnderchest(player);
    }

    private boolean giveConfiguredPublicKit(Player player, String configured, String path) {
        if (!player.hasPermission("perplayerkit.publickit")) return false;
        String id = KitManager.get().getPublicKitList().stream().map(kit -> kit.id)
                .filter(key -> key.equalsIgnoreCase(configured)).findFirst().orElse(configured);
        if (KitManager.get().hasPublicKit(id)) return KitManager.get().loadPublicKitSilent(player, id);
        if (warnedMissingKits.add(path + ":" + configured)) plugin.getLogger().warning(path
                + " is configured to give public kit '" + configured + "' but that kit has no saved contents. "
                + "Using the player's last loaded kit instead.");
        return false;
    }
}
