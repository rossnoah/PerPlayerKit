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

public interface StorageManager {


    boolean isConnected();

    void connect() throws StorageConnectionException;

    void init() throws StorageOperationException;

    void close() throws StorageConnectionException;

    void keepAlive() throws StorageConnectionException;

    void saveKitDataByID(String kitID, String data);

    String getKitDataByID(String kitID);

    boolean doesKitExistByID(String kitID);

    void deleteKitByID(String kitID);

}

