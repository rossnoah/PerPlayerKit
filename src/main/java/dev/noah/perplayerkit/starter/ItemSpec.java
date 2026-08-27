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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single item from the bundled starter defaults, parsed but not yet turned
 * into an {@link org.bukkit.inventory.ItemStack}. Holding the raw names rather
 * than Bukkit objects keeps {@link ItemSpecParser} testable off a live server
 * and lets unknown materials and enchantments be skipped one at a time.
 */
public final class ItemSpec {

    private final String material;
    private final int amount;
    private final String potionType;
    private final String displayName;
    private final Map<String, Integer> enchantments;
    private final Map<Integer, ItemSpec> contents;

    public ItemSpec(String material, int amount, String potionType, String displayName,
                    Map<String, Integer> enchantments) {
        this(material, amount, potionType, displayName, enchantments, Map.of());
    }

    public ItemSpec(String material, int amount, String potionType, String displayName,
                    Map<String, Integer> enchantments, Map<Integer, ItemSpec> contents) {
        this.material = material;
        this.amount = amount;
        this.potionType = potionType;
        this.displayName = displayName;
        this.enchantments = Collections.unmodifiableMap(new LinkedHashMap<>(enchantments));
        this.contents = Collections.unmodifiableMap(new LinkedHashMap<>(contents));
    }

    /** The same item, carrying the given container contents. */
    public ItemSpec withContents(Map<Integer, ItemSpec> newContents) {
        return new ItemSpec(material, amount, potionType, displayName, enchantments, newContents);
    }

    /** Material name as written in the defaults file, e.g. {@code NETHERITE_HELMET}. */
    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    /** Base potion type name, or null for items that are not potions. */
    public String getPotionType() {
        return potionType;
    }

    /** MiniMessage display name, or null to leave the vanilla name. */
    public String getDisplayName() {
        return displayName;
    }

    /** Enchantment id to level, in the order they were written. */
    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    /**
     * Items stored inside this one, by slot in its own inventory. Only shulker
     * boxes and other containers can hold anything; empty for everything else.
     */
    public Map<Integer, ItemSpec> getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return "ItemSpec{" + material + " x" + amount
                + (potionType != null ? " potion=" + potionType : "")
                + (enchantments.isEmpty() ? "" : " " + enchantments)
                + '}';
    }
}
