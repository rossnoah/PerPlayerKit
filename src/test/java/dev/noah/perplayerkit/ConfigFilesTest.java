package dev.noah.perplayerkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFilesTest {
    @TempDir Path folder;

    private Set<String> files() throws IOException {
        try (var files = Files.list(folder)) {
            return files.map(path -> path.getFileName().toString()).collect(Collectors.toSet());
        }
    }

    @Test void failedBackupDoesNotLeaveAnEmptyRecoveryFile() throws IOException {
        assertThrows(IOException.class, () -> ConfigFiles.backup(folder.resolve("missing.yml")));
        assertTrue(files().isEmpty());
    }

    @Test void writeFailureRetainsTheOriginalAndCleansTemporaryFiles() throws IOException {
        Path target = Files.createDirectory(folder.resolve("config.yml"));
        Files.writeString(target.resolve("owner-data"), "keep me");
        YamlConfiguration config = new YamlConfiguration();
        config.set("config-version", 3);
        assertThrows(IOException.class, () -> ConfigFiles.write(config, target));
        assertEquals("keep me", Files.readString(target.resolve("owner-data")));
        assertEquals(Set.of("config.yml"), files());
    }

    @Test void backupAndRestoreKeepExactOwnerBytes() throws IOException {
        Path config = folder.resolve("config.yml");
        String original = "# Owner comment\nprefix: '<gold>Пример</gold>'\n";
        Files.writeString(config, original);
        Path backup = ConfigFiles.backup(config);
        YamlConfiguration replacement = new YamlConfiguration();
        replacement.set("prefix", "changed");
        ConfigFiles.write(replacement, config);
        ConfigFiles.restore(backup, config);
        assertEquals(original, Files.readString(config));
        assertEquals(original, Files.readString(backup));
        assertEquals(2, files().size());
    }

    @Test void missingBackupCannotDamageTheCurrentConfig() throws IOException {
        Path config = folder.resolve("config.yml");
        Files.writeString(config, "prefix: original\n");
        assertThrows(IOException.class, () -> ConfigFiles.restore(folder.resolve("missing"), config));
        assertEquals("prefix: original\n", Files.readString(config));
        assertEquals(Set.of("config.yml"), files());
    }
}
