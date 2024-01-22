package net.vanillapractice.perplayerkit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class Filter {


    public static ItemStack[] filterItemStack(ItemStack[] input) {
        ItemStack[] output = input.clone();
        for (ItemStack item : output) {
            if (!isSafe(item)) {
                item.setType(Material.AIR);
                //item = null;
            }


            if (item != null) {

                if (item.getType().toString().contains("SHULKER_BOX")) {
                    if (item.getItemMeta() instanceof BlockStateMeta) {
                        BlockStateMeta im = (BlockStateMeta) item.getItemMeta();
                        if (im.getBlockState() instanceof ShulkerBox) {
                            ShulkerBox shulker = (ShulkerBox) im.getBlockState();
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
            if (!(PerPlayerKit.whitelist.contains(i.getType().toString()))) {
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
                if (meta.hasAttributeModifiers()) {
                    return false;
                }
                return meta.getItemFlags().isEmpty();

            }

        }
        return true;
    }

    public static void createWhitelist() {
        for (ItemStack[] itemStacks : PerPlayerKit.kitroomData) {

            for (ItemStack item : itemStacks) {
                if (item != null) {
                    if (!PerPlayerKit.whitelist.contains(item.getType().toString())) {
                        PerPlayerKit.whitelist.add(item.getType().toString());
                    }

                }
            }

        }

        Bukkit.getLogger().info("Whitelist created");
    }


}
