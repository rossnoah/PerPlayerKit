/*
 * Copyright 2026 Noah Ross
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
package dev.noah.perplayerkit.starter;

import dev.noah.perplayerkit.util.StyleManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Turns a parsed {@link ItemSpec} into a live {@link ItemStack}.
 *
 * <p>Anything the running server does not recognise - a material, an
 * enchantment, a potion - is reported to the warning sink and dropped rather
 * than failing the whole page, so the bundled defaults still mostly land on
 * older Minecraft versions.
 */
public final class ItemSpecFactory {

    private ItemSpecFactory() {
    }

    /**
     * Builds the first alternative this server can actually make, so a slot can
     * ask for 1.21 gear and still land on 1.19. Falling back is silent - it is
     * the point of writing alternatives - and only a slot where nothing at all
     * resolves is worth a warning.
     *
     * @param options one spec per alternative, best first
     * @return the item, or null if this server has none of the alternatives
     */
    public static ItemStack create(List<ItemSpec> options, Consumer<String> warn) {
        for (ItemSpec option : options) {
            if (isAvailable(option)) {
                return create(option, warn);
            }
        }

        warn.accept("no version of " + options.get(0).getMaterial() + " is available here, skipping item");
        return null;
    }

    /** Whether this server has the material, every enchantment and the potion. */
    private static boolean isAvailable(ItemSpec spec) {
        Material material = Material.matchMaterial(spec.getMaterial());
        if (material == null || material.isAir()) {
            return false;
        }
        for (String id : spec.getEnchantments().keySet()) {
            if (resolveEnchantment(id) == null) {
                return false;
            }
        }
        return spec.getPotionType() == null || PotionCompat.exists(spec.getPotionType());
    }

    /**
     * @param warn receives one line per part of the spec this server cannot honour
     * @return the item, or null if the material itself is unknown here
     */
    public static ItemStack create(ItemSpec spec, Consumer<String> warn) {
        Material material = Material.matchMaterial(spec.getMaterial());
        if (material == null || material.isAir()) {
            warn.accept("unknown material " + spec.getMaterial() + ", skipping item");
            return null;
        }

        ItemStack item = new ItemStack(material, spec.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (spec.getDisplayName() != null) {
            meta.setDisplayName(StyleManager.convertMiniMessage(spec.getDisplayName()));
        }

        for (Map.Entry<String, Integer> entry : spec.getEnchantments().entrySet()) {
            Enchantment enchantment = resolveEnchantment(entry.getKey());
            if (enchantment == null) {
                warn.accept("unknown enchantment " + entry.getKey() + " on " + spec.getMaterial());
                continue;
            }
            meta.addEnchant(enchantment, entry.getValue(), true);
        }

        if (spec.getPotionType() != null) {
            if (meta instanceof PotionMeta potionMeta) {
                if (!PotionCompat.apply(potionMeta, spec.getPotionType())) {
                    warn.accept("unknown potion " + spec.getPotionType() + " on " + spec.getMaterial());
                }
            } else {
                warn.accept(spec.getMaterial() + " cannot hold a potion type");
            }
        }

        if (!spec.getContents().isEmpty()) {
            fillContainer(spec, meta, warn);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Puts the spec's contents inside a shulker box. The block state has to be
     * written back onto the meta afterwards - mutating the inventory alone does
     * not stick.
     */
    private static void fillContainer(ItemSpec spec, ItemMeta meta, Consumer<String> warn) {
        if (!(meta instanceof BlockStateMeta blockMeta) || !(blockMeta.getBlockState() instanceof Container box)) {
            warn.accept(spec.getMaterial() + " cannot hold items");
            return;
        }

        Inventory inventory = box.getInventory();
        for (Map.Entry<Integer, ItemSpec> entry : spec.getContents().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= inventory.getSize()) {
                warn.accept(spec.getMaterial() + " has no slot " + slot + ", skipping item");
                continue;
            }
            ItemStack nested = create(entry.getValue(), warn);
            if (nested != null) {
                inventory.setItem(slot, nested);
            }
        }
        blockMeta.setBlockState(box);
    }

    private static Enchantment resolveEnchantment(String id) {
        try {
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
