package dev.noah.perplayerkit.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageSelectorTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        YamlConfiguration config = new YamlConfiguration();

        when(plugin.getDataFolder()).thenReturn(new File("target/test-plugin-data"));
        when(plugin.getConfig()).thenReturn(config);
        config.set("storage.mysql.host", "localhost");
        config.set("storage.mysql.port", "3306");
        config.set("storage.mysql.dbname", "ppk");
        config.set("storage.mysql.username", "user");
        config.set("storage.mysql.password", "pass");
        config.set("storage.mysql.use-ssl", false);

        config.set("storage.postgresql.host", "localhost");
        config.set("storage.postgresql.port", "5432");
        config.set("storage.postgresql.dbname", "ppk");
        config.set("storage.postgresql.username", "user");
        config.set("storage.postgresql.password", "pass");
        config.set("storage.postgresql.use-ssl", false);

        config.set("storage.redis.host", "localhost");
        config.set("storage.redis.port", 6379);
        config.set("storage.redis.password", "");
    }

    @Test
    void yamlTypeReturnsYamlStorage() {
        StorageManager manager = new StorageSelector(plugin, "yaml").getDbManager();

        assertInstanceOf(YAMLStorage.class, manager);
    }

    @Test
    void ymlTypeReturnsYamlStorage() {
        StorageManager manager = new StorageSelector(plugin, "yml").getDbManager();

        assertInstanceOf(YAMLStorage.class, manager);
    }

    @Test
    void redisTypeReturnsRedisStorage() {
        StorageManager manager = new StorageSelector(plugin, "redis").getDbManager();

        assertInstanceOf(RedisStorage.class, manager);
    }

    @Test
    void mysqlTypeReturnsSqlStorage() {
        StorageManager manager = new StorageSelector(plugin, "mysql").getDbManager();

        assertInstanceOf(SQLStorage.class, manager);
    }

    @Test
    void postgresqlTypeReturnsSqlStorage() {
        StorageManager manager = new StorageSelector(plugin, "postgresql").getDbManager();

        assertInstanceOf(SQLStorage.class, manager);
    }

    @Test
    void sqliteTypeReturnsSqlStorage() {
        StorageManager manager = new StorageSelector(plugin, "sqlite").getDbManager();

        assertInstanceOf(SQLStorage.class, manager);
    }

    @Test
    void unknownTypeIsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new StorageSelector(plugin, "something-else"));
    }

    @Test
    void namesAreNormalizedWithoutChangingBackend() {
        org.junit.jupiter.api.Assertions.assertEquals("mysql", StorageSelector.normalize(" MySQL "));
        org.junit.jupiter.api.Assertions.assertEquals("yaml", StorageSelector.normalize("YAML"));
        org.junit.jupiter.api.Assertions.assertEquals("postgresql", StorageSelector.normalize("Postgres"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> StorageSelector.normalize(null));
    }
}
