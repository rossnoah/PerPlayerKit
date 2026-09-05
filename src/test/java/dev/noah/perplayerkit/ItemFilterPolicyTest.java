package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.PotionEffectsCompat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemFilterPolicyTest {
    private org.mockito.MockedStatic<org.bukkit.enchantments.Enchantment> registeredEnchants;
    private org.mockito.MockedStatic<org.bukkit.potion.PotionEffectType> registeredEffects;
    @org.junit.jupiter.api.BeforeEach void registries() {
        registeredEnchants = org.mockito.Mockito.mockStatic(org.bukkit.enchantments.Enchantment.class);
        registeredEnchants.when(() -> org.bukkit.enchantments.Enchantment.getByKey(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> java.util.Set.of("protection", "sharpness").contains(((org.bukkit.NamespacedKey) call.getArgument(0)).getKey())
                        ? org.mockito.Mockito.mock(org.bukkit.enchantments.Enchantment.class) : null);
        registeredEffects = org.mockito.Mockito.mockStatic(org.bukkit.potion.PotionEffectType.class);
        registeredEffects.when(() -> org.bukkit.potion.PotionEffectType.getByKey(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> java.util.Set.of("strength", "speed").contains(((org.bukkit.NamespacedKey) call.getArgument(0)).getKey())
                        ? org.mockito.Mockito.mock(org.bukkit.potion.PotionEffectType.class) : null);
    }
    @org.junit.jupiter.api.AfterEach void closeRegistries() { registeredEffects.close(); registeredEnchants.close(); }

    YamlConfiguration config;
    Plugin plugin;
    MockedStatic<PotionEffectsCompat> potions;
    @BeforeEach void setup() {
        config = new YamlConfiguration(); config.set("item-filter.enabled", true);
        plugin = mock(Plugin.class); when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("filter-policy-test"));
        potions = mockStatic(PotionEffectsCompat.class);
        potions.when(() -> PotionEffectsCompat.baseEffects(any())).thenReturn(List.of());
    }
    @AfterEach void close() { potions.close(); }
    ItemStack item(Material type, ItemMeta meta, Map<Enchantment, Integer> enchants) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type); when(item.getAmount()).thenReturn(1);
        when(item.getMaxStackSize()).thenReturn(type.getMaxStackSize());
        when(item.getItemMeta()).thenReturn(meta); when(item.getEnchantments()).thenReturn(enchants);
        when(item.clone()).thenAnswer(i -> item(type, meta, enchants));
        return item;
    }
    Enchantment enchant(String name, int max, boolean applicable) {
        Enchantment enchant = mock(Enchantment.class); when(enchant.getKey()).thenReturn(NamespacedKey.minecraft(name));
        when(enchant.getMaxLevel()).thenReturn(max); when(enchant.canEnchantItem(any())).thenReturn(applicable); return enchant;
    }
    ItemStack filtered(ItemStack item) { return new ItemFilter(plugin).filterItemStack(new ItemStack[]{item})[0]; }
    PotionEffect effect(String name, int ticks, int amplifier) {
        PotionEffectType type = mock(PotionEffectType.class); when(type.getKey()).thenReturn(NamespacedKey.minecraft(name));
        return new PotionEffect(type, ticks, amplifier);
    }
    @Test void metadataLimitsWorkWithoutRequiringKitRoomMaterials() {
        ItemMeta meta = mock(ItemMeta.class); when(meta.isUnbreakable()).thenReturn(true);
        ItemStack original = item(Material.NETHERITE_SWORD, meta, Map.of());
        assertNull(filtered(original)); assertTrue(meta.isUnbreakable());
        config.set("item-filter.overrides.NETHERITE_SWORD.allow-unbreakable", true);
        assertNotNull(filtered(original)); assertTrue(new ItemFilter(plugin).isReady());
    }
    @Test void attributesAndTooltipFlagsAreIndependent() {
        ItemMeta meta = mock(ItemMeta.class); when(meta.hasAttributeModifiers()).thenReturn(true);
        when(meta.getItemFlags()).thenReturn(Set.of(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES));
        ItemStack original = item(Material.NETHERITE_SWORD, meta, Map.of());
        assertNull(filtered(original));
        config.set("item-filter.allow-custom-attributes", true); assertNotNull(filtered(original));
        config.set("item-filter.allow-item-flags", false); assertNull(filtered(original));
    }
    @Test void vanillaEnchantmentCapsCanBeOverriddenPerMaterial() {
        Enchantment protection = enchant("protection", 4, true);
        ItemStack helmet = item(Material.DIAMOND_HELMET, null, Map.of(protection, 10));
        assertNull(filtered(helmet));
        config.set("item-filter.overrides.DIAMOND_HELMET.enchantments.max-levels.protection", 10);
        assertNotNull(filtered(helmet));
        assertNull(filtered(item(Material.LEATHER_HELMET, null, Map.of(protection, 10))));
    }
    @Test void explicitCapsStillApplyWhenOverLevelledEnchantmentsAreAllowed() {
        Enchantment protection = enchant("protection", 4, true);
        ItemStack helmet = item(Material.DIAMOND_HELMET, null, Map.of(protection, 10));
        config.set("item-filter.enchantments.allow-over-levelled", true); assertNotNull(filtered(helmet));
        config.set("item-filter.enchantments.max-levels.protection", 4); assertNull(filtered(helmet));
        config.set("item-filter.enchantments.max-levels.protection", 0); assertNotNull(filtered(helmet));
    }
    @Test void wrongItemTypesAndConflictingEnchantsCanBeRejected() {
        Enchantment sharpness = enchant("sharpness", 5, false);
        ItemStack carrot = item(Material.CARROT, null, Map.of(sharpness, 5));
        assertNull(filtered(carrot));
        config.set("item-filter.enchantments.allow-incompatible", true); assertNotNull(filtered(carrot));
        config.set("item-filter.enchantments.allow-incompatible", false);
        when(sharpness.canEnchantItem(any())).thenReturn(true);
        Enchantment smite = enchant("smite", 5, true); when(sharpness.conflictsWith(smite)).thenReturn(true);
        assertNull(filtered(item(Material.DIAMOND_SWORD, null, Map.of(sharpness, 5, smite, 5))));
    }
    @Test void enchantedBooksCheckStoredLevelsWithoutRejectingTheirValidItemType() {
        Enchantment protection = enchant("protection", 4, false);
        EnchantmentStorageMeta book = mock(EnchantmentStorageMeta.class);
        when(book.getStoredEnchants()).thenReturn(Map.of(protection, 10));
        ItemStack original = item(Material.ENCHANTED_BOOK, book, Map.of()); assertNull(filtered(original));
        when(book.getStoredEnchants()).thenReturn(Map.of(protection, 4)); assertNotNull(filtered(original));
        verify(protection, never()).canEnchantItem(any());
    }
    @Test void customPotionCapsUsePlayerFacingLevelsAndSeconds() {
        config.set("item-filter.potions.max-level", 2); config.set("item-filter.potions.max-duration-seconds", 60);
        PotionMeta meta = mock(PotionMeta.class); ItemStack potion = item(Material.POTION, meta, Map.of());
        doReturn(List.of(effect("speed", 1200, 1))).when(meta).getCustomEffects(); assertNotNull(filtered(potion));
        doReturn(List.of(effect("speed", 1201, 1))).when(meta).getCustomEffects(); assertNull(filtered(potion));
        doReturn(List.of(effect("speed", 1200, 2))).when(meta).getCustomEffects(); assertNull(filtered(potion));
        doReturn(List.of(effect("speed", -1, 0))).when(meta).getCustomEffects(); assertNull(filtered(potion));
        doReturn(List.of(effect("speed", 20, Integer.MAX_VALUE))).when(meta).getCustomEffects(); assertNull(filtered(potion));
    }
    @Test void baseEffectsAndSpecificEffectLimitsApplyToPotionsAndTippedArrows() {
        config.set("item-filter.potions.effects.strength.max-level", 1);
        PotionMeta meta = mock(PotionMeta.class);
        PotionEffect strong = effect("strength", 1800, 1);
        potions.when(() -> PotionEffectsCompat.baseEffects(meta)).thenReturn(List.of(strong));
        assertNull(filtered(item(Material.POTION, meta, Map.of())));
        assertNull(filtered(item(Material.TIPPED_ARROW, meta, Map.of())));
        PotionEffect normal = effect("strength", 1800, 0);
        potions.when(() -> PotionEffectsCompat.baseEffects(meta)).thenReturn(List.of(normal));
        assertNotNull(filtered(item(Material.POTION, meta, Map.of())));
    }
    @Test void potionInspectionFailureRejectsTheItemWithoutChangingItsSource() {
        config.set("item-filter.potions.max-level", 2);
        PotionMeta meta = mock(PotionMeta.class);
        potions.when(() -> PotionEffectsCompat.baseEffects(meta)).thenThrow(new IllegalStateException("fixture API failure"));
        ItemStack source = item(Material.POTION, meta, Map.of()); ItemStack[] input = {source};
        assertNull(new ItemFilter(plugin).filterItemStack(input)[0]); assertSame(source, input[0]);
        verify(source, never()).setType(any()); verify(source, never()).setItemMeta(any());
    }
}
