package dev.noah.perplayerkit.util;

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
        p.sendMessage(ChatColor.GREEN + "You have been healed!");
    }

    public static void healPlayerSilent(Player p) {
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(20);
    }

}
