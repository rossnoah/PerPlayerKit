package dev.noah.perplayerkit.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Pure parsing and matching, shared by location filters and automatic kit selection. */
public final class LocationRules {
    private LocationRules() {}

    public record Selector(String world, String region) {
        public static Selector parse(String value) {
            String[] parts = value.trim().toLowerCase(Locale.ROOT).split(":", -1);
            if (parts.length > 2 || parts[0].isBlank() || (parts.length == 2 && parts[1].isBlank()))
                throw new IllegalArgumentException("Expected a world or world:region, got '" + value + "'");
            if (parts.length == 2 && parts[1].trim().equals("__global__"))
                throw new IllegalArgumentException("Use '" + parts[0].trim() + "' for the whole world, without :__global__");
            return new Selector(parts[0].trim(), parts.length == 2 ? parts[1].trim() : null);
        }
        public boolean inWorld(String name) { return world.equalsIgnoreCase(name); }
    }

    public record Rule(boolean allow, List<Selector> entries) {
        public Rule { entries = List.copyOf(entries); }
        public boolean permits(String world, Supplier<List<String>> regions) {
            // Whole-world entries decide the result without a WorldGuard dependency.
            if (entries.stream().anyMatch(entry -> entry.inWorld(world) && entry.region() == null)) return allow;
            List<String> relevant = entries.stream().filter(entry -> entry.inWorld(world))
                    .map(Selector::region).toList();
            boolean match = !relevant.isEmpty() && regions.get().stream()
                    .anyMatch(id -> relevant.stream().anyMatch(id::equalsIgnoreCase));
            return allow == match;
        }
    }

    public static Map<LocationFeature, Rule> parse(ConfigurationSection config) {
        Map<LocationFeature, Rule> rules = new EnumMap<>(LocationFeature.class);
        if (!config.contains("locations")) return rules;
        ConfigurationSection locations = config.getConfigurationSection("locations");
        if (locations == null) throw new IllegalArgumentException("locations must contain named rules");
        for (String key : locations.getKeys(false)) {
            LocationFeature feature = LocationFeature.fromKey(key);
            String path = "locations." + key;
            ConfigurationSection section = locations.getConfigurationSection(key);
            if (section == null || !Set.of("mode", "entries").containsAll(section.getKeys(false)))
                throw new IllegalArgumentException(path + " must contain only mode and entries");
            Object modeValue = section.get("mode");
            if (!(modeValue instanceof String mode) || !(mode.equals("allow") || mode.equals("deny")))
                throw new IllegalArgumentException(path + ".mode must be allow or deny");
            Object value = section.get("entries");
            if (!(value instanceof List<?> list))
                throw new IllegalArgumentException(path + ".entries must be a list; use [] for an empty list");
            List<Selector> entries = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof String text)) throw new IllegalArgumentException(path + ".entries must contain text");
                try { entries.add(Selector.parse(text)); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException(path + ": " + e.getMessage()); }
            }
            rules.put(feature, new Rule(mode.equals("allow"), entries));
        }
        return rules;
    }
}
