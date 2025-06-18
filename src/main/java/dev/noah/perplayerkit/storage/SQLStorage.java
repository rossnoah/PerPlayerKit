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
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.storage.sql.SQLDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLStorage implements StorageManager {

    private final SQLDatabase db;

    public SQLStorage(SQLDatabase db) {
        this.db = db;
    }

    private void createTable() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS kits (KITID VARCHAR(100), KITDATA TEXT(15000), PRIMARY KEY (KITID))")) {
            ps.executeUpdate();
        }
    }

    @Override
    public void init() throws StorageOperationException {
        try {
            createTable();
        } catch (SQLException e) {
           throw new StorageOperationException("Failed to initialize the database", e);
        }
    }

    @Override
    public void connect() throws StorageConnectionException {
        try {
            db.connect();
        } catch (ClassNotFoundException | SQLException e) {
            throw new StorageConnectionException("Failed to connect to the database", e);
        }
    }

    @Override
    public boolean isConnected() {
        return db.isConnected();
    }

    @Override
    public void close() throws StorageConnectionException {
        try {
            db.disconnect();
        } catch (SQLException e) {
            throw new StorageConnectionException("Failed to close the database connection", e);
        }
    }

    @Override
    public void keepAlive() throws StorageConnectionException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
             ps.executeQuery();
        } catch (SQLException e) {
            throw new StorageConnectionException("Failed to keep the connection alive", e);
        }
    }

    @Override
    public void saveKitDataByID(String kitID, String data) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "REPLACE INTO kits (KITID, KITDATA) VALUES (?,?)")) {
            ps.setString(1, kitID);
            ps.setString(2, data);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKitDataByID(String kitID) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                    "SELECT KITDATA FROM kits WHERE KITID=?")) {
            ps.setString(1, kitID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("KITDATA");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Error";
    }

    @Override
    public boolean doesKitExistByID(String kitID) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT KITID FROM kits WHERE KITID=?")) {
            ps.setString(1, kitID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void deleteKitByID(String kitID) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM kits WHERE KITID=?")) {
            ps.setString(1, kitID);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
