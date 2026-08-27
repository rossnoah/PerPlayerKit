/*
 * Copyright 2026 Noah Ross
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
package dev.noah.perplayerkit.starter;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Sets the base potion of a {@link PotionMeta} from a 1.20.5+ style name such as
 * {@code STRONG_HEALING}.
 *
 * <p>1.20.5 folded potency into {@link PotionType} and added
 * {@code setBasePotionType}. On older servers the same potion is the old enum
 * constant plus extended/upgraded flags, set through the now-removed
 * {@code PotionData}. That older path lives in a nested class so its classes are
 * only loaded on the servers that still have them.
 */
final class PotionCompat {

    private static final Method SET_BASE_POTION_TYPE = findSetBasePotionType();

    private PotionCompat() {
    }

    private static Method findSetBasePotionType() {
        try {
            return PotionMeta.class.getMethod("setBasePotionType", PotionType.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * @return false if this server has no such potion, leaving the meta untouched
     */
    static boolean apply(PotionMeta meta, String potionName) {
        String name = potionName.toUpperCase(Locale.ROOT);

        if (SET_BASE_POTION_TYPE != null) {
            try {
                SET_BASE_POTION_TYPE.invoke(meta, PotionType.valueOf(name));
                return true;
            } catch (IllegalArgumentException | ReflectiveOperationException e) {
                return false;
            }
        }

        return Legacy.apply(meta, name);
    }

    /**
     * Reads a meta's base potion back out as a 1.20.5+ style name, for writing
     * a live item into the defaults notation.
     *
     * @return the name, or null if the item has no base potion
     */
    static String read(PotionMeta meta) {
        if (SET_BASE_POTION_TYPE != null) {
            try {
                Object type = PotionMeta.class.getMethod("getBasePotionType").invoke(meta);
                return type == null ? null : ((PotionType) type).name();
            } catch (ReflectiveOperationException | ClassCastException e) {
                return null;
            }
        }
        return Legacy.read(meta);
    }

    /** @return whether this server has the named potion at all */
    static boolean exists(String potionName) {
        String name = potionName.toUpperCase(Locale.ROOT);

        if (SET_BASE_POTION_TYPE != null) {
            try {
                PotionType.valueOf(name);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return legacyType(name) != null;
    }

    /**
     * Resolves a 1.20.5+ potion name to the pre-1.20.5 enum constant it used to
     * be, or null if no such potion has ever existed. {@link PotionType} is a
     * plain enum, so this also lets tests check the bundled defaults without a
     * running server.
     */
    static PotionType legacyType(String potionName) {
        String base = Legacy.stripPotency(potionName.toUpperCase(Locale.ROOT));
        try {
            return PotionType.valueOf(Legacy.RENAMED.getOrDefault(base, base));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Pre-1.20.5 potions: a base type plus extended/upgraded flags. */
    private static final class Legacy {

        /** Constants 1.20.5 renamed; everything else kept its name. */
        private static final Map<String, String> RENAMED = Map.of(
                "HEALING", "INSTANT_HEAL",
                "HARMING", "INSTANT_DAMAGE",
                "REGENERATION", "REGEN",
                "SWIFTNESS", "SPEED",
                "LEAPING", "JUMP"
        );

        /** Rebuilds a 1.20.5+ name from the old type plus its extended/upgraded flags. */
        @SuppressWarnings("deprecation")
        static String read(PotionMeta meta) {
            try {
                org.bukkit.potion.PotionData data = meta.getBasePotionData();
                if (data == null || data.getType() == null) {
                    return null;
                }
                String base = data.getType().name();
                for (Map.Entry<String, String> renamed : RENAMED.entrySet()) {
                    if (renamed.getValue().equals(base)) {
                        base = renamed.getKey();
                        break;
                    }
                }
                if (data.isUpgraded()) {
                    return "STRONG_" + base;
                }
                return data.isExtended() ? "LONG_" + base : base;
            } catch (IllegalArgumentException | LinkageError e) {
                return null;
            }
        }

        static String stripPotency(String name) {
            if (name.startsWith("LONG_")) {
                return name.substring("LONG_".length());
            }
            if (name.startsWith("STRONG_")) {
                return name.substring("STRONG_".length());
            }
            return name;
        }

        @SuppressWarnings("deprecation")
        static boolean apply(PotionMeta meta, String name) {
            PotionType type = legacyType(name);
            if (type == null) {
                return false;
            }

            try {
                meta.setBasePotionData(new org.bukkit.potion.PotionData(
                        type,
                        name.startsWith("LONG_") && type.isExtendable(),
                        name.startsWith("STRONG_") && type.isUpgradeable()));
                return true;
            } catch (IllegalArgumentException | LinkageError e) {
                return false;
            }
        }
    }
}
