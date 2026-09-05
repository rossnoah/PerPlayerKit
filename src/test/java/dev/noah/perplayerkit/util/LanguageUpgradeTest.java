package dev.noah.perplayerkit.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LanguageUpgradeTest {
    @TempDir Path folder;
    @AfterEach void reset() { Lang.resetForTesting(); }
    private Lang load(String code) { return load(code, null); }
    private Lang load(String code, String bundledOverride) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("language-upgrade-test"));
        YamlConfiguration config = new YamlConfiguration(); config.set("language", code);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getResource(anyString())).thenAnswer(call -> {
            String name = call.getArgument(0);
            if (bundledOverride != null && name.equals("lang/es.yml"))
                return new java.io.ByteArrayInputStream(bundledOverride.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return getClass().getResourceAsStream("/" + name);
        });
        try (var audiences = mockStatic(BukkitAudiences.class)) {
            audiences.when(() -> BukkitAudiences.create(plugin)).thenReturn(mock(BukkitAudiences.class));
            return new Lang(plugin);
        }
    }
    @Test void existingTranslationUsesNewBundledMessagesAndPreservesCustomTextAndEmptyValues() throws Exception {
        Path file = folder.resolve("lang/es.yml"); Files.createDirectories(file.getParent());
        String custom = "prefix: ''\nerror:\n  kit-not-found: 'Mi texto personalizado'\n  no-permission: ''\nwelcome:\n  first-admin: []\n";
        Files.writeString(file, custom);
        Lang lang = load("es");
        assertEquals("<red>Esta acción está desactivada en tu ubicación.", lang.raw("error.disabled-at-location"));
        assertEquals("Mi texto personalizado", lang.raw("error.kit-not-found"));
        assertEquals("", lang.raw("error.no-permission"));
        assertEquals("", lang.raw("prefix"));
        assertEquals(List.of(), lang.rawList("welcome.first-admin"));
        assertTrue(lang.rawList("welcome.setup-reminder").get(0).contains("sigue vacía"));
        assertEquals(custom, Files.readString(file));
    }
    @Test void customLanguageWithoutBundledTranslationFallsBackToEnglish() throws Exception {
        Path file = folder.resolve("lang/custom.yml"); Files.createDirectories(file.getParent());
        Files.writeString(file, "prefix: 'Custom '\n");
        Lang lang = load("custom");
        assertEquals("Custom ", lang.raw("prefix"));
        assertEquals("<red>This action is disabled at your location.", lang.raw("error.disabled-at-location"));
    }
    @ParameterizedTest @ValueSource(strings={"es", "ES"})
    void freshInstallUsesTheSelectedTranslation(String code) {
        assertEquals("<red>No tienes permiso para hacer eso.", load(code).raw("error.no-permission"));
    }
    @Test void languageCodesAreIndependentOfTheServerLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Lang lang = load("FI");
            assertEquals("fi", lang.getActiveLanguage());
            assertEquals("<red>Sinulla ei ole oikeutta tehdä sitä.", lang.raw("error.no-permission"));
        } finally { Locale.setDefault(original); }
    }

    @Test void missingBundledTranslationStillFallsBackToEnglish() throws Exception {
        Path file = folder.resolve("lang/es.yml"); Files.createDirectories(file.getParent());
        Files.writeString(file, "prefix: 'Personalizado '\n");
        Lang lang = load("es", "error:\n  no-permission: 'Sin permiso'\n");
        assertEquals("Personalizado ", lang.raw("prefix"));
        assertEquals("Sin permiso", lang.raw("error.no-permission"));
        assertEquals("<red>This action is disabled at your location.", lang.raw("error.disabled-at-location"));
        assertTrue(lang.rawList("welcome.setup-reminder").get(0).contains("still empty"));
    }
}
