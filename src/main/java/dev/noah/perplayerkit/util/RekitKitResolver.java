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

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Resolves which public kit, if any, is configured for a location under
 * {@code rekit.respawn.kits} or {@code rekit.kill.kits}. Entry keys are {@code "<world>"} or
 * {@code "<world>:<region>"} (region entries require WorldGuard); values are
 * either a plain kit id or a section containing a {@code kit} key.
 */
public final class RekitKitResolver {

    private RekitKitResolver() {
    }

    public static void validate(ConfigurationSection config) {
        for (String path : List.of("rekit.respawn.kits", "rekit.kill.kits")) {
            if (!config.contains(path)) continue;
            ConfigurationSection kits = config.getConfigurationSection(path);
            if (kits == null) throw new IllegalArgumentException(path + " must map locations to public kit IDs");
            java.util.Set<LocationRules.Selector> seen = new java.util.HashSet<>();
            for (String key : kits.getKeys(false)) {
                try {
                    if (!seen.add(LocationRules.Selector.parse(key))) throw new IllegalArgumentException("Duplicate location '" + key + "'");
                    if (readKitName(kits, key) == null) throw new IllegalArgumentException("Missing public kit ID for '" + key + "'");
                } catch (IllegalArgumentException e) { throw new IllegalArgumentException(path + ": " + e.getMessage()); }
            }
        }
    }

    public static boolean hasRegionEntries(ConfigurationSection kits, String world) {
        return kits != null && kits.getKeys(false).stream().anyMatch(key -> {
            LocationRules.Selector entry = LocationRules.Selector.parse(key);
            return entry.region() != null && entry.inWorld(world);
        });
    }

    public static boolean hasRegionEntries(ConfigurationSection kitsSection) {
        if (kitsSection == null) {
            return false;
        }
        return kitsSection.getKeys(false).stream().anyMatch(key -> key.indexOf(':') >= 0);
    }

    /**
     * Region entries win over world-wide entries. {@code regionIdsByPriority}
     * must be ordered highest WorldGuard priority first so the first matching
     * region entry wins; pass an empty list to skip region entries entirely.
     *
     * @return the configured kit id, or null when no entry matches
     */
    public static String resolveKit(ConfigurationSection kitsSection, String worldName, List<String> regionIdsByPriority) {
        if (kitsSection == null || worldName == null) {
            return null;
        }
        if (regionIdsByPriority != null) {
            for (String regionId : regionIdsByPriority) {
                String kit = findEntry(kitsSection, worldName, regionId);
                if (kit != null) {
                    return kit;
                }
            }
        }
        return findEntry(kitsSection, worldName, null);
    }

    private static String findEntry(ConfigurationSection kitsSection, String worldName, String regionId) {
        for (String key : kitsSection.getKeys(false)) {
            int separator = key.indexOf(':');
            String keyWorld = (separator < 0 ? key : key.substring(0, separator)).trim();
            String keyRegion = separator < 0 ? null : key.substring(separator + 1).trim();
            if (!keyWorld.equalsIgnoreCase(worldName)) {
                continue;
            }
            boolean regionMatches = regionId == null
                    ? keyRegion == null
                    : keyRegion != null && keyRegion.equalsIgnoreCase(regionId);
            if (!regionMatches) {
                continue;
            }
            String kit = readKitName(kitsSection, key);
            if (kit != null) {
                return kit;
            }
        }
        return null;
    }

    private static String readKitName(ConfigurationSection kitsSection, String key) {
        ConfigurationSection entry = kitsSection.getConfigurationSection(key);
        String kit = entry != null ? entry.getString("kit") : kitsSection.getString(key);
        if (kit == null || kit.trim().isEmpty()) {
            return null;
        }
        return kit.trim();
    }
}
