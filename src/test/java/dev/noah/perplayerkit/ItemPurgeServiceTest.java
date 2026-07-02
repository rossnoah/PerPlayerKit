package dev.noah.perplayerkit;

import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemPurgeServiceTest {

    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private ItemStack mockItem(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    @Test
    void isPlayerDataIdMatchesKitAndEnderchestIdsOnly() {
        assertTrue(ItemPurgeService.isPlayerDataId(IDUtil.getPlayerKitId(PLAYER, 1)));
        assertTrue(ItemPurgeService.isPlayerDataId(IDUtil.getPlayerKitId(PLAYER, 9)));
        assertTrue(ItemPurgeService.isPlayerDataId(IDUtil.getPlayerKitId(PLAYER, 10)));
        assertTrue(ItemPurgeService.isPlayerDataId(IDUtil.getPlayerKitId(PLAYER, 99)));
        assertTrue(ItemPurgeService.isPlayerDataId(IDUtil.getECId(PLAYER, 5)));
        assertTrue(ItemPurgeService.isPlayerDataId(IDUtil.getECId(PLAYER, 42)));

        assertFalse(ItemPurgeService.isPlayerDataId(null));
        assertFalse(ItemPurgeService.isPlayerDataId(PLAYER.toString()));
        assertFalse(ItemPurgeService.isPlayerDataId(PLAYER + "0"));
        assertFalse(ItemPurgeService.isPlayerDataId(PLAYER + "05"));
        assertFalse(ItemPurgeService.isPlayerDataId(PLAYER + "100"));
        assertFalse(ItemPurgeService.isPlayerDataId(PLAYER + "ec0"));
        assertFalse(ItemPurgeService.isPlayerDataId(PLAYER + "ec100"));
        assertFalse(ItemPurgeService.isPlayerDataId(IDUtil.getPublicKitId("warrior")));
        assertFalse(ItemPurgeService.isPlayerDataId(IDUtil.getKitRoomId(1)));
        assertFalse(ItemPurgeService.isPlayerDataId("not-a-uuid-aaaa-bbbb-cccc-ddddeeeeffff1"));
    }

    @Test
    void purgeAllPlayersSkipsPublicKitsAndKitRoom() {
        StorageManager storage = mock(StorageManager.class);
        KitManager kitManager = mock(KitManager.class);
        String kitId = IDUtil.getPlayerKitId(PLAYER, 1);

        when(storage.getAllKitIDs()).thenReturn(Set.of(
                kitId, IDUtil.getPublicKitId("warrior"), IDUtil.getKitRoomId(1)));
        when(storage.getKitDataByID(kitId)).thenReturn("blob");

        ItemStack[] contents = {mockItem(Material.TNT, 2), mockItem(Material.APPLE, 1)};

        try (MockedStatic<Serializer> serializer = mockStatic(Serializer.class)) {
            serializer.when(() -> Serializer.itemStackArrayFromBase64("blob")).thenReturn(contents);
            serializer.when(() -> Serializer.itemStackArrayToBase64(contents)).thenReturn("newblob");

            ItemPurgeService service = new ItemPurgeService(storage, kitManager);
            ItemPurgeService.PurgeResult result = service.purgeAllPlayers(Material.TNT, null);

            assertEquals(1, result.scanned());
            assertEquals(1, result.modified());
            assertEquals(0, result.deleted());
            assertEquals(2, result.itemsRemoved());
            assertEquals(0, result.failed());
        }

        verify(storage).saveKitDataByID(kitId, "newblob");
        verify(storage).getKitDataByID(kitId);
        verify(storage, never()).getKitDataByID(IDUtil.getPublicKitId("warrior"));
        verify(storage, never()).getKitDataByID(IDUtil.getKitRoomId(1));
        verify(kitManager).updateCachedKit(kitId, contents);
    }

    @Test
    void deletesEntriesThatBecomeEmpty() {
        StorageManager storage = mock(StorageManager.class);
        KitManager kitManager = mock(KitManager.class);
        String kitId = IDUtil.getPlayerKitId(PLAYER, 3);

        when(storage.getAllKitIDs()).thenReturn(Set.of(kitId));
        when(storage.getKitDataByID(kitId)).thenReturn("blob");

        ItemStack[] contents = {mockItem(Material.TNT, 5), null};

        try (MockedStatic<Serializer> serializer = mockStatic(Serializer.class)) {
            serializer.when(() -> Serializer.itemStackArrayFromBase64("blob")).thenReturn(contents);

            ItemPurgeService service = new ItemPurgeService(storage, kitManager);
            ItemPurgeService.PurgeResult result = service.purgeAllPlayers(Material.TNT, null);

            assertEquals(1, result.deleted());
            assertEquals(1, result.modified());
            assertEquals(5, result.itemsRemoved());
        }

        verify(storage).deleteKitByID(kitId);
        verify(storage, never()).saveKitDataByID(eq(kitId), any());
        verify(kitManager).updateCachedKit(kitId, null);
    }

    @Test
    void leavesEntriesWithoutMatchesUnchanged() {
        StorageManager storage = mock(StorageManager.class);
        KitManager kitManager = mock(KitManager.class);
        String kitId = IDUtil.getPlayerKitId(PLAYER, 2);

        when(storage.getAllKitIDs()).thenReturn(Set.of(kitId));
        when(storage.getKitDataByID(kitId)).thenReturn("blob");

        ItemStack[] contents = {mockItem(Material.APPLE, 1)};

        try (MockedStatic<Serializer> serializer = mockStatic(Serializer.class)) {
            serializer.when(() -> Serializer.itemStackArrayFromBase64("blob")).thenReturn(contents);

            ItemPurgeService service = new ItemPurgeService(storage, kitManager);
            ItemPurgeService.PurgeResult result = service.purgeAllPlayers(Material.TNT, null);

            assertEquals(1, result.scanned());
            assertEquals(0, result.modified());
            assertEquals(0, result.itemsRemoved());
        }

        verify(storage, never()).saveKitDataByID(any(), any());
        verify(storage, never()).deleteKitByID(any());
    }

    @Test
    void purgePlayersOnlyTouchesEntriesOfTheGivenPlayers() {
        StorageManager storage = mock(StorageManager.class);
        KitManager kitManager = mock(KitManager.class);
        UUID otherPlayer = UUID.fromString("99999999-8888-7777-6666-555555555555");
        String presentId = IDUtil.getECId(PLAYER, 4);
        // slot 42 exists even though it is above the default max-kits of 9:
        // orphaned slots from a lowered limit must still be purged
        String orphanedId = IDUtil.getPlayerKitId(PLAYER, 42);

        when(storage.getAllKitIDs()).thenReturn(Set.of(
                presentId, orphanedId,
                IDUtil.getPlayerKitId(otherPlayer, 1),
                IDUtil.getPublicKitId("warrior"),
                IDUtil.getKitRoomId(1)));
        when(storage.getKitDataByID(presentId)).thenReturn("blob1");
        when(storage.getKitDataByID(orphanedId)).thenReturn("blob2");

        ItemStack[] contents1 = {mockItem(Material.TNT, 1), mockItem(Material.APPLE, 1)};
        ItemStack[] contents2 = {mockItem(Material.TNT, 1), mockItem(Material.APPLE, 1)};

        try (MockedStatic<Serializer> serializer = mockStatic(Serializer.class)) {
            serializer.when(() -> Serializer.itemStackArrayFromBase64("blob1")).thenReturn(contents1);
            serializer.when(() -> Serializer.itemStackArrayFromBase64("blob2")).thenReturn(contents2);
            serializer.when(() -> Serializer.itemStackArrayToBase64(contents1)).thenReturn("newblob1");
            serializer.when(() -> Serializer.itemStackArrayToBase64(contents2)).thenReturn("newblob2");

            ItemPurgeService service = new ItemPurgeService(storage, kitManager);
            ItemPurgeService.PurgeResult result = service.purgePlayers(Material.TNT, List.of(PLAYER), null);

            assertEquals(2, result.scanned());
            assertEquals(2, result.modified());
            assertEquals(2, result.itemsRemoved());
        }

        verify(storage).saveKitDataByID(presentId, "newblob1");
        verify(storage).saveKitDataByID(orphanedId, "newblob2");
        verify(storage, never()).getKitDataByID(IDUtil.getPlayerKitId(otherPlayer, 1));
        verify(storage, never()).getKitDataByID(IDUtil.getPublicKitId("warrior"));
    }

    @Test
    void countsFailuresWithoutAborting() {
        StorageManager storage = mock(StorageManager.class);
        KitManager kitManager = mock(KitManager.class);
        String badId = IDUtil.getPlayerKitId(PLAYER, 1);
        String goodId = IDUtil.getPlayerKitId(PLAYER, 2);

        when(storage.getAllKitIDs()).thenReturn(Set.of(badId, goodId));
        when(storage.getKitDataByID(badId)).thenReturn("badblob");
        when(storage.getKitDataByID(goodId)).thenReturn("goodblob");

        ItemStack[] contents = {mockItem(Material.TNT, 1), mockItem(Material.APPLE, 1)};

        try (MockedStatic<Serializer> serializer = mockStatic(Serializer.class)) {
            serializer.when(() -> Serializer.itemStackArrayFromBase64("badblob"))
                    .thenThrow(new RuntimeException("corrupt"));
            serializer.when(() -> Serializer.itemStackArrayFromBase64("goodblob")).thenReturn(contents);
            serializer.when(() -> Serializer.itemStackArrayToBase64(contents)).thenReturn("newblob");

            ItemPurgeService service = new ItemPurgeService(storage, kitManager);
            ItemPurgeService.PurgeResult result = service.purgeAllPlayers(Material.TNT, null);

            assertEquals(1, result.failed());
            assertEquals(1, result.modified());
        }

        verify(storage).saveKitDataByID(goodId, "newblob");
    }
}
