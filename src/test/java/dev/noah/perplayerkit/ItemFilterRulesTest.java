package dev.noah.perplayerkit;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ItemFilterRulesTest {
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

    private ItemFilterRules.Settings parse(String body) throws Exception {
        var config = new YamlConfiguration(); config.loadFromString(body); return ItemFilterRules.parse(config);
    }
    @Test void materialOverridesInheritUnspecifiedRules() throws Exception {
        var rules = parse("""
                item-filter:
                  enabled: true
                  allow-unbreakable: false
                  enchantments:
                    max-levels: {protection: 4}
                  potions:
                    max-duration-seconds: 180
                    effects:
                      strength: {max-level: 1}
                  overrides:
                    NETHERITE_SWORD:
                      allow-unbreakable: true
                      enchantments:
                        max-levels: {sharpness: 5}
                    SPLASH_POTION:
                      potions:
                        max-duration-seconds: 60
                """);
        assertFalse(rules.defaults().unbreakable());
        var sword = rules.forMaterial(Material.NETHERITE_SWORD);
        assertTrue(sword.unbreakable()); assertFalse(sword.attributes());
        assertEquals(4, sword.enchantments().get("minecraft:protection"));
        assertEquals(5, sword.enchantments().get("minecraft:sharpness"));
        var potion = rules.forMaterial(Material.SPLASH_POTION);
        assertEquals(60, potion.potionDuration()); assertEquals(1, potion.effects().get("minecraft:strength").level());
    }
    @Test void emptyOverrideMapsKeepInheritedSpecificLimits() throws Exception {
        var rules = parse("""
                item-filter:
                  enchantments: {max-levels: {protection: 4}}
                  overrides:
                    ENCHANTED_BOOK:
                      enchantments: {max-levels: {}}
                """);
        assertEquals(4, rules.forMaterial(Material.ENCHANTED_BOOK).enchantments().get("minecraft:protection"));
        assertFalse(rules.defaults().enchantments().isEmpty());
    }
    @Test void namespacedIdsWithDotsKeepTheirMeaning() throws Exception {
        var rules = parse("""
                item-filter:
                  enchantments:
                    max-levels: {"custom.plugin:power": 5}
                  potions:
                    effects:
                      "custom.plugin:speed": {max-level: 2}
                """);
        assertEquals(5, rules.defaults().enchantments().get("custom.plugin:power"));
        assertEquals(2, rules.defaults().effects().get("custom.plugin:speed").level());
    }
    @ParameterizedTest @ValueSource(strings={
            "item-filter: []", "item-filter: {enabled: perhaps}", "item-filter: {typo: true}",
            "item-filter: {potions: {max-level: -1}}", "item-filter: {potions: {max-duration-seconds: 1.5}}",
            "item-filter: {enchantments: {max-levels: {protection: many}}}",
            "item-filter: {enchantments: {max-levels: {protection: 4, 'minecraft:protection': 5}}}",
            "item-filter: {overrides: {NOT_AN_ITEM: {allow-unbreakable: true}}}",
            "item-filter: {overrides: {STONE: {enabled: true}}}",
            "item-filter: {potions: {effects: {strength: {max-lvel: 1}}}}",
            "item-filter: {enchantments: {max-levels: {protecton: 4}}}",
            "item-filter: {potions: {effects: {strenght: {max-level: 1}}}}"})
    void invalidRulesFailInsteadOfSilentlyDisablingRestrictions(String config) {
        assertThrows(IllegalArgumentException.class, () -> parse(config));
    }
}
