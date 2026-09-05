package dev.noah.perplayerkit.kitdata;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.kitdata.KitDataService.ImportPlan;
import dev.noah.perplayerkit.kitdata.KitDataService.Scope;
import dev.noah.perplayerkit.kitdata.KitDataSnapshot.PublicKitEntry;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KitDataServiceTest {

    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final StorageManager storage = mock(StorageManager.class);
    private final KitManager kitManager = mock(KitManager.class);
    private final KitDataService service = new KitDataService(storage, kitManager);

    @Test
    void buildSnapshotAllCollectsEverySection() {
        String kitId = IDUtil.getPlayerKitId(PLAYER, 1);
        when(storage.getKitDataByID(anyString())).thenReturn("error");
        when(storage.getKitDataByID(IDUtil.getKitRoomId(0))).thenReturn("room0");
        when(storage.getKitDataByID(IDUtil.getPublicKitId("warrior"))).thenReturn("warrior-blob");
        when(storage.getKitDataByID(kitId)).thenReturn("kit-blob");
        when(storage.getAllKitIDs()).thenReturn(Set.of(
                kitId, IDUtil.getPublicKitId("warrior"), IDUtil.getKitRoomId(0)));
        when(kitManager.getPublicKitList()).thenReturn(List.of(
                new PublicKit("warrior", "Warrior", Material.DIAMOND_SWORD),
                new PublicKit("archer", "Archer", Material.BOW)));

        KitDataSnapshot snapshot = service.buildSnapshot(Scope.ALL);

        assertEquals("room0", snapshot.getKitRoomPages().get(0));
        assertEquals(1, snapshot.getKitRoomPages().size());

        assertEquals(2, snapshot.getPublicKits().size());
        assertEquals("warrior-blob", snapshot.getPublicKits().get("warrior").data());
        assertEquals("DIAMOND_SWORD", snapshot.getPublicKits().get("warrior").icon());
        // config-only public kit is still exported, without item data
        assertNull(snapshot.getPublicKits().get("archer").data());

        assertEquals("kit-blob", snapshot.getPlayerKits().get(kitId));
        assertEquals(1, snapshot.getPlayerKits().size());
    }

    @Test
    void buildSnapshotRespectsScope() {
        when(storage.getKitDataByID(anyString())).thenReturn("error");
        for (int page = 0; page < 5; page++) when(storage.getKitDataByID(IDUtil.getKitRoomId(page))).thenReturn("blob");
        when(storage.getKitDataByID(IDUtil.getPublicKitId("warrior"))).thenReturn("blob");
        when(storage.getKitDataByID(IDUtil.getPlayerKitId(PLAYER, 1))).thenReturn("blob");
        when(storage.getAllKitIDs()).thenReturn(Set.of(IDUtil.getPlayerKitId(PLAYER, 1)));
        when(kitManager.getPublicKitList()).thenReturn(List.of(
                new PublicKit("warrior", "Warrior", Material.DIAMOND_SWORD)));

        KitDataSnapshot kitroomOnly = service.buildSnapshot(Scope.KITROOM);
        assertEquals(5, kitroomOnly.getKitRoomPages().size());
        assertTrue(kitroomOnly.getPublicKits().isEmpty());
        assertTrue(kitroomOnly.getPlayerKits().isEmpty());

        KitDataSnapshot publicOnly = service.buildSnapshot(Scope.PUBLICKITS);
        assertTrue(publicOnly.getKitRoomPages().isEmpty());
        assertEquals(1, publicOnly.getPublicKits().size());
        assertTrue(publicOnly.getPlayerKits().isEmpty());

        KitDataSnapshot playersOnly = service.buildSnapshot(Scope.PLAYERKITS);
        assertTrue(playersOnly.getKitRoomPages().isEmpty());
        assertTrue(playersOnly.getPublicKits().isEmpty());
        assertEquals(1, playersOnly.getPlayerKits().size());
    }

    @Test
    void analyzeImportCountsOverwrites() {
        String existingKit = IDUtil.getPlayerKitId(PLAYER, 1);
        String newKit = IDUtil.getPlayerKitId(PLAYER, 2);
        when(storage.getAllKitIDs()).thenReturn(Set.of(
                existingKit, IDUtil.getKitRoomId(0), IDUtil.getPublicKitId("warrior")));
        // "archer" exists only in the config, without saved data
        when(kitManager.getPublicKitList()).thenReturn(List.of(
                new PublicKit("archer", "Archer", Material.BOW)));

        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(0, "blob"); // overwrites
        snapshot.getKitRoomPages().put(1, "blob"); // new
        snapshot.getPublicKits().put("warrior", new PublicKitEntry("warrior", "W", "CHEST", "blob")); // overwrites data
        snapshot.getPublicKits().put("archer", new PublicKitEntry("archer", "A", "BOW", "blob")); // overwrites config
        snapshot.getPublicKits().put("mage", new PublicKitEntry("mage", "M", "STICK", "blob")); // new
        snapshot.getPlayerKits().put(existingKit, "blob"); // overwrites
        snapshot.getPlayerKits().put(newKit, "blob"); // new

        ImportPlan plan = service.analyzeImport(snapshot);
        assertEquals(1, plan.kitRoomPagesOverwritten());
        assertEquals(2, plan.publicKitsOverwritten());
        assertEquals(1, plan.playerKitsOverwritten());
        assertTrue(plan.overwritesAnything());
    }

    @Test
    void analyzeImportWithNoConflictsNeedsNoConfirm() {
        when(storage.getAllKitIDs()).thenReturn(Set.of());
        when(kitManager.getPublicKitList()).thenReturn(List.of());

        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(0, "blob");

        assertFalse(service.analyzeImport(snapshot).overwritesAnything());
    }

    @Test
    void validateItemDataFailsClosedOnUndecodableBlob() {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(0, "good");
        snapshot.getPlayerKits().put(IDUtil.getPlayerKitId(PLAYER, 1), "bad");

        try (MockedStatic<Serializer> serializer = mockStatic(Serializer.class)) {
            serializer.when(() -> Serializer.itemStackArrayFromBase64("good")).thenReturn(new ItemStack[0]);
            serializer.when(() -> Serializer.itemStackArrayFromBase64("bad")).thenThrow(new IOException("boom"));

            InvalidKitDataException e = assertThrows(InvalidKitDataException.class,
                    () -> service.validateItemData(snapshot));
            assertTrue(e.getMessage().contains("could not be decoded"));
        }
    }

    @Test
    void applyToStorageWritesEveryBlobVerbatim() {
        String kitId = IDUtil.getPlayerKitId(PLAYER, 1);
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(2, "room-blob");
        snapshot.getPublicKits().put("warrior", new PublicKitEntry("warrior", "W", "CHEST", "warrior-blob"));
        snapshot.getPublicKits().put("archer", new PublicKitEntry("archer", "A", "BOW", null));
        snapshot.getPlayerKits().put(kitId, "kit-blob");

        service.applyToStorage(snapshot);

        verify(storage).saveKitDataByID(IDUtil.getKitRoomId(2), "room-blob");
        verify(storage).saveKitDataByID(IDUtil.getPublicKitId("warrior"), "warrior-blob");
        verify(storage).saveKitDataByID(kitId, "kit-blob");
        // config-only kit has no data to write
        verify(storage, org.mockito.Mockito.never())
                .saveKitDataByID(org.mockito.ArgumentMatchers.eq(IDUtil.getPublicKitId("archer")), anyString());
    }
}
