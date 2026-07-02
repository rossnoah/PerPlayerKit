package dev.noah.perplayerkit;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitShareManagerRequestTest {

    private final UUID sender = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final UUID otherTarget = UUID.randomUUID();

    private ShareRequest kitRequest(String id, UUID targetId) {
        return ShareRequest.kit(id, sender, "Sender", targetId, new ItemStack[0]);
    }

    @Test
    void findRequestsForTargetOnlyReturnsMatchingTarget() {
        Map<String, ShareRequest> requests = new HashMap<>();
        requests.put("AAAAAA", kitRequest("AAAAAA", target));
        requests.put("BBBBBB", kitRequest("BBBBBB", otherTarget));

        List<ShareRequest> pending = KitShareManager.findRequestsForTarget(requests, target);

        assertEquals(1, pending.size());
        assertEquals("AAAAAA", pending.get(0).getId());
    }

    @Test
    void findRequestsForTargetReturnsEmptyWhenNoneMatch() {
        Map<String, ShareRequest> requests = new HashMap<>();
        requests.put("AAAAAA", kitRequest("AAAAAA", otherTarget));

        assertTrue(KitShareManager.findRequestsForTarget(requests, target).isEmpty());
    }

    @Test
    void findRequestByIdMatchesCaseInsensitively() {
        Map<String, ShareRequest> requests = new HashMap<>();
        ShareRequest request = kitRequest("ABC123", target);
        requests.put("ABC123", request);

        assertSame(request, KitShareManager.findRequestById(requests, target, "abc123"));
    }

    @Test
    void findRequestByIdRejectsWrongTarget() {
        Map<String, ShareRequest> requests = new HashMap<>();
        requests.put("ABC123", kitRequest("ABC123", otherTarget));

        assertNull(KitShareManager.findRequestById(requests, target, "ABC123"));
    }

    @Test
    void findRequestByIdReturnsNullForUnknownId() {
        Map<String, ShareRequest> requests = new HashMap<>();
        requests.put("ABC123", kitRequest("ABC123", target));

        assertNull(KitShareManager.findRequestById(requests, target, "ZZZZZZ"));
    }

    @Test
    void transferRequestKeepsSlotSnapshots() {
        Map<Integer, ItemStack[]> kits = Map.of(1, new ItemStack[0], 3, new ItemStack[0]);
        Map<Integer, ItemStack[]> enderchests = Map.of(2, new ItemStack[0]);

        ShareRequest request = ShareRequest.transfer("XYZ789", sender, "Sender", target, kits, enderchests);

        assertEquals(ShareRequest.Type.TRANSFER, request.getType());
        assertEquals(2, request.getKits().size());
        assertEquals(1, request.getEnderchests().size());
        assertEquals("Sender", request.getSenderName());
    }
}
