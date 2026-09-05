/*
 * Copyright 2022-2026 Noah Ross
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

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One-time v1/v2-to-v3 migration. Config is committed last; failed writes are rolled back. */
public class ConfigMigrator {
    public static final int CURRENT_VERSION = 3;
    private static final List<String> ACTIONS = List.of("player-repaired", "player-healed", "player-opened-kit-room",
            "player-loaded-private-kit", "player-loaded-public-kit", "player-loaded-enderchest",
            "player-copied-kit", "player-copied-ec", "player-regeared");
    private final Plugin plugin;

    public ConfigMigrator(Plugin plugin) { this.plugin = plugin; }

    public boolean migrate() {
        Path configFile = plugin.getDataFolder().toPath().resolve("config.yml");
        if (!Files.exists(configFile)) return true;
        Map<Path, Path> backups = new LinkedHashMap<>();
        List<Path> written = new ArrayList<>();
        try {
            YamlConfiguration original = ConfigFiles.read(configFile);
            if (original.contains("config-version") && !original.isInt("config-version"))
                throw new IOException("config-version must be an integer; retain the version supplied by the old plugin");
            int version = original.getInt("config-version", 1);
            if (version < 1 || version > CURRENT_VERSION) throw new IOException("Unsupported config-version " + version);
            if (version == CURRENT_VERSION) return true;

            // Build and validate the complete migration before changing any source file.
            YamlConfiguration old = new YamlConfiguration();
            old.loadFromString(original.saveToString());
            Map<Path, YamlConfiguration> changes = new LinkedHashMap<>();
            int messageCount = 0;
            if (version == 1) {
                Map<String, Object> messages = legacyMessages(old);
                messageCount = messages.size();
                if (!messages.isEmpty()) {
                    String code = old.getString("language", "en");
                    code = code == null || code.isBlank() ? "en" : code.toLowerCase(Locale.ROOT);
                    Path folder = plugin.getDataFolder().toPath().toAbsolutePath().resolve("lang").normalize();
                    Path file = folder.resolve(code + ".yml").normalize();
                    if (!file.startsWith(folder)) throw new IOException("language must name a file inside lang/");
                    YamlConfiguration language = Files.exists(file) ? ConfigFiles.read(file) : bundledLanguage(code);
                    messages.forEach(language::set);
                    changes.put(file, language);
                }
                old.set("prefix", null);
                old.set("disabled-command-message", null);
                old.set("motd.message", null);
                old.set("scheduled-broadcast.messages", null);
                for (String action : ACTIONS) old.set("messages." + action + ".message", null);
            }
            YamlConfiguration updated = ConfigSchema.upgrade(old, bundled("config.yml"));
            changes.put(configFile, updated);
            for (Path file : changes.keySet()) {
                if (Files.exists(file)) backups.put(file, ConfigFiles.backup(file));
            }
            for (Map.Entry<Path, YamlConfiguration> change : changes.entrySet()) {
                written.add(change.getKey()); // Include a write that commits but then reports an I/O error.
                ConfigFiles.write(change.getValue(), change.getKey());
            }
            logSummary(version, original, updated, messageCount, backups);
            return true;
        } catch (Exception error) {
            Collections.reverse(written);
            boolean restored = true;
            for (Path file : written) {
                try {
                    Path backup = backups.get(file);
                    if (backup == null) { if (Files.exists(file)) Files.delete(file); }
                    else ConfigFiles.restore(backup, file);
                } catch (IOException rollbackError) {
                    restored = false;
                    plugin.getLogger().severe("Restore " + file + " from " + backups.get(file) + ": " + rollbackError.getMessage());
                }
            }
            plugin.getLogger().severe("Config migration failed. " + (restored ? "Original files retained. " : "Restore the backups listed above. ") + error.getMessage());
            return false;
        }
    }

    private Map<String, Object> legacyMessages(YamlConfiguration old) throws IOException {
        Map<String, Object> messages = new LinkedHashMap<>();
        if (old.contains("prefix")) messages.put("prefix", string(old, "prefix"));
        for (String path : List.of("motd.message", "scheduled-broadcast.messages")) {
            if (!old.contains(path)) continue;
            Object value = old.get(path);
            if (!(value instanceof List<?> list) || list.stream().anyMatch(item -> !(item instanceof String || item instanceof Number || item instanceof Boolean)))
                throw new IOException(path + " must be a list of message strings (use [] for none)");
            // An empty list is an explicit choice, not an absent value.
            messages.put(path, new ArrayList<>(old.getStringList(path)));
        }
        if (old.contains("disabled-command-message")) {
            // This one v1 message used Bukkit's ampersand/section color codes, not MiniMessage.
            String legacy = string(old, "disabled-command-message");
            String translated = org.bukkit.ChatColor.translateAlternateColorCodes('&', legacy);
            messages.put("error.disabled-in-world", MiniMessage.miniMessage().serialize(
                    LegacyComponentSerializer.legacySection().deserialize(translated)));
        }
        for (String action : ACTIONS) {
            String key = "messages." + action + ".message";
            if (old.contains(key)) messages.put("broadcast-messages." + action,
                    string(old, key).replace("%player%", "{player}").replace("%kitname%", "{kitname}"));
        }
        return messages;
    }

    private String string(YamlConfiguration config, String key) throws IOException {
        Object value = config.get(key);
        if (!(value instanceof String || value instanceof Number || value instanceof Boolean))
            throw new IOException(key + " must be text (use \"\" for empty text)");
        return config.getString(key);
    }

    private YamlConfiguration bundledLanguage(String code) throws Exception {
        try (var in = plugin.getResource("lang/" + code + ".yml")) {
            if (in == null) return bundled("lang/en.yml");
            YamlConfiguration language = new YamlConfiguration();
            language.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return language;
        }
    }

    private YamlConfiguration bundled(String resource) throws Exception {
        try (var in = plugin.getResource(resource)) {
            if (in == null) throw new IOException("Bundled " + resource + " is missing");
            YamlConfiguration config = new YamlConfiguration();
            config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return config;
        }
    }

    private void logSummary(int version, YamlConfiguration old, YamlConfiguration updated,
                            int messages, Map<Path, Path> backups) {
        plugin.getLogger().info("Config upgraded from v" + version + " to v3. Saved kit data is unchanged.");
        backups.forEach((file, backup) -> plugin.getLogger().info("Backup for " + file.getFileName() + ": " + backup));
        long moved = ConfigSchema.RENAMED.keySet().stream().filter(old::contains).count();
        plugin.getLogger().info("Moved " + moved + " settings into storage, kits, rekit and broadcasts; preserved " + messages + " legacy messages.");
        String previousStorage = old.getString("storage.type");
        String storage = updated.getString("storage.type");
        if (previousStorage != null && storage.equals("sqlite") && !"sqlite".equals(previousStorage))
            plugin.getLogger().warning("The old storage.type did not select a supported backend. Retaining the SQLite database previously used. Use the storage migration command before selecting another backend.");
        else plugin.getLogger().info("Storage remains " + storage + ".");
        plugin.getLogger().info("Action broadcasts " + (updated.getBoolean("broadcasts.enabled") ? "remain enabled" : "remain disabled")
                + "; notification permissions and custom public kits are preserved.");
        plugin.getLogger().info("Rollback: stop the server, restore these config/language backups and the previous jar. Keep the database backup for recovery; restore it only to undo item changes, since doing so discards later saves.");
    }
}
