package dev.noah.perplayerkit;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Compiles server-wide rules and optional material overrides once at startup. */
final class ItemFilterRules {
    private static final Set<String> RULE_KEYS = Set.of("allow-unbreakable", "allow-custom-attributes", "allow-item-flags", "enchantments", "potions");
    record EffectLimit(int level, int durationSeconds) {}
    record Rule(boolean unbreakable, boolean attributes, boolean flags, boolean overLevelled, boolean incompatible,
                Map<String, Integer> enchantments, int potionLevel, int potionDuration, Map<String, EffectLimit> effects) {
        boolean checksPotions() { return potionLevel > 0 || potionDuration > 0 || !effects.isEmpty(); }
    }
    record Settings(boolean enabled, boolean kitRoomOnly, boolean imports, boolean saves, Rule defaults, Map<Material, Rule> overrides) {
        Rule forMaterial(Material material) { return overrides.getOrDefault(material, defaults); }
    }
    private static final Rule DEFAULT = new Rule(false, false, true, false, false, Map.of(), 0, 0, Map.of());
    private ItemFilterRules() {}

    static Settings parse(ConfigurationSection config) {
        ConfigurationSection section = child(config, "item-filter");
        if (section == null) return new Settings(false, false, true, true, DEFAULT, Map.of());
        Set<String> allowed = new HashSet<>(RULE_KEYS);
        allowed.addAll(Set.of("enabled", "kit-room-only", "filter-imports", "filter-saves", "overrides"));
        keys(section, allowed);
        Rule defaults = rule(section, DEFAULT);
        Map<Material, Rule> overrides = new EnumMap<>(Material.class);
        ConfigurationSection items = child(section, "overrides");
        if (items != null) for (String name : items.getKeys(false)) {
            Material material = Material.matchMaterial(name.trim());
            if (material == null || !material.isItem() || material.isAir()) throw invalid(items, "Unknown item material: " + name);
            ConfigurationSection entry = child(items, name);
            if (entry == null) throw invalid(items, name + " must contain rules");
            keys(entry, RULE_KEYS);
            if (overrides.put(material, rule(entry, defaults)) != null) throw invalid(items, "Duplicate material: " + name);
        }
        return new Settings(bool(section, "enabled", false), bool(section, "kit-room-only", false),
                bool(section, "filter-imports", true), bool(section, "filter-saves", true), defaults, Map.copyOf(overrides));
    }
    private static Rule rule(ConfigurationSection section, Rule base) {
        ConfigurationSection enchants = child(section, "enchantments");
        Map<String, Integer> levels = new HashMap<>(base.enchantments());
        if (enchants != null) {
            keys(enchants, Set.of("allow-over-levelled", "allow-incompatible", "max-levels"));
            ConfigurationSection limits = child(enchants, "max-levels");
            if (limits != null) {
                Set<String> seen = new HashSet<>();
                for (String name : limits.getKeys(true)) {
                    if (limits.isConfigurationSection(name)) continue;
                    String id = id(limits, name, false);
                    if (!seen.add(id)) throw invalid(limits, "Duplicate enchantment: " + name);
                    levels.put(id, number(limits, name, 0));
                }
            }
        }
        ConfigurationSection potions = child(section, "potions");
        Map<String, EffectLimit> effects = new HashMap<>(base.effects());
        if (potions != null) {
            keys(potions, Set.of("max-level", "max-duration-seconds", "effects"));
            ConfigurationSection limits = child(potions, "effects");
            if (limits != null) {
                Set<String> names = new LinkedHashSet<>();
                for (String path : limits.getKeys(true)) {
                    if (limits.isConfigurationSection(path)) {
                        if (limits.getConfigurationSection(path).getKeys(false).isEmpty()) names.add(path);
                        continue;
                    }
                    int separator = path.lastIndexOf('.');
                    if (separator < 0 || !Set.of("max-level", "max-duration-seconds").contains(path.substring(separator + 1)))
                        throw invalid(limits, "Expected max-level or max-duration-seconds under " + path);
                    names.add(path.substring(0, separator));
                }
                Set<String> seen = new HashSet<>();
                for (String name : names) {
                    ConfigurationSection effect = child(limits, name);
                    keys(effect, Set.of("max-level", "max-duration-seconds"));
                    String id = id(limits, name, true);
                    if (!seen.add(id)) throw invalid(limits, "Duplicate effect: " + name);
                    EffectLimit old = effects.getOrDefault(id, new EffectLimit(0, 0));
                    effects.put(id, new EffectLimit(number(effect, "max-level", old.level()), number(effect, "max-duration-seconds", old.durationSeconds())));
                }
            }
        }
        return new Rule(bool(section, "allow-unbreakable", base.unbreakable()),
                bool(section, "allow-custom-attributes", base.attributes()), bool(section, "allow-item-flags", base.flags()),
                bool(enchants, "allow-over-levelled", base.overLevelled()), bool(enchants, "allow-incompatible", base.incompatible()),
                Map.copyOf(levels), number(potions, "max-level", base.potionLevel()), number(potions, "max-duration-seconds", base.potionDuration()), Map.copyOf(effects));
    }
    private static String id(ConfigurationSection section, String name, boolean effect) {
        NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) throw invalid(section, "Invalid enchantment/effect ID: " + name);
        if (key.getNamespace().equals(NamespacedKey.MINECRAFT)
                && (effect ? PotionEffectType.getByKey(key) == null : Enchantment.getByKey(key) == null))
            throw invalid(section, "Unknown Minecraft " + (effect ? "effect" : "enchantment") + ": " + name);
        return key.toString();
    }
    private static boolean bool(ConfigurationSection section, String key, boolean fallback) {
        if (section == null || !section.contains(key)) return fallback;
        if (!section.isBoolean(key)) throw invalid(section, key + " must be true or false");
        return section.getBoolean(key);
    }
    private static int number(ConfigurationSection section, String key, int fallback) {
        if (section == null || !section.contains(key)) return fallback;
        if (!section.isInt(key) || section.getInt(key) < 0) throw invalid(section, key + " must be a nonnegative whole number");
        return section.getInt(key);
    }
    private static ConfigurationSection child(ConfigurationSection section, String key) {
        if (section == null || !section.contains(key)) return null;
        ConfigurationSection child = section.getConfigurationSection(key);
        if (child == null) throw invalid(section, key + " must contain settings");
        return child;
    }
    private static void keys(ConfigurationSection section, Set<String> allowed) {
        for (String key : section.getKeys(false)) if (!allowed.contains(key)) throw invalid(section, "Unknown setting: " + key);
    }
    private static IllegalArgumentException invalid(ConfigurationSection section, String message) {
        return new IllegalArgumentException(section.getCurrentPath() + ": " + message);
    }
}
