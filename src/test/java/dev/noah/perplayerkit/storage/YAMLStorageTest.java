package dev.noah.perplayerkit.storage;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YAMLStorageTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        Logger logger = Logger.getLogger("YAMLStorageTest");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.OFF);
        when(plugin.getLogger()).thenReturn(logger);
    }

    @Test
    void initCreatesStorageFileWhenMissing(@TempDir Path tempDir) {
        Path filePath = tempDir.resolve("storage.yml");
        YAMLStorage storage = new YAMLStorage(plugin, filePath.toString());

        storage.init();

        assertTrue(filePath.toFile().exists());
        assertTrue(storage.getAllKitIDs().isEmpty());
    }

    @Test
    void saveLoadDeleteAndListWork(@TempDir Path tempDir) {
        Path filePath = tempDir.resolve("storage.yml");
        YAMLStorage storage = new YAMLStorage(plugin, filePath.toString());
        storage.init();

        storage.saveKitDataByID("kit-1", "payload-1");
        storage.saveKitDataByID("kit-2", "payload-2");

        assertEquals("payload-1", storage.getKitDataByID("kit-1"));
        assertTrue(storage.doesKitExistByID("kit-2"));
        assertEquals(Set.of("kit-1", "kit-2"), storage.getAllKitIDs());

        YAMLStorage reloaded = new YAMLStorage(plugin, filePath.toString());
        reloaded.init();
        assertEquals("payload-1", reloaded.getKitDataByID("kit-1"));

        reloaded.deleteKitByID("kit-1");
        assertFalse(reloaded.doesKitExistByID("kit-1"));
        assertEquals("error", reloaded.getKitDataByID("kit-1"));
    }

    @Test
    void closePersistsCurrentState(@TempDir Path tempDir) {
        Path filePath = tempDir.resolve("storage.yml");
        YAMLStorage storage = new YAMLStorage(plugin, filePath.toString());
        storage.init();
        storage.saveKitDataByID("kit-3", "payload-3");

        storage.close();

        YAMLStorage reloaded = new YAMLStorage(plugin, filePath.toString());
        reloaded.init();
        assertEquals("payload-3", reloaded.getKitDataByID("kit-3"));
    }
}
