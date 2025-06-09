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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ItemFilter {

    public static Set<String> whitelist;
    private static ItemFilter instance;

    private Plugin plugin;
    private boolean isEnabled;

    public ItemFilter(Plugin plugin) {
        whitelist = new HashSet<>();
        this.plugin = plugin;
        instance = this;
        reloadConfig();
    }

    public static ItemFilter get() {
        if (instance == null) {
            throw new IllegalStateException("ItemFilter has not been initialized yet!");
        }
        return instance;
    }

    /**
     * Reloads configuration settings for the ItemFilter
     */
    public void reloadConfig() {
        isEnabled = ConfigManager.get().isOnlyAllowKitroomItems();
    }

    public ItemStack[] filterItemStack(ItemStack[] input) {

        if (!isEnabled) {
            return input;
        }

        ItemStack[] output = input.clone();
        for (ItemStack item : output) {
            if (!isSafe(item)) {
                item.setType(Material.AIR);
                // item = null;
            }

            if (item != null) {

                if (item.getType().toString().contains("SHULKER_BOX")) {
                    if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                        if (blockStateMeta.getBlockState() instanceof ShulkerBox shulker) {
                            shulker.getInventory().setContents(filterItemStack(shulker.getInventory().getContents()));
                        }
                    }
                }
            }

        }

        return output;
    }

    public static boolean isSafe(ItemStack i) {

        if (i != null) {
            if (!(whitelist.contains(i.getType().toString()))) {
                return false;
            }
            if (i.getAmount() != -1) {
                if (i.getAmount() > i.getMaxStackSize()) {
                    return false;
                }
            }
            for (Enchantment e : i.getEnchantments().keySet()) {
                if (i.getEnchantmentLevel(e) > e.getMaxLevel()) {
                    return false;
                }

            }

            if (i.hasItemMeta()) {
                ItemMeta meta = i.getItemMeta();
                if (meta != null && meta.hasAttributeModifiers()) {
                    return false;
                }
                return meta != null && meta.getItemFlags().isEmpty();

            }

        }
        return true;
    }

    public void addToWhitelist(Collection<ItemStack[]> items) {
        for (ItemStack[] itemStacks : items) {
            for (ItemStack item : itemStacks) {
                if (item != null) {
                    whitelist.add(item.getType().toString());
                }
            }
        }
    }

    public void clearWhitelist() {
        whitelist.clear();
    }

}
