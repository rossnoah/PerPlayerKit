package dev.noah.perplayerkit.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class InvUtil {

    public static Inventory addItem(Inventory inv, int slot, Material material, int quantity, String name, boolean enchanted) {
        ItemStack item = new ItemStack(material, quantity);
        if (enchanted) {
            ItemUtil.addEnchantLook(item);
        }
        if (!name.equalsIgnoreCase("false")) {
            ItemUtil.setName(item, name);
        }
        inv.setItem(slot, item);
        return inv;
    }

    public static Inventory toggle(Inventory inv, int slot) {

        if (Objects.requireNonNull(inv.getItem(slot)).getType() == Material.RED_CONCRETE && Objects.requireNonNull(inv.getItem(slot)).getAmount() == 1) {
            addItem(inv, slot, Material.GREEN_CONCRETE, 1, Objects.requireNonNull(Objects.requireNonNull(inv.getItem(slot)).getItemMeta()).getDisplayName(), false);
            return inv;
        } else if (Objects.requireNonNull(inv.getItem(slot)).getType() == Material.GREEN_CONCRETE && Objects.requireNonNull(inv.getItem(slot)).getAmount() == 1) {
            addItem(inv, slot, Material.RED_CONCRETE, 1, Objects.requireNonNull(Objects.requireNonNull(inv.getItem(slot)).getItemMeta()).getDisplayName(), false);
            return inv;
        }
        return inv;

    }

}
