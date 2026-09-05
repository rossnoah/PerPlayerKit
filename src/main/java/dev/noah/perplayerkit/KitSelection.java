package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.KitSlots;
import java.util.UUID;

/** Small preference records kept outside the inventory ID namespace. */
final class KitSelection {
    private KitSelection() {}
    static String kitKey(UUID player) { return "ppk-selection:kit:" + player; }
    static String enderchestKey(UUID player) { return "ppk-selection:ec:" + player; }
    static String encode(KitManager.KitReference kit) {
        return kit.publicId() == null ? "kit:" + kit.slot() : "public:" + kit.publicId();
    }
    static KitManager.KitReference decodeKit(String value) {
        if (value.startsWith("kit:")) return new KitManager.KitReference(decodeSlot(value.substring(4)), null);
        if (value.startsWith("public:") && !value.substring(7).isBlank())
            return new KitManager.KitReference(null, value.substring(7));
        throw new IllegalArgumentException("Invalid remembered kit reference");
    }
    static int decodeSlot(String value) {
        Integer slot = KitSlots.parseSlotSuffix(value);
        if (slot == null) throw new IllegalArgumentException("Invalid remembered kit slot");
        return slot;
    }
}
