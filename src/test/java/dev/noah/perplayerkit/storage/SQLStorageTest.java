package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.storage.sql.SQLDatabase;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLStorageTest {

    @Test
    void connectInitAndCloseWorkWithRealInMemoryDatabase() throws Exception {
        InMemorySQLiteDatabase db = new InMemorySQLiteDatabase();
        SQLStorage storage = new SQLStorage(db);

        storage.connect();
        storage.init();

        assertTrue(storage.isConnected());

        storage.close();
        assertFalse(storage.isConnected());
    }

    @Test
    void saveGetExistsDeleteAndListWork() throws Exception {
        InMemorySQLiteDatabase db = new InMemorySQLiteDatabase();
        SQLStorage storage = new SQLStorage(db);
        storage.connect();
        storage.init();

        storage.saveKitDataByID("kit-1", "payload-1");
        storage.saveKitDataByID("kit-2", "payload-2");

        assertEquals("payload-1", storage.getKitDataByID("kit-1"));
        assertTrue(storage.doesKitExistByID("kit-2"));
        assertEquals(Set.of("kit-1", "kit-2"), storage.getAllKitIDs());

        storage.deleteKitByID("kit-2");
        assertFalse(storage.doesKitExistByID("kit-2"));
        assertEquals("Error", storage.getKitDataByID("kit-2"));

        storage.close();
    }

    @Test
    void keepAliveSucceedsWhenConnected() throws Exception {
        InMemorySQLiteDatabase db = new InMemorySQLiteDatabase();
        SQLStorage storage = new SQLStorage(db);
        storage.connect();

        storage.keepAlive();

        storage.close();
    }

    @Test
    void connectWrapsCheckedException() {
        SQLStorage storage = new SQLStorage(new ThrowingConnectDatabase());

        assertThrows(StorageConnectionException.class, storage::connect);
    }

    @Test
    void initWrapsSqlException() throws Exception {
        SQLStorage storage = new SQLStorage(new ThrowingGetConnectionDatabase());

        storage.connect();
        assertThrows(StorageOperationException.class, storage::init);
    }

    @Test
    void keepAliveWrapsSqlException() throws Exception {
        SQLStorage storage = new SQLStorage(new ThrowingGetConnectionDatabase());

        storage.connect();
        assertThrows(StorageConnectionException.class, storage::keepAlive);
    }

    @Test
    void closeWrapsSqlException() throws Exception {
        SQLStorage storage = new SQLStorage(new ThrowingDisconnectDatabase());

        storage.connect();
        assertThrows(StorageConnectionException.class, storage::close);
    }

    private static class InMemorySQLiteDatabase implements SQLDatabase {
        private final String jdbcUrl = "jdbc:sqlite:file:" + UUID.randomUUID() + "?mode=memory&cache=shared";
        private Connection keepAliveConnection;

        @Override
        public boolean isConnected() {
            try {
                return keepAliveConnection != null && !keepAliveConnection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }

        @Override
        public void connect() throws SQLException {
            if (!isConnected()) {
                keepAliveConnection = DriverManager.getConnection(jdbcUrl);
            }
        }

        @Override
        public void disconnect() throws SQLException {
            if (keepAliveConnection != null) {
                keepAliveConnection.close();
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (!isConnected()) {
                throw new SQLException("Not connected");
            }
            return DriverManager.getConnection(jdbcUrl);
        }
    }

    private static class ThrowingConnectDatabase implements SQLDatabase {
        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void connect() throws ClassNotFoundException {
            throw new ClassNotFoundException("driver");
        }

        @Override
        public void disconnect() {
        }

        @Override
        public Connection getConnection() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ThrowingGetConnectionDatabase implements SQLDatabase {
        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("boom");
        }
    }

    private static class ThrowingDisconnectDatabase implements SQLDatabase {
        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() throws SQLException {
            throw new SQLException("boom");
        }

        @Override
        public Connection getConnection() {
            throw new UnsupportedOperationException();
        }
    }
}
