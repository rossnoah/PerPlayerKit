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
package dev.noah.perplayerkit.util;

import com.google.common.primitives.Ints;
import org.bukkit.plugin.Plugin;

/**
 * Holds the configured number of kit slots each player has ({@code max-kits}).
 * The limit applies to kits and enderchest kits alike. Static (not a
 * {@code get()} singleton) so code paths under unit test keep working without
 * a Bukkit server, falling back to the default of 9.
 */
public final class KitSlots {

    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 99;
    public static final int SLOTS_PER_PAGE = 9;
    public static final int DEFAULT_LIMIT = 9;

    private static int maxKits = DEFAULT_LIMIT;

    private KitSlots() {
    }

    public static void init(Plugin plugin) {
        int configured = plugin.getConfig().getInt("max-kits", DEFAULT_LIMIT);
        int clamped = Ints.constrainToRange(configured, MIN_LIMIT, MAX_LIMIT);
        if (clamped != configured) {
            plugin.getLogger().warning("max-kits is set to " + configured + " but must be between "
                    + MIN_LIMIT + " and " + MAX_LIMIT + ", using " + clamped);
        }
        maxKits = clamped;
    }

    public static int maxKits() {
        return maxKits;
    }

    public static int pageCount() {
        return (maxKits + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
    }

    /**
     * The main-menu page (0-indexed) a kit slot appears on.
     */
    public static int pageOf(int slot) {
        return (slot - 1) / SLOTS_PER_PAGE;
    }

    /**
     * Parses a slot suffix as it appears in command labels ("k12") and storage
     * IDs ("&lt;uuid&gt;12"): digits only, no leading zero, within the absolute
     * 1..{@link #MAX_LIMIT} range. Returns null if the suffix is not a valid
     * slot number. Deliberately not bounded by the configured max-kits — the
     * database may hold slots above a lowered limit; callers enforce the
     * configured bound where it applies.
     */
    public static Integer parseSlotSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty() || suffix.charAt(0) == '0') {
            return null;
        }
        Integer slot = Ints.tryParse(suffix);
        if (slot == null || slot < MIN_LIMIT || slot > MAX_LIMIT) {
            return null;
        }
        return slot;
    }

    public static void setForTesting(int value) {
        maxKits = value;
    }

    public static void resetForTesting() {
        maxKits = DEFAULT_LIMIT;
    }
}
