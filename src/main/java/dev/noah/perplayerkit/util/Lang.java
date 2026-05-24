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
package dev.noah.perplayerkit.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Lang {

    public static final List<String> BUNDLED_LANGS = Arrays.asList("en", "zh", "es", "pt", "fr", "uk", "de", "sv", "da", "fi", "it", "pl", "ro", "nl");
    private static final String DEFAULT_LANG = "en";

    private static Lang instance;

    private final Plugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final BukkitAudiences audience;
    private final YamlConfiguration lang;
    private final YamlConfiguration fallback;
    private final String activeLang;

    public Lang(Plugin plugin) {
        this.plugin = plugin;
        this.audience = BukkitAudiences.create(plugin);
        extractBundledLangFiles();

        String configured = plugin.getConfig().getString("language", DEFAULT_LANG);
        if (configured == null || configured.isBlank()) {
            configured = DEFAULT_LANG;
        }
        this.activeLang = configured.toLowerCase();

        this.fallback = loadFromJar(DEFAULT_LANG);
        this.lang = loadActive(activeLang);

        instance = this;
        plugin.getLogger().info("Loaded language: " + activeLang);
    }

    /**
     * Test-only constructor: skips file extraction and Bukkit audience setup.
     * Sends fall back to {@link CommandSender#sendMessage(String)} with legacy
     * color codes so tests can verify with simple Mockito matchers.
     */
    Lang(YamlConfiguration langConfig) {
        this.plugin = null;
        this.audience = null;
        this.activeLang = DEFAULT_LANG;
        this.fallback = langConfig;
        this.lang = langConfig;
    }

    public static Lang get() {
        if (instance == null) {
            throw new IllegalStateException("Lang has not been initialized");
        }
        return instance;
    }

    /**
     * Install a test-mode Lang instance backed by the bundled en.yml jar resource.
     * Intended for unit tests; should not be called from production code.
     */
    public static void installForTesting() {
        try (InputStream in = Lang.class.getResourceAsStream("/lang/en.yml")) {
            YamlConfiguration cfg = (in == null)
                    ? new YamlConfiguration()
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            instance = new Lang(cfg);
        } catch (IOException e) {
            instance = new Lang(new YamlConfiguration());
        }
    }

    /** Reset for test isolation. */
    public static void resetForTesting() {
        instance = null;
    }

    public String getActiveLanguage() {
        return activeLang;
    }

    private void extractBundledLangFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create lang directory");
        }
        for (String code : BUNDLED_LANGS) {
            String name = code + ".yml";
            File outFile = new File(langDir, name);
            if (outFile.exists()) {
                continue;
            }
            try (InputStream in = plugin.getResource("lang/" + name)) {
                if (in == null) {
                    continue;
                }
                java.nio.file.Files.copy(in, outFile.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to extract lang file " + name + ": " + e.getMessage());
            }
        }
    }

    private YamlConfiguration loadActive(String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file lang/" + code + ".yml not found; falling back to " + DEFAULT_LANG);
            return loadFromJar(DEFAULT_LANG);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.setDefaults(loadFromJar(DEFAULT_LANG));
        return cfg;
    }

    private YamlConfiguration loadFromJar(String code) {
        InputStream in = plugin.getResource("lang/" + code + ".yml");
        if (in == null) {
            plugin.getLogger().severe("Bundled lang file lang/" + code + ".yml is missing from the jar");
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read bundled lang file " + code + ".yml: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    public String raw(String key) {
        String value = lang.getString(key);
        if (value == null) {
            value = fallback.getString(key);
        }
        if (value == null) {
            if (plugin != null) {
                plugin.getLogger().warning("Missing language key: " + key);
            }
            return key;
        }
        return value;
    }

    public String raw(String key, Map<String, String> placeholders) {
        return applyPlaceholders(raw(key), placeholders);
    }

    public String raw(String key, String... pairs) {
        return applyPlaceholders(raw(key), pairsToMap(pairs));
    }

    public List<String> rawList(String key) {
        if (lang.contains(key)) {
            return lang.getStringList(key);
        }
        return fallback.getStringList(key);
    }

    public Component component(String key) {
        return mm.deserialize(raw(key));
    }

    public Component component(String key, Map<String, String> placeholders) {
        return mm.deserialize(raw(key, placeholders));
    }

    public Component component(String key, String... pairs) {
        return mm.deserialize(raw(key, pairs));
    }

    public Component prefix() {
        return mm.deserialize(raw("prefix"));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, (Map<String, String>) null);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        Component msg = prefix().append(component(key, placeholders == null ? Map.of() : placeholders));
        deliver(sender, msg);
    }

    public void send(CommandSender sender, String key, String... pairs) {
        send(sender, key, pairsToMap(pairs));
    }

    public void sendNoPrefix(CommandSender sender, String key) {
        sendNoPrefix(sender, key, (Map<String, String>) null);
    }

    public void sendNoPrefix(CommandSender sender, String key, Map<String, String> placeholders) {
        Component msg = component(key, placeholders == null ? Map.of() : placeholders);
        deliver(sender, msg);
    }

    private void deliver(CommandSender sender, Component msg) {
        if (audience != null) {
            if (sender instanceof Player p) {
                audience.player(p).sendMessage(msg);
            } else {
                audience.sender(sender).sendMessage(msg);
            }
        } else {
            sender.sendMessage(LegacyComponentSerializer.legacySection().serialize(msg));
        }
    }

    public void sendNoPrefix(CommandSender sender, String key, String... pairs) {
        sendNoPrefix(sender, key, pairsToMap(pairs));
    }

    public String legacy(String key) {
        return LegacyComponentSerializer.legacySection().serialize(component(key));
    }

    public String legacy(String key, String... pairs) {
        return LegacyComponentSerializer.legacySection().serialize(component(key, pairs));
    }

    /**
     * Splits a template like "Kit: {slot}" around the named placeholder. Returns
     * {@code [prefix, suffix]} — useful for both rendering the template and parsing
     * back the placeholder value from a rendered title.
     */
    public String[] splitTemplate(String key, String placeholder) {
        String tmpl = raw(key);
        String token = "{" + placeholder + "}";
        int idx = tmpl.indexOf(token);
        if (idx < 0) {
            return new String[]{tmpl, ""};
        }
        return new String[]{tmpl.substring(0, idx), tmpl.substring(idx + token.length())};
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }
        String out = input;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static Map<String, String> pairsToMap(String... pairs) {
        if (pairs == null || pairs.length == 0) {
            return Map.of();
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("placeholder pairs must be key,value pairs");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    public static Map<String, String> placeholders(String... pairs) {
        return pairsToMap(pairs);
    }

    public Map<String, String> ph(String... pairs) {
        return pairsToMap(pairs);
    }

    static Map<String, String> applyPlaceholdersForTest(String input, String... pairs) {
        Map<String, String> map = pairsToMap(pairs);
        Map<String, String> out = new HashMap<>(map);
        out.put("__result__", applyPlaceholders(input, map));
        return out;
    }
}
