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
package dev.noah.perplayerkit;

import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes every occurrence of a material from an item stack array, including
 * items nested inside shulker boxes of any color, any other container block
 * stored as an item (chests, barrels, dispensers, ...), and bundles.
 */
public final class ItemPurger {

    private ItemPurger() {
    }

    /**
     * Removes all items of the given material from the array (in place).
     *
     * @return the total number of items removed, counting stack amounts
     */
    public static int purgeContents(ItemStack[] contents, Material target) {
        if (contents == null) {
            return 0;
        }

        int removed = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) {
                continue;
            }
            if (item.getType() == target) {
                removed += item.getAmount();
                contents[i] = null;
                continue;
            }
            removed += purgeNested(item, target);
        }
        return removed;
    }

    private static int purgeNested(ItemStack item, Material target) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        int removed = 0;

        // Shulker boxes of all types and any other container block held as an item.
        if (meta instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.getBlockState() instanceof Container container) {
            ItemStack[] inner = container.getInventory().getContents();
            int innerRemoved = purgeContents(inner, target);
            if (innerRemoved > 0) {
                container.getInventory().setContents(inner);
                blockStateMeta.setBlockState(container);
                item.setItemMeta(blockStateMeta);
                removed += innerRemoved;
            }
        }

        if (meta instanceof BundleMeta bundleMeta && !bundleMeta.getItems().isEmpty()) {
            List<ItemStack> kept = new ArrayList<>();
            int bundleRemoved = 0;
            for (ItemStack bundleItem : bundleMeta.getItems()) {
                if (bundleItem == null) {
                    continue;
                }
                if (bundleItem.getType() == target) {
                    bundleRemoved += bundleItem.getAmount();
                    continue;
                }
                bundleRemoved += purgeNested(bundleItem, target);
                kept.add(bundleItem);
            }
            if (bundleRemoved > 0) {
                bundleMeta.setItems(kept);
                item.setItemMeta(bundleMeta);
                removed += bundleRemoved;
            }
        }

        return removed;
    }

    /**
     * @return true when the array contains no items (only null or air slots)
     */
    public static boolean isEmpty(ItemStack[] contents) {
        if (contents == null) {
            return true;
        }
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }
}
