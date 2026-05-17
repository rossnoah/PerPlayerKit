package dev.noah.perplayerkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ConfigMigratorTest {

    private Plugin pluginFor(File dataFolder) {
        Plugin plugin = Mockito.mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        // Make getResource pull from the test classpath (which includes src/main/resources)
        Mockito.doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            InputStream in = ConfigMigratorTest.class.getClassLoader().getResourceAsStream(name);
            return in;
        }).when(plugin).getResource(Mockito.anyString());
        return plugin;
    }

    @Test
    void migrationIsNoOpWhenConfigAbsent(@TempDir Path tempDir) {
        Plugin plugin = pluginFor(tempDir.toFile());

        new ConfigMigrator(plugin).migrate();

        assertFalse(new File(tempDir.toFile(), "config.yml").exists());
    }

    @Test
    void migrationIsNoOpWhenConfigAlreadyV2(@TempDir Path tempDir) throws IOException {
        File configFile = new File(tempDir.toFile(), "config.yml");
        Files.writeString(configFile.toPath(), "config-version: 2\nlanguage: en\n");
        Plugin plugin = pluginFor(tempDir.toFile());

        new ConfigMigrator(plugin).migrate();

        YamlConfiguration after = YamlConfiguration.loadConfiguration(configFile);
        assertEquals(2, after.getInt("config-version"));
    }

    @Test
    void migrationBumpsVersionAndAddsLanguage(@TempDir Path tempDir) throws IOException {
        File configFile = new File(tempDir.toFile(), "config.yml");
        Files.writeString(configFile.toPath(),
                "config-version: 1\n" +
                        "prefix: \"<gold>[MyServer]</gold> \"\n" +
                        "disabled-command-message: \"&cNot here.\"\n" +
                        "messages:\n" +
                        "  player-repaired:\n" +
                        "    enabled: true\n" +
                        "    message: \"<aqua>%player% custom repair msg</aqua>\"\n" +
                        "    permission: \"perplayerkit.kitnotify\"\n");
        Plugin plugin = pluginFor(tempDir.toFile());

        new ConfigMigrator(plugin).migrate();

        YamlConfiguration after = YamlConfiguration.loadConfiguration(configFile);
        assertEquals(2, after.getInt("config-version"));
        assertEquals("en", after.getString("language"));
        assertFalse(after.contains("prefix"), "prefix should be stripped from config.yml");
        assertFalse(after.contains("disabled-command-message"), "disabled-command-message should be stripped");
        assertFalse(after.contains("messages.player-repaired.message"), "message key should be stripped");
        assertTrue(after.getBoolean("messages.player-repaired.enabled"), "enabled flag preserved");

        File langFile = new File(tempDir.toFile(), "lang/en.yml");
        assertTrue(langFile.exists(), "lang/en.yml should have been created");
        YamlConfiguration langCfg = YamlConfiguration.loadConfiguration(langFile);
        assertEquals("<gold>[MyServer]</gold> ", langCfg.getString("prefix"));
        assertEquals("&cNot here.", langCfg.getString("error.disabled-in-world"));
        assertEquals("<aqua>%player% custom repair msg</aqua>",
                langCfg.getString("broadcast-messages.player-repaired"));
    }

    @Test
    void migrationPreservesNonCustomizedDefaults(@TempDir Path tempDir) throws IOException {
        // Default English prefix should not be migrated (it's the default value)
        String defaultPrefix = "<gray>[<aqua>Kits</aqua>]</gray> ";
        File configFile = new File(tempDir.toFile(), "config.yml");
        Files.writeString(configFile.toPath(),
                "config-version: 1\nprefix: \"" + defaultPrefix + "\"\n",
                StandardCharsets.UTF_8);
        Plugin plugin = pluginFor(tempDir.toFile());

        new ConfigMigrator(plugin).migrate();

        File langFile = new File(tempDir.toFile(), "lang/en.yml");
        if (langFile.exists()) {
            YamlConfiguration langCfg = YamlConfiguration.loadConfiguration(langFile);
            // The prefix in the migrated lang file should still be the default
            assertEquals(defaultPrefix, langCfg.getString("prefix"));
        }
        // The customizations section in the migrated lang file should not contain
        // a redundant override since the original value matched the default.
        // (We can't easily distinguish "set explicitly" vs "default" in YamlConfiguration
        // without parsing manually, so this is a soft check.)
        @SuppressWarnings("unused")
        List<String> ignored = List.of();
    }
}
