package dev.noah.perplayerkit;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

/** Stored layouts and ownership rules shared by kit saving, loading, and editors. */
public final class KitContents {
    public static final int INVENTORY_SIZE = 41;
    public static final int ENDERCHEST_SIZE = 27;
    public static final int ROOM_SIZE = 45;

    private KitContents() {}

    public static boolean hasSize(ItemStack[] items, int size) {
        return items != null && items.length == size;
    }

    public static boolean isEmpty(ItemStack[] items) {
        if (items == null) return true;
        for (ItemStack item : items) {
            if (!isEmpty(item)) return false;
        }
        return true;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    public static ItemStack[] copy(ItemStack[] items) {
        if (items == null) return null;
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) copy[i] = items[i] == null ? null : items[i].clone();
        return copy;
    }

    public static ItemStack[] copyRange(ItemStack[] items, int start, int count) {
        Objects.checkFromIndexSize(start, count, items.length);
        return copy(Arrays.copyOfRange(items, start, start + count));
    }

    /** Returns an owned, nonempty snapshot, or null when the save is invalid. */
    static ItemStack[] prepare(ItemStack[] items, boolean enderchest) {
        if (!hasSize(items, enderchest ? ENDERCHEST_SIZE : INVENTORY_SIZE)) return null;
        ItemStack[] prepared = copy(items);
        for (int i = 0; i < prepared.length; i++) {
            if (isEmpty(prepared[i])) prepared[i] = null;
        }
        if (!enderchest) {
            keepArmor(prepared, 36, "BOOTS");
            keepArmor(prepared, 37, "LEGGINGS");
            keepArmor(prepared, 38, "CHESTPLATE", "ELYTRA");
            keepArmor(prepared, 39, "HELMET");
        }
        return isEmpty(prepared) ? null : prepared;
    }

    private static void keepArmor(ItemStack[] items, int slot, String... allowedNames) {
        ItemStack item = items[slot];
        if (item == null) return;
        String material = item.getType().name();
        for (String allowed : allowedNames) {
            if (material.contains(allowed)) return;
        }
        items[slot] = null;
    }
}
