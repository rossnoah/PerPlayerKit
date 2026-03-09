package dev.noah.perplayerkit.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PlayerUtilTest {

    @Test
    void getPlayerNameUsesOnlinePlayerNameWhenAvailable() {
        UUID uuid = UUID.randomUUID();
        Player onlinePlayer = mock(Player.class);
        when(onlinePlayer.getName()).thenReturn("OnlineName");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(onlinePlayer);

            assertEquals("OnlineName", PlayerUtil.getPlayerName(uuid));
        }
    }

    @Test
    void getPlayerNameUsesOfflinePlayerNameWhenPlayerIsNotOnline() {
        UUID uuid = UUID.randomUUID();
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getName()).thenReturn("OfflineName");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer(uuid)).thenReturn(offlinePlayer);

            assertEquals("OfflineName", PlayerUtil.getPlayerName(uuid));
        }
    }

    @Test
    void getPlayerNameFallsBackToUuidWhenNoNameIsKnown() {
        UUID uuid = UUID.randomUUID();
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getName()).thenReturn(null);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer(uuid)).thenReturn(offlinePlayer);

            assertEquals(uuid.toString(), PlayerUtil.getPlayerName(uuid));
        }
    }
}
