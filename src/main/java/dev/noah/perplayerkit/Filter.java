package dev.noah.perplayerkit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Filter {


    public static Set<String> whitelist;
    private static Filter instance;

    public Filter() {
        whitelist = new HashSet<>();
    }

    public static Filter get(){
        if(instance == null){
            instance = new Filter();
        }
        return instance;
    }


    public static ItemStack[] filterItemStack(ItemStack[] input) {
        ItemStack[] output = input.clone();
        for (ItemStack item : output) {
            if (!isSafe(item)) {
                item.setType(Material.AIR);
                //item = null;
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

    public void createWhitelist(Collection<ItemStack[]> items) {
        for (ItemStack[] itemStacks : items) {
            for (ItemStack item : itemStacks) {
                if (item != null) {
                        whitelist.add(item.getType().toString());
                }
            }
        }
        Bukkit.getLogger().info("Whitelist created");
    }


}
