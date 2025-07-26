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

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerUtil {

    public static void repairItem(ItemStack i) {
        if (i != null) {
            ItemMeta meta = i.getItemMeta();
            Damageable damageable = (Damageable) meta;
            if (damageable != null && damageable.hasDamage()) {
                damageable.setDamage(0);
            }
            i.setItemMeta(damageable);
        }

    }

    public static void repairAll(Player p) {

        for (ItemStack i : p.getInventory().getContents()) {
            repairItem(i);
        }
        p.sendMessage(ChatColor.GREEN + "All items repaired!");
    }

    public static void healPlayer(Player p) {
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(20);

        // Remove potion effects if configured to do so
        if (PerPlayerKit.getPlugin().getConfig().getBoolean("feature.heal-remove-effects", false)) {
            p.getActivePotionEffects().forEach(potionEffect -> p.removePotionEffect(potionEffect.getType()));
        }

        p.sendMessage(ChatColor.GREEN + "You have been healed!");
    }

    public static void healPlayerSilent(Player p) {
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(20);
    }

}
