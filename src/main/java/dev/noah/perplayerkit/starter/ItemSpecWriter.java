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

import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a live {@link ItemStack} back out in the notation the bundled defaults
 * use - the inverse of {@link ItemSpecParser}, so a kit room arranged in game
 * can be read back into {@code defaults/kitroom.yml}.
 *
 * <p>Only what the notation can express survives the trip: material, amount,
 * enchantments, base potion and display name, plus the contents of a shulker
 * box. Anything else on the item is dropped, which is the point - the defaults
 * are meant to be plain.
 */
public final class ItemSpecWriter {

    private ItemSpecWriter() {
    }

    /** @return the one-line form, e.g. {@code NETHERITE_SWORD { sharpness=5 }} */
    public static String write(ItemStack item) {
        StringBuilder out = new StringBuilder(item.getType().name());
        if (item.getAmount() != 1) {
            out.append(" x").append(item.getAmount());
        }

        List<String> attributes = attributesOf(item);
        if (!attributes.isEmpty()) {
            out.append(" { ").append(String.join(", ", attributes)).append(" }");
        }
        return out.toString();
    }

    private static List<String> attributesOf(ItemStack item) {
        List<String> attributes = new ArrayList<>();
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        if (meta == null) {
            return attributes;
        }

        if (meta.hasDisplayName()) {
            attributes.add("name=\"" + meta.getDisplayName().replace("\"", "'") + "\"");
        }
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            attributes.add(entry.getKey().getKey().getKey() + "=" + entry.getValue());
        }
        if (meta instanceof PotionMeta potionMeta) {
            String potion = PotionCompat.read(potionMeta);
            if (potion != null) {
                attributes.add("potion=" + potion);
            }
        }
        return attributes;
    }

    /**
     * @return what is inside a shulker box, by slot, or an empty map for
     *         anything that is not a container or is empty
     */
    public static Map<Integer, ItemStack> contentsOf(ItemStack item) {
        Map<Integer, ItemStack> contents = new LinkedHashMap<>();
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof BlockStateMeta blockMeta)) {
            return contents;
        }
        if (!blockMeta.hasBlockState() || !(blockMeta.getBlockState() instanceof Container box)) {
            return contents;
        }

        Inventory inventory = box.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack nested = inventory.getItem(slot);
            if (nested != null && !nested.getType().isAir()) {
                contents.put(slot, nested);
            }
        }
        return contents;
    }
}
