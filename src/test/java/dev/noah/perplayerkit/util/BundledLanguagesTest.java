package dev.noah.perplayerkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class BundledLanguagesTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[a-zA-Z][a-zA-Z0-9_-]*}");
    static Stream<String> languages() { return Lang.BUNDLED_LANGS.stream(); }
    private YamlConfiguration read(String code) throws Exception {
        var resource = getClass().getResourceAsStream("/lang/" + code + ".yml");
        assertNotNull(resource, code);
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            YamlConfiguration result = new YamlConfiguration(); result.load(reader); return result;
        }
    }
    private List<String> lines(Object value) {
        if (value instanceof List<?> list) return list.stream().map(String.class::cast).toList();
        return List.of((String) value);
    }
    private Set<String> placeholders(List<String> lines) {
        Set<String> result = new TreeSet<>();
        for (String line : lines) PLACEHOLDER.matcher(line).results().forEach(match -> result.add(match.group()));
        return result;
    }
    private Set<String> clicks(List<String> lines) {
        Set<String> result = new TreeSet<>();
        for (String line : lines) collectClicks(MiniMessage.miniMessage().deserialize(line), result);
        return result;
    }
    private void collectClicks(Component component, Set<String> result) {
        if (component.clickEvent() != null) result.add(component.clickEvent().action() + ":" + component.clickEvent().value());
        component.children().forEach(child -> collectClicks(child, result));
    }
    @ParameterizedTest @MethodSource("languages")
    void everyBundledLanguageHasAllMessagesWithTheirPlaceholdersAndClickActions(String code) throws Exception {
        YamlConfiguration english = read("en"), translated = read(code);
        for (String key : english.getKeys(true)) {
            if (english.isConfigurationSection(key)) continue;
            assertTrue(translated.contains(key), code + " is missing " + key);
            Object original = english.get(key), value = translated.get(key);
            assertEquals(original instanceof List<?>, value instanceof List<?>, code + " changed the type of " + key);
            List<String> originalLines = lines(original), translatedLines = lines(value);
            assertEquals(placeholders(originalLines), placeholders(translatedLines), code + " changed placeholders in " + key);
            assertEquals(clicks(originalLines), clicks(translatedLines), code + " changed click actions in " + key);
        }
    }
}
