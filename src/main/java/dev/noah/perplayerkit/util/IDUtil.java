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
package dev.noah.perplayerkit.util;

import java.util.UUID;

public class IDUtil {


    public static String getPlayerKitId(UUID playerId, int slot) {
        return playerId.toString() + slot;
    }

    public static String getECId(UUID playerId, int slot) {
        return playerId.toString() + "ec" + slot;
    }

    public static String getPublicKitId(String name) {
        return "public" + name;
    }

    public static String getKitRoomId(int slot) {
        return "kitroom" + slot;
    }


}
