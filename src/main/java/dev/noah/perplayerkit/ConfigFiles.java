package dev.noah.perplayerkit;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Config writes must never turn a parse or disk error into an empty configuration. */
public final class ConfigFiles {
    private ConfigFiles() {}

    public static YamlConfiguration read(Path path) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(path.toFile());
        } catch (InvalidConfigurationException e) {
            throw new IOException("Invalid YAML in " + path.getFileName() + ": " + e.getMessage(), e);
        }
        return config;
    }

    public static Path backup(Path path) throws IOException {
        Path backup = Files.createTempFile(path.toAbsolutePath().getParent(), path.getFileName() + ".backup-", ".yml");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException failure) {
            try { Files.deleteIfExists(backup); }
            catch (IOException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        }
    }

    public static void write(FileConfiguration config, Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), ".ppk-config-", ".tmp");
        try {
            Files.writeString(temporary, config.saveToString(), StandardCharsets.UTF_8);
            read(temporary);
            replace(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Restore exact pre-upgrade bytes, including an owner's formatting and comments. */
    public static void restore(Path backup, Path path) throws IOException {
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), ".ppk-restore-", ".tmp");
        try {
            Files.copy(backup, temporary, StandardCopyOption.REPLACE_EXISTING);
            replace(temporary, path);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void replace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
