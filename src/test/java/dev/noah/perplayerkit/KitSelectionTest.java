package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.IDUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class KitSelectionTest {
    @Test void referencesRoundTripWithoutCollidingWithInventoryIds() {
        UUID uuid = UUID.randomUUID();
        assertFalse(IDUtil.isPlayerDataId(KitSelection.kitKey(uuid)));
        assertFalse(IDUtil.isPlayerDataId(KitSelection.enderchestKey(uuid)));
        assertNotEquals(KitSelection.kitKey(uuid), KitSelection.enderchestKey(uuid));
        for (KitManager.KitReference ref : new KitManager.KitReference[]{
                new KitManager.KitReference(99, null), new KitManager.KitReference(null, "custom:kit.with.dots")})
            assertEquals(ref, KitSelection.decodeKit(KitSelection.encode(ref)));
    }
    @ParameterizedTest @ValueSource(strings={"kit:0", "kit:100", "kit:nope", "public:", "public: ", "1", "something:2"})
    void rejectsInvalidReferences(String value) {
        assertThrows(IllegalArgumentException.class, () -> KitSelection.decodeKit(value));
    }
}
