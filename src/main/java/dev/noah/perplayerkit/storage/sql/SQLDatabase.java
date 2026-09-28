/*
 * Copyright 2022-2026 Noah Ross
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
package dev.noah.perplayerkit.storage.sql;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public interface SQLDatabase {

    boolean isConnected();

    void connect() throws ClassNotFoundException, SQLException;

    void disconnect() throws SQLException;

    Connection getConnection() throws SQLException;

    /**
     * SQL used to create the kits table. Defaults to MySQL/SQLite-compatible syntax;
     * backends with a different dialect (e.g. PostgreSQL) override this.
     */
    default String getCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS kits (KITID VARCHAR(100), KITDATA TEXT(15000), PRIMARY KEY (KITID))";
    }

    /**
     * Bring a kits table created by an older version up to the current schema.
     * Called after {@link #getCreateTableStatement()} on every startup, so it must
     * be a no-op when the table is already current.
     */
    default void upgradeSchema(Connection conn) throws SQLException {
    }

    /**
     * SQL used to insert-or-update a kit row. Defaults to MySQL/SQLite {@code REPLACE INTO};
     * backends with a different dialect (e.g. PostgreSQL) override this.
     */
    default String getUpsertStatement() {
        return "REPLACE INTO kits (KITID, KITDATA) VALUES (?,?)";
    }

    /**
     * Whether this backend can snapshot itself to a file with
     * {@link #backupTo(Path)}. Only embedded, file-backed databases can; server
     * backends (MySQL, PostgreSQL) are backed up by their own tooling.
     */
    default boolean supportsOnlineBackup() {
        return false;
    }

    /**
     * Write a consistent snapshot of the database to {@code target} using the
     * database engine's own backup facility, safe to call while the server is
     * running and writing.
     *
     * @throws SQLException if the snapshot could not be taken
     */
    default void backupTo(Path target) throws SQLException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support online backups");
    }

}
