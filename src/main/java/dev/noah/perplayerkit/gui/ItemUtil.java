package dev.noah.perplayerkit.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class ItemUtil {

    public static ItemStack createItem(Material m, int quantity, String name) {
        ItemStack i = new ItemStack(m, quantity);
        ItemMeta itemMeta = i.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        i.setItemMeta(itemMeta);
        return i;
    }

    public static ItemStack createItem(Material m, String name) {
        int quantity = 1;
        ItemStack i = new ItemStack(m, quantity);
        ItemMeta itemMeta = i.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        i.setItemMeta(itemMeta);
        return i;
    }

    public static ItemStack createItem(Material m, int quantity, String name, String s1, String s2) {
        ItemStack i = new ItemStack(m, quantity);
        ItemMeta itemMeta = i.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        ArrayList<String> lore = new ArrayList<>(); // Create an ArrayList object
        lore.add(ChatColor.translateAlternateColorCodes('&', s1));
        lore.add(ChatColor.translateAlternateColorCodes('&', s2));
        itemMeta.setLore(lore);
        i.setItemMeta(itemMeta);
        return i;
    }

    public static ItemStack createItem(Material m, int quantity, String name, String s1) {
        ItemStack i = new ItemStack(m, quantity);
        ItemMeta itemMeta = i.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        ArrayList<String> lore = new ArrayList<>(); // Create an ArrayList object
        lore.add(ChatColor.translateAlternateColorCodes('&', s1));
        itemMeta.setLore(lore);
        i.setItemMeta(itemMeta);
        return i;
    }

    public static ItemStack createItem(Material m, int quantity, String name, String s1, String s2, String s3) {
        ItemStack i = new ItemStack(m, quantity);
        ItemMeta itemMeta = i.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        ArrayList<String> lore = new ArrayList<>(); // Create an ArrayList object
        lore.add(ChatColor.translateAlternateColorCodes('&', s1));
        lore.add(ChatColor.translateAlternateColorCodes('&', s2));
        lore.add(ChatColor.translateAlternateColorCodes('&', s3));

        itemMeta.setLore(lore);
        i.setItemMeta(itemMeta);
        return i;
    }


    public static ItemStack addEnchantLook(ItemStack i) {
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (meta != null) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
        }
        i.setItemMeta(meta);
        return i;
    }

    public static ItemStack setName(ItemStack i, String name) {
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        i.setItemMeta(meta);
        return i;
    }


}
