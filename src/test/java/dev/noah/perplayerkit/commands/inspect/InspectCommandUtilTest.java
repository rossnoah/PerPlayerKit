package dev.noah.perplayerkit.commands.inspect;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        UUID resolvedUuid = InspectCommandUtil.selectResolvedUuid(identifier, null, () -> null, false);

        assertEquals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + identifier).getBytes(StandardCharsets.UTF_8)),
                resolvedUuid);
    }

    @Test
    void selectResolvedUuidReturnsNullWhenServerIsOnlineModeAndNoMatchExists() {
        UUID resolvedUuid = InspectCommandUtil.selectResolvedUuid("TargetPlayer", null, () -> null, true);

        assertNull(resolvedUuid);
    }
}
