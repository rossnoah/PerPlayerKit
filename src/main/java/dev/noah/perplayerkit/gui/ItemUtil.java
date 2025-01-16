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
package dev.noah.perplayerkit.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemUtil {

    public static ItemStack createItem(Material material, int quantity, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, quantity);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }

            if (loreLines != null && loreLines.length > 0) {
                List<String> lore = new ArrayList<>();
                Arrays.stream(loreLines)
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .forEach(lore::add);
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createItem(Material material, String name) {
        return createItem(material, 1, name);
    }

    public static ItemStack createItem(Material material, int quantity, String name) {
        return createItem(material, quantity, name, new String[0]);
    }

    public static ItemStack addHideFlags(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack addEnchantLook(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE);
            item.setItemMeta(meta);
        }

        return item;
    }

}
