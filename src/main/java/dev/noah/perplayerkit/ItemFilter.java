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

import dev.noah.perplayerkit.util.PotionEffectsCompat;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.entity.Player;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import java.util.*;

/** Applies one configured policy to copied items, including stored enchants and nested contents. */
public class ItemFilter {
    public static Set<String> whitelist;
    private static ItemFilter instance;
    private static final int MAX_CONTAINER_DEPTH = 16;
    private final ItemFilterRules.Settings settings;
    private final Plugin plugin;
    private final Set<String> warnings = new HashSet<>();

    public ItemFilter(Plugin plugin) {
        this.plugin = plugin;
        this.settings = ItemFilterRules.parse(plugin.getConfig());
        whitelist = new HashSet<>();
        instance = this;
    }
    public static ItemFilter get() {
        if (instance == null) throw new IllegalStateException("ItemFilter has not been initialized");
        return instance;
    }
    public static ItemStack[] copy(ItemStack[] input) { return KitContents.copy(input); }
    public boolean isReady() { return !settings.enabled() || !settings.kitRoomOnly() || !whitelist.isEmpty(); }
    public boolean filtersImports() { return settings.enabled() && settings.imports(); }
    public boolean filtersSaves() { return settings.enabled() && settings.saves(); }
    public ItemStack[] filterItemStack(ItemStack[] input) { return filter(input, 0); }

    /** Refuse an entirely filtered kit instead of clearing the destination inventory/editor. */
    public ItemStack[] filterKit(ItemStack[] input, Player recipient) {
        ItemStack[] output = filterItemStack(input);
        if (!KitContents.isEmpty(input) && KitContents.isEmpty(output)) {
            if (recipient != null) Lang.get().send(recipient, "error.item-filter-empty");
            return null;
        }
        return output;
    }

    private ItemStack[] filter(ItemStack[] input, int depth) {
        ItemStack[] output = KitContents.copy(input);
        if (output == null || !settings.enabled()) return output;
        for (int i = 0; i < output.length; i++) {
            ItemStack item = output[i];
            if (item == null) continue;
            try {
                if (depth > MAX_CONTAINER_DEPTH || !allowed(item)) { output[i] = null; continue; }
                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta instanceof BlockStateMeta meta && meta.getBlockState() instanceof Container container) {
                    container.getInventory().setContents(filter(container.getInventory().getContents(), depth + 1));
                    meta.setBlockState(container); item.setItemMeta(meta);
                }
                if (itemMeta instanceof BundleMeta meta) {
                    ItemStack[] nested = filter(meta.getItems().toArray(new ItemStack[0]), depth + 1);
                    List<ItemStack> kept = new ArrayList<>();
                    for (ItemStack child : nested) if (child != null) kept.add(child);
                    meta.setItems(kept); item.setItemMeta(meta);
                }
            } catch (RuntimeException | LinkageError error) {
                output[i] = null;
                String message = "Could not filter " + item.getType() + ": " + error;
                if (warnings.add(message)) plugin.getLogger().warning(message);
            }
        }
        return output;
    }

    public static boolean isSafe(ItemStack item) { return get().allowed(item); }
    private boolean allowed(ItemStack item) {
        if (!settings.enabled() || item == null || item.getType().isAir()) return true;
        if (settings.kitRoomOnly() && !whitelist.contains(item.getType().name())) return false;
        if (item.getAmount() < 1 || item.getAmount() > item.getMaxStackSize()) return false;
        ItemFilterRules.Rule rule = settings.forMaterial(item.getType());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!rule.unbreakable() && meta.isUnbreakable()) return false;
            if (!rule.attributes() && meta.hasAttributeModifiers()) return false;
            if (!rule.flags() && !meta.getItemFlags().isEmpty()) return false;
        }
        if (!allowedEnchants(item, item.getEnchantments(), rule, false)) return false;
        if (meta instanceof EnchantmentStorageMeta book && !allowedEnchants(item, book.getStoredEnchants(), rule, true)) return false;
        if (meta instanceof PotionMeta potion && rule.checksPotions()) {
            for (PotionEffect effect : PotionEffectsCompat.baseEffects(potion)) if (!allowedEffect(effect, rule)) return false;
            for (PotionEffect effect : potion.getCustomEffects()) if (!allowedEffect(effect, rule)) return false;
        }
        return true;
    }
    private boolean allowedEnchants(ItemStack item, Map<Enchantment, Integer> enchants, ItemFilterRules.Rule rule, boolean stored) {
        for (var entry : enchants.entrySet()) {
            Enchantment enchant = entry.getKey(); int level = entry.getValue();
            Integer max = rule.enchantments().get(enchant.getKey().toString());
            if (level < 1) return false;
            if (max != null) {
                if (max > 0 && level > max) return false;
            } else if (!rule.overLevelled() && level > enchant.getMaxLevel()) return false;
            if (!rule.incompatible()) {
                if (!stored && !enchant.canEnchantItem(item)) return false;
                for (Enchantment other : enchants.keySet())
                    if (other != enchant && (enchant.conflictsWith(other) || other.conflictsWith(enchant))) return false;
            }
        }
        return true;
    }
    private boolean allowedEffect(PotionEffect effect, ItemFilterRules.Rule rule) {
        long level = (long) effect.getAmplifier() + 1;
        int duration = effect.getDuration();
        if (level < 1 || duration < -1 || exceeds(level, duration, rule.potionLevel(), rule.potionDuration())) return false;
        ItemFilterRules.EffectLimit limit = rule.effects().get(effect.getType().getKey().toString());
        return limit == null || !exceeds(level, duration, limit.level(), limit.durationSeconds());
    }
    private boolean exceeds(long level, int ticks, int maxLevel, int maxSeconds) {
        return (maxLevel > 0 && level > maxLevel) || (maxSeconds > 0 && (ticks < 0 || ticks > (long) maxSeconds * 20));
    }
    public void addToWhitelist(Collection<ItemStack[]> pages) {
        for (ItemStack[] page : pages) if (page != null) for (ItemStack item : page) addWhitelistedItem(item, 0);
    }
    private void addWhitelistedItem(ItemStack item, int depth) {
        if (item == null || item.getType().isAir() || depth > MAX_CONTAINER_DEPTH) return;
        whitelist.add(item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta block && block.getBlockState() instanceof Container container)
            for (ItemStack nested : container.getInventory().getContents()) addWhitelistedItem(nested, depth + 1);
        if (meta instanceof BundleMeta bundle) for (ItemStack nested : bundle.getItems()) addWhitelistedItem(nested, depth + 1);
    }
    public void clearWhitelist() { whitelist.clear(); }
}
