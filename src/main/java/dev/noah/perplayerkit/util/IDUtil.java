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
