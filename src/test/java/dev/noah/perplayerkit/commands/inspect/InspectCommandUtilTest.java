package dev.noah.perplayerkit.commands.inspect;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

class InspectCommandUtilTest {

    @Test
    void selectResolvedUuidPrefersCachedOfflinePlayer() {
        UUID cachedUuid = UUID.randomUUID();

        UUID resolvedUuid = InspectCommandUtil.selectResolvedUuid("TargetPlayer", cachedUuid,
                () -> {
                    throw new AssertionError("Mojang lookup should not run when cached player exists");
                }, false);

        assertEquals(cachedUuid, resolvedUuid);
    }

    @Test
    void selectResolvedUuidUsesMojangResultWhenCacheMisses() {
        UUID mojangUuid = UUID.randomUUID();

        UUID resolvedUuid = InspectCommandUtil.selectResolvedUuid("TargetPlayer", null, () -> mojangUuid, true);

        assertEquals(mojangUuid, resolvedUuid);
    }

    @Test
    void selectResolvedUuidUsesOfflineFallbackWhenServerIsOfflineMode() {
        String identifier = "TargetPlayer";

        UUID resolvedUuid = InspectCommandUtil.selectResolvedUuid(identifier, null, () -> {
            throw new RuntimeException("mojang should not be consulted");
        }, false);

        assertEquals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + identifier).getBytes(StandardCharsets.UTF_8)),
                resolvedUuid);
    }

    @Test
    void selectResolvedUuidReturnsNullWhenServerIsOnlineModeAndNoMatchExists() {
        UUID resolvedUuid = InspectCommandUtil.selectResolvedUuid("TargetPlayer", null, () -> null, true);

        assertNull(resolvedUuid);
    }

    @Test
    void findCachedOfflinePlayerUuidDoesNotScanOfflinePlayers() throws Exception {
        Method method = InspectCommandUtil.class.getDeclaredMethod("findCachedOfflinePlayerUuid", String.class);
        method.setAccessible(true);
        Server server = mock(Server.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);

            UUID cachedUuid = (UUID) method.invoke(null, "TargetPlayer");

            assertNull(cachedUuid);
            bukkit.verify(Bukkit::getOfflinePlayers, never());
        }
    }
}
