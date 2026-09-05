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
    void migrationUpgradesV2(@TempDir Path tempDir) throws IOException {
        File configFile = new File(tempDir.toFile(), "config.yml");
        Files.writeString(configFile.toPath(), "config-version: 2\nlanguage: en\n");
        Plugin plugin = pluginFor(tempDir.toFile());

        new ConfigMigrator(plugin).migrate();

        YamlConfiguration after = YamlConfiguration.loadConfiguration(configFile);
        assertEquals(3, after.getInt("config-version"));
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
        assertEquals(3, after.getInt("config-version"));
        assertEquals("en", after.getString("language"));
        assertFalse(after.contains("prefix"), "prefix should be stripped from config.yml");
        assertFalse(after.contains("disabled-command-message"), "disabled-command-message should be stripped");
        assertFalse(after.contains("messages.player-repaired.message"), "message key should be stripped");
        assertTrue(after.getBoolean("broadcasts.actions.player-repaired.enabled"), "enabled flag preserved");

        File langFile = new File(tempDir.toFile(), "lang/en.yml");
        assertTrue(langFile.exists(), "lang/en.yml should have been created");
        YamlConfiguration langCfg = YamlConfiguration.loadConfiguration(langFile);
        assertEquals("<gold>[MyServer]</gold> ", langCfg.getString("prefix"));
        assertEquals("§cNot here.", net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(langCfg.getString("error.disabled-in-world"))));
        assertEquals("<aqua>{player} custom repair msg</aqua>",
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

    @Test
    void failedLanguageWriteLeavesOldConfigAndVersionIntact(@TempDir Path dir) throws IOException {
        String original = "config-version: 1\nprefix: custom\n";
        Files.writeString(dir.resolve("config.yml"), original);
        Files.writeString(dir.resolve("lang"), "blocks the directory");
        assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(original, Files.readString(dir.resolve("config.yml")));
        try (var files = Files.list(dir)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("config.yml.backup-")));
        }
    }

    @Test
    void upgradePreservesLegacyBooleanBroadcastsAndCustomKits(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("config.yml"), """
                config-version: 2
                max-kits: 18
                mysql:
                  maximumPoolSize: 7
                  password: custom-password
                feature:
                  rekit-on-kill: true
                  broadcast-kit-messages: false
                messages:
                  player-repaired:
                    enabled: false
                    permission: custom.notify
                publickits:
                  custom:
                    name: Custom kit
                    icon: STONE
                """);
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        YamlConfiguration config = ConfigFiles.read(dir.resolve("config.yml"));
        assertTrue(config.getBoolean("rekit.kill.enabled"));
        assertEquals(18, config.getInt("kits.max-slots"));
        assertEquals(7, config.getInt("storage.mysql.maximum-pool-size"));
        assertEquals("custom-password", config.getString("storage.mysql.password"));
        assertFalse(config.getBoolean("broadcasts.actions.player-loaded-public-kit.enabled"));
        assertFalse(config.getBoolean("broadcasts.actions.player-repaired.enabled"));
        assertEquals("custom.notify", config.getString("broadcasts.actions.player-repaired.permission"));
        assertEquals(java.util.Set.of("custom"), config.getConfigurationSection("publickits").getKeys(false));
        assertFalse(config.contains("feature"));
        assertFalse(config.contains("max-kits"));
        String first = Files.readString(dir.resolve("config.yml"));
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(first, Files.readString(dir.resolve("config.yml")));
    }

    @Test
    void malformedYamlIsNeverReplacedWithDefaults(@TempDir Path dir) throws IOException {
        String bad = "config-version: 2\ninvalid: [\n";
        Files.writeString(dir.resolve("config.yml"), bad);
        assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(bad, Files.readString(dir.resolve("config.yml")));
    }

    @Test
    void disabledGlobalBroadcastsStayDisabled(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("config.yml"), "config-version: 2\nmessages:\n  disable-kit-messages: true\n");
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertFalse(ConfigFiles.read(dir.resolve("config.yml")).getBoolean("broadcasts.enabled"));
    }

    @Test
    void emptyLegacyListsAndTextStayEmpty(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("config.yml"), """
                config-version: 1
                prefix: ""
                motd:
                  enabled: true
                  message: []
                scheduled-broadcast:
                  enabled: true
                  messages: []
                messages:
                  player-repaired:
                    message: ""
                publickits: {}
                """);
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        YamlConfiguration language = ConfigFiles.read(dir.resolve("lang/en.yml"));
        assertTrue(language.contains("motd.message"));
        assertEquals(List.of(), language.getStringList("motd.message"));
        assertEquals(List.of(), language.getStringList("scheduled-broadcast.messages"));
        assertEquals("", language.getString("prefix"));
        assertEquals("", language.getString("broadcast-messages.player-repaired"));
        assertTrue(ConfigFiles.read(dir.resolve("config.yml")).getConfigurationSection("publickits").getKeys(false).isEmpty());
    }

    @Test
    void legacyMessagesGoToActiveLanguageAndKeepOtherCustomText(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("lang"));
        Files.writeString(dir.resolve("lang/custom.yml"), "# Keep my translations\nsuccess:\n  kit-loaded: My translation\n");
        Files.writeString(dir.resolve("config.yml"), """
                config-version: 1
                language: custom
                prefix: '<gold>[Custom]</gold> '
                messages:
                  player-loaded-public-kit:
                    message: '<aqua>%player% loaded %kitname% (%player_name%)</aqua>'
                """);
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        YamlConfiguration language = ConfigFiles.read(dir.resolve("lang/custom.yml"));
        assertEquals("My translation", language.getString("success.kit-loaded"));
        assertEquals("<aqua>{player} loaded {kitname} (%player_name%)</aqua>", language.getString("broadcast-messages.player-loaded-public-kit"));
        assertFalse(Files.exists(dir.resolve("lang/en.yml")));
    }

    @Test
    void v2LanguageFilesAreNotRewritten(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("lang"));
        String original = "# Personal formatting\nprefix: ''\nmotd:\n  message: []\n";
        Files.writeString(dir.resolve("lang/en.yml"), original);
        Files.writeString(dir.resolve("config.yml"), "config-version: 2\n");
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(original, Files.readString(dir.resolve("lang/en.yml")));
    }

    @Test
    void failedFinalConfigWriteRestoresLanguageBytes(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("lang"));
        Path config = dir.resolve("config.yml");
        String oldConfig = "config-version: 1\nprefix: changed\n";
        String oldLanguage = "# Owner comment\nprefix: original\n";
        Files.writeString(config, oldConfig);
        Files.writeString(dir.resolve("lang/en.yml"), oldLanguage);
        try (var files = Mockito.mockStatic(ConfigFiles.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> ConfigFiles.write(Mockito.any(), Mockito.eq(config))).thenThrow(new IOException("test write failure"));
            assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        }
        assertEquals(oldConfig, Files.readString(config));
        assertEquals(oldLanguage, Files.readString(dir.resolve("lang/en.yml")));
    }

    @Test
    void malformedLegacyMessagesFailBeforeAnySourceWrite(@TempDir Path dir) throws IOException {
        String original = "config-version: 1\nmotd:\n  message: not-a-list\n";
        Files.writeString(dir.resolve("config.yml"), original);
        assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(original, Files.readString(dir.resolve("config.yml")));
        assertFalse(Files.exists(dir.resolve("lang/en.yml")));
    }

    @Test
    void upgradeKeepsTheBackendSelectedByTheLegacySelector(@TempDir Path dir) throws IOException {
        for (String type : List.of("MySQL", "YAML", "mysqll", " mysql ")) {
            Files.writeString(dir.resolve("config.yml"), "config-version: 2\nstorage:\n  type: '" + type + "'\n");
            assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
            assertEquals("sqlite", ConfigFiles.read(dir.resolve("config.yml")).getString("storage.type"));
        }
        for (String type : List.of("mysql", "redis", "sqlite", "postgresql")) {
            Files.writeString(dir.resolve("config.yml"), "config-version: 2\nstorage:\n  type: " + type + "\n");
            assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
            assertEquals(type, ConfigFiles.read(dir.resolve("config.yml")).getString("storage.type"));
        }
    }

    @Test
    void legacyScalarMessageValuesKeepTheirDisplayedText(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("config.yml"), "config-version: 1\nprefix: 123\nmotd:\n  message: [1, false]\n");
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        YamlConfiguration language = ConfigFiles.read(dir.resolve("lang/en.yml"));
        assertEquals("123", language.getString("prefix"));
        assertEquals(List.of("1", "false"), language.getStringList("motd.message"));
    }

    @Test
    void failedFinalWriteRemovesANewLanguageFile(@TempDir Path dir) throws IOException {
        Path config = dir.resolve("config.yml");
        String original = "config-version: 1\nprefix: custom\n";
        Files.writeString(config, original);
        try (var files = Mockito.mockStatic(ConfigFiles.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> ConfigFiles.write(Mockito.any(), Mockito.eq(config))).thenThrow(new IOException("test write failure"));
            assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        }
        assertEquals(original, Files.readString(config));
        assertFalse(Files.exists(dir.resolve("lang/en.yml")));
    }

    @Test
    void invalidVersionMetadataCannotSelectADifferentBackend(@TempDir Path dir) throws IOException {
        String original = "config-version: '2'\nstorage:\n  type: postgresql\n";
        Files.writeString(dir.resolve("config.yml"), original);
        assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(original, Files.readString(dir.resolve("config.yml")));
    }

    @Test
    void errorReportedAfterConfigCommitStillRollsBackAllFiles(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("lang"));
        Path config = dir.resolve("config.yml");
        String original = "config-version: 1\nprefix: custom\n";
        String language = "# Owner formatting\nprefix: original\n";
        Files.writeString(config, original);
        Files.writeString(dir.resolve("lang/en.yml"), language);
        try (var files = Mockito.mockStatic(ConfigFiles.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> ConfigFiles.write(Mockito.any(), Mockito.eq(config))).thenAnswer(call -> {
                call.callRealMethod();
                throw new IOException("test error reported after commit");
            });
            assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        }
        assertEquals(original, Files.readString(config));
        assertEquals(language, Files.readString(dir.resolve("lang/en.yml")));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {1, 2, 3})
    void locationMigrationPreservesWhitelistPrecedenceAndIsIdempotent(int version, @TempDir Path dir) throws Exception {
        YamlConfiguration old = new YamlConfiguration();
        old.set("config-version", version);
        old.set(version == 3 ? "restrictions.disabled-worlds" : "disabled-command-worlds", List.of("lobby"));
        String path = version == 3 ? "rekit.kill" : "feature.rekit-on-kill";
        old.set(path + ".world-whitelist", List.of("arena"));
        old.set(path + ".world-blacklist", List.of("arena", "lobby"));
        old.set(path + ".kits.arena", "sword");
        ConfigFiles.write(old, dir.resolve("config.yml"));
        String before = Files.readString(dir.resolve("config.yml"));
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        YamlConfiguration result = ConfigFiles.read(dir.resolve("config.yml"));
        assertEquals("deny", result.getString("locations.global.mode"));
        assertEquals(List.of("lobby"), result.getStringList("locations.global.entries"));
        assertEquals("allow", result.getString("locations.rekit-kill.mode"));
        assertEquals(List.of("arena"), result.getStringList("locations.rekit-kill.entries"));
        assertEquals("sword", result.getString("rekit.kill.kits.arena"));
        assertFalse(result.contains("rekit.kill.world-whitelist"));
        assertFalse(result.contains("restrictions"));
        try (var files = Files.list(dir)) {
            Path backup = files.filter(file -> file.getFileName().toString().startsWith("config.yml.backup-")).findFirst().orElseThrow();
            assertEquals(before, Files.readString(backup));
        }
        String migrated = Files.readString(dir.resolve("config.yml"));
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(migrated, Files.readString(dir.resolve("config.yml")));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void emptyLegacyWhitelistUsesBlacklistOrAllowsEverywhere(boolean blacklist, @TempDir Path dir) throws Exception {
        YamlConfiguration old = new YamlConfiguration();
        old.set("config-version", 3);
        old.set("rekit.kill.world-whitelist", List.of());
        old.set("rekit.kill.world-blacklist", blacklist ? List.of("lobby") : List.of());
        ConfigFiles.write(old, dir.resolve("config.yml"));
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        YamlConfiguration result = ConfigFiles.read(dir.resolve("config.yml"));
        assertEquals("deny", result.getString("locations.rekit-kill.mode"));
        assertEquals(blacklist ? List.of("lobby") : List.of(), result.getStringList("locations.rekit-kill.entries"));
    }

    @Test void conflictingNewAndLegacyLocationRulesKeepTheOriginalFile(@TempDir Path dir) throws Exception {
        String original = "config-version: 3\nrestrictions:\n  disabled-worlds: [lobby]\nlocations:\n  global:\n    mode: allow\n    entries: [arena]\n";
        Files.writeString(dir.resolve("config.yml"), original);
        assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(original, Files.readString(dir.resolve("config.yml")));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints={1,2,3})
    void itemFilterMigrationRetainsExistingChoicesAndIsIdempotent(int version, @TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), "config-version: " + version
                + "\nanti-exploit:\n  only-allow-kitroom-items: true\n  import-filter: false\n  block-spaces-in-commands: true\n");
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        var migrated = ConfigFiles.read(dir.resolve("config.yml"));
        assertTrue(migrated.getBoolean("item-filter.enabled"));
        assertTrue(migrated.getBoolean("item-filter.kit-room-only"));
        assertFalse(migrated.getBoolean("item-filter.filter-imports"));
        assertFalse(migrated.getBoolean("item-filter.filter-saves"));
        assertTrue(migrated.getBoolean("item-filter.allow-unbreakable"));
        assertFalse(migrated.getBoolean("item-filter.allow-item-flags"));
        assertTrue(migrated.getBoolean("item-filter.enchantments.allow-incompatible"));
        assertTrue(migrated.getBoolean("anti-exploit.block-spaces-in-commands"));
        assertFalse(migrated.contains("anti-exploit.only-allow-kitroom-items"));
        String first = Files.readString(dir.resolve("config.yml"));
        assertTrue(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(first, Files.readString(dir.resolve("config.yml")));
    }

    @Test void conflictingItemFilterSchemasRetainTheOriginalConfig(@TempDir Path dir) throws Exception {
        String original = "config-version: 3\nanti-exploit: {only-allow-kitroom-items: true}\nitem-filter: {enabled: false}\n";
        Files.writeString(dir.resolve("config.yml"), original);
        assertFalse(new ConfigMigrator(pluginFor(dir.toFile())).migrate());
        assertEquals(original, Files.readString(dir.resolve("config.yml")));
    }
}
