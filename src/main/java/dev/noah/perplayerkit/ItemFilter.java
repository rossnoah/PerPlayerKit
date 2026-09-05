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
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
        isEnabled = plugin.getConfig().getBoolean("anti-exploit.only-allow-kitroom-items",false);
    }

    public static ItemFilter get(){
        if(instance == null){
            throw new IllegalStateException("ItemFilter has not been initialized yet!");
        }
        return instance;
    }


    public static ItemStack[] copy(ItemStack[] input) {
        if (input == null) return null;
        ItemStack[] output = new ItemStack[input.length];
        for (int i = 0; i < input.length; i++) output[i] = input[i] == null ? null : input[i].clone();
        return output;
    }

    public boolean isReady() { return !isEnabled || !whitelist.isEmpty(); }

    public ItemStack[] filterItemStack(ItemStack[] input) {
        ItemStack[] output = copy(input);
        if (output == null || !isEnabled) return output;
        for (int i = 0; i < output.length; i++) {
            ItemStack item = output[i];
            if (item == null) continue;
            if (!isSafe(item)) { output[i] = null; continue; }
            if (item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof Container container) {
                container.getInventory().setContents(filterItemStack(container.getInventory().getContents()));
                meta.setBlockState(container);
                item.setItemMeta(meta);
            }
            if (item.getItemMeta() instanceof BundleMeta meta) {
                ItemStack[] nested = filterItemStack(meta.getItems().toArray(new ItemStack[0]));
                List<ItemStack> safe = new ArrayList<>();
                for (ItemStack child : nested) if (child != null) safe.add(child);
                meta.setItems(safe);
                item.setItemMeta(meta);
            }
        }
        return output;
    }

    public static boolean isSafe(ItemStack i) {

        if (i != null && !i.getType().isAir()) {
            if (!(whitelist.contains(i.getType().toString()))) {
                return false;
            }
            {
                if (i.getAmount() < 1 || i.getAmount() > i.getMaxStackSize()) {
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
                addWhitelistedItem(item);

            }
        }
    }

    private void addWhitelistedItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        whitelist.add(item.getType().toString());
        if (item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof Container container)
            for (ItemStack nested : container.getInventory().getContents()) addWhitelistedItem(nested);
        if (item.getItemMeta() instanceof BundleMeta meta)
            meta.getItems().forEach(this::addWhitelistedItem);
    }

    public void clearWhitelist() {
        whitelist.clear();
    }


}