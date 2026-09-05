package dev.noah.perplayerkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Map;
import java.util.Set;

/** The v2-to-v3 path map is also the migration's complete list of renamed settings. */
public final class ConfigSchema {
    private ConfigSchema() {}
    public static final Map<String, String> RENAMED = Map.ofEntries(
        Map.entry("max-kits", "kits.max-slots"),
        Map.entry("disabled-command-worlds", "restrictions.disabled-worlds"),
        Map.entry("motd.delay", "motd.delay-seconds"),
        Map.entry("use-display-name", "broadcasts.use-display-name"),
        Map.entry("backup.enabled", "storage.backup.enabled"),
        Map.entry("feature.set-health-on-kit-load", "kits.load.heal"),
        Map.entry("feature.set-hunger-on-kit-load", "kits.load.feed"),
        Map.entry("feature.set-saturation-on-kit-load", "kits.load.saturate"),
        Map.entry("feature.remove-potion-effects-on-kit-load", "kits.load.clear-effects"),
        Map.entry("feature.heal-on-enderchest-load", "enderchests.load.heal"),
        Map.entry("feature.feed-on-enderchest-load", "enderchests.load.feed"),
        Map.entry("feature.set-saturation-on-enderchest-load", "enderchests.load.saturate"),
        Map.entry("feature.remove-potion-effects-on-enderchest-load", "enderchests.load.clear-effects"),
        Map.entry("feature.heal-remove-effects", "heal.clear-effects"),
        Map.entry("feature.rekit-on-respawn", "rekit.respawn.enabled"),
        Map.entry("feature.rekit-on-respawn-delay", "rekit.respawn.delay-ticks"),
        Map.entry("feature.send-update-message-on-join", "updates.notify-admins-on-join"),
        Map.entry("feature.old-death-drops", "death.condensed-drops"),
        Map.entry("regear.rg-mode", "regear.modes.rg"),
        Map.entry("regear.regear-mode", "regear.modes.regear"),
        Map.entry("regear.command-cooldown", "regear.command-cooldown-seconds"),
        Map.entry("regear.damage-timer", "regear.damage-cooldown-seconds"),
        Map.entry("scheduled-broadcast.enabled", "broadcasts.scheduled.enabled"),
        Map.entry("scheduled-broadcast.period", "broadcasts.scheduled.period-seconds"),
        Map.entry("sounds.open_gui", "sounds.open-gui"),
        Map.entry("sounds.close_gui", "sounds.close-gui"),
        Map.entry("mysql.host", "storage.mysql.host"),
        Map.entry("mysql.port", "storage.mysql.port"),
        Map.entry("mysql.dbname", "storage.mysql.dbname"),
        Map.entry("mysql.username", "storage.mysql.username"),
        Map.entry("mysql.password", "storage.mysql.password"),
        Map.entry("mysql.useSSL", "storage.mysql.use-ssl"),
        Map.entry("mysql.maximumPoolSize", "storage.mysql.maximum-pool-size"),
        Map.entry("postgresql.host", "storage.postgresql.host"),
        Map.entry("postgresql.port", "storage.postgresql.port"),
        Map.entry("postgresql.dbname", "storage.postgresql.dbname"),
        Map.entry("postgresql.username", "storage.postgresql.username"),
        Map.entry("postgresql.password", "storage.postgresql.password"),
        Map.entry("postgresql.useSSL", "storage.postgresql.use-ssl"),
        Map.entry("postgresql.maximumPoolSize", "storage.postgresql.maximum-pool-size"),
        Map.entry("redis.host", "storage.redis.host"),
        Map.entry("redis.port", "storage.redis.port"),
        Map.entry("redis.password", "storage.redis.password")
    );
    private static final Set<String> COMBINED = Set.of("feature.broadcast-on-player-action",
            "feature.broadcast-kit-messages", "messages.disable-kit-messages");

    public static String legacyStorageType(String configured, int version) {
        if (configured == null) return "sqlite"; // The old default merger supplied SQLite when absent.
        return switch (configured) {
            case "sqlite", "mysql", "redis" -> configured;
            case "yml", "yaml" -> "yaml";
            case "postgres", "postgresql" -> version >= 2 ? "postgresql" : "sqlite";
            default -> "sqlite";
        };
    }

    public static boolean hasLegacyLocations(FileConfiguration config) {
        return config.contains("restrictions.disabled-worlds") || config.contains("rekit.kill.world-whitelist")
                || config.contains("rekit.kill.world-blacklist");
    }

    public static boolean hasLegacyItemFilter(FileConfiguration config) {
        return config.contains("anti-exploit.only-allow-kitroom-items") || config.contains("anti-exploit.import-filter");
    }

    public static void upgradeItemFilter(FileConfiguration config) {
        if (!hasLegacyItemFilter(config)) return;
        if (config.contains("item-filter")) throw new IllegalArgumentException("Both legacy anti-exploit filter settings and item-filter are configured. Remove one before restarting.");
        boolean enabled = config.getBoolean("anti-exploit.only-allow-kitroom-items", false);
        config.set("item-filter.enabled", enabled);
        config.set("item-filter.kit-room-only", enabled);
        config.set("item-filter.filter-imports", config.getBoolean("anti-exploit.import-filter", false));
        config.set("item-filter.filter-saves", false); // Existing definitions were filtered on use, not on save.
        config.set("item-filter.allow-unbreakable", true);
        config.set("item-filter.allow-custom-attributes", false);
        config.set("item-filter.allow-item-flags", false);
        config.set("item-filter.enchantments.allow-over-levelled", false);
        config.set("item-filter.enchantments.allow-incompatible", true);
        config.set("anti-exploit.only-allow-kitroom-items", null);
        config.set("anti-exploit.import-filter", null);
    }

    /** Also upgrades configs written by earlier v3 development builds. */
    public static void upgradeLocations(FileConfiguration config) {
        if (config.contains("restrictions.disabled-worlds")) {
            setLocationRule(config, "global", "deny", config.getStringList("restrictions.disabled-worlds"));
            config.set("restrictions.disabled-worlds", null);
            if (config.getConfigurationSection("restrictions").getKeys(false).isEmpty()) config.set("restrictions", null);
        }
        if (config.contains("rekit.kill.world-whitelist") || config.contains("rekit.kill.world-blacklist")) {
            var allow = config.getStringList("rekit.kill.world-whitelist");
            var deny = config.getStringList("rekit.kill.world-blacklist");
            setLocationRule(config, "rekit-kill", allow.isEmpty() ? "deny" : "allow", allow.isEmpty() ? deny : allow);
            config.set("rekit.kill.world-whitelist", null);
            config.set("rekit.kill.world-blacklist", null);
        }
    }

    private static void setLocationRule(FileConfiguration config, String feature, String mode, java.util.List<String> entries) {
        String path = "locations." + feature;
        if (config.contains(path)) throw new IllegalArgumentException("Both legacy world settings and " + path
                + " are configured. Remove one before restarting; neither rule has been overwritten.");
        config.set(path + ".mode", mode);
        config.set(path + ".entries", entries);
    }

    public static YamlConfiguration upgrade(FileConfiguration old, YamlConfiguration template) {
        // Location defaults are merged after legacy rules have been converted.
        template.set("locations", null);
        template.set("item-filter", null);
        // These collections belong to the owner. Never repopulate removed public kits.
        if (old.contains("publickits")) template.set("publickits", old.get("publickits"));
        for (String key : old.getKeys(true)) {
            if (old.isConfigurationSection(key) || key.equals("config-version") || COMBINED.contains(key)
                    || key.equals("publickits") || key.startsWith("publickits.")) continue;
            String target = RENAMED.getOrDefault(key, key);
            if (key.equals("feature.rekit-on-kill")) target = "rekit.kill.enabled";
            else if (key.startsWith("feature.rekit-on-kill.")) target = key.replace("feature.rekit-on-kill.", "rekit.kill.");
            else if (key.startsWith("messages.")) target = "broadcasts.actions." + key.substring("messages.".length());
            template.set(target, old.get(key));
        }
        template.set("broadcasts.enabled", old.getBoolean("feature.broadcast-on-player-action", true)
                && !old.getBoolean("messages.disable-kit-messages", false));
        if (!old.getBoolean("feature.broadcast-kit-messages", true)) {
            for (String action : Set.of("player-loaded-private-kit", "player-loaded-public-kit", "player-loaded-enderchest"))
                template.set("broadcasts.actions." + action + ".enabled", false);
        }
        // The old selector was case-sensitive and selected SQLite for anything else.
        // Preserve the backend actually used, rather than connecting to an empty new database.
        template.set("storage.type", legacyStorageType(old.getString("storage.type"), old.getInt("config-version", 1)));
        upgradeLocations(template);
        upgradeItemFilter(template);
        template.set("config-version", 3);
        return template;
    }
}
