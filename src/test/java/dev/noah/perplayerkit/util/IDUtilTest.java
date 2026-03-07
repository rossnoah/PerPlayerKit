package dev.noah.perplayerkit.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IDUtilTest {

    @Test
    void getPlayerKitIdIncludesUuidAndSlot() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        assertEquals("123e4567-e89b-12d3-a456-4266141740003", IDUtil.getPlayerKitId(uuid, 3));
    }

    @Test
    void getECIdIncludesUuidEcAndSlot() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        assertEquals("123e4567-e89b-12d3-a456-426614174000ec7", IDUtil.getECId(uuid, 7));
    }

    @Test
    void getPublicKitIdPrefixesPublic() {
        assertEquals("publicduel", IDUtil.getPublicKitId("duel"));
    }

    @Test
    void getKitRoomIdPrefixesKitRoom() {
        assertEquals("kitroom9", IDUtil.getKitRoomId(9));
    }
}
