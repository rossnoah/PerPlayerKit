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
package dev.noah.perplayerkit.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The only class allowed to reference WorldGuard types. WorldGuard is a
 * soft dependency, so callers must confirm the plugin is installed before
 * invoking anything here — otherwise class loading fails at runtime.
 */
public final class WorldGuardSupport {

    private WorldGuardSupport() {
    }

    /**
     * Ids of the WorldGuard regions at the player's location, including parents, ordered highest
     * region priority first (ties broken alphabetically for determinism).
     */
    public static List<String> getRegionIdsByPriority(Player player) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        List<ProtectedRegion> regions = new ArrayList<>();
        // The default query includes inherited parent regions, preserving existing rekit mappings.
        var applicable = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
        if (applicable.isVirtual()) throw new IllegalStateException("Region data is unavailable or protection is disabled");
        applicable.forEach(regions::add);
        regions.sort(Comparator.comparingInt(ProtectedRegion::getPriority).reversed()
                .thenComparing(ProtectedRegion::getId));
        List<String> ids = new ArrayList<>(regions.size());
        for (ProtectedRegion region : regions) {
            ids.add(region.getId());
        }
        return ids;
    }
}
