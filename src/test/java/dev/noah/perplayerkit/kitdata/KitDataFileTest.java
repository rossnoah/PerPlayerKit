package dev.noah.perplayerkit.kitdata;

import dev.noah.perplayerkit.kitdata.KitDataSnapshot.PublicKitEntry;
import dev.noah.perplayerkit.util.IDUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitDataFileTest {

    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private KitDataSnapshot sampleSnapshot() {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(0, "kitroom-page-0-blob");
        snapshot.getKitRoomPages().put(4, "kitroom-page-4-blob");
        snapshot.getPublicKits().put("warrior",
                new PublicKitEntry("warrior", "Warrior Kit", "DIAMOND_SWORD", "warrior-blob"));
        snapshot.getPublicKits().put("archer",
                new PublicKitEntry("archer", "Archer", "BOW", null));
        snapshot.getPlayerKits().put(IDUtil.getPlayerKitId(PLAYER, 1), "kit-1-blob");
        snapshot.getPlayerKits().put(IDUtil.getECId(PLAYER, 3), "ec-3-blob");
        return snapshot;
    }

    private byte[] write(KitDataSnapshot snapshot) throws IOException, InvalidKitDataException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        KitDataFile.write(out, snapshot, "1.7.1", 1_700_000_000_000L);
        return out.toByteArray();
    }

    private KitDataFile.ReadResult read(byte[] bytes) throws IOException, InvalidKitDataException {
        return KitDataFile.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void roundTripPreservesAllSections() throws Exception {
        KitDataFile.ReadResult result = read(write(sampleSnapshot()));
        KitDataSnapshot snapshot = result.snapshot();

        assertEquals("1.7.1", result.pluginVersion());
        assertEquals(1_700_000_000_000L, result.createdAtMillis());

        assertEquals("kitroom-page-0-blob", snapshot.getKitRoomPages().get(0));
        assertEquals("kitroom-page-4-blob", snapshot.getKitRoomPages().get(4));
        assertEquals(2, snapshot.getKitRoomPages().size());

        assertEquals(2, snapshot.getPublicKits().size());
        PublicKitEntry warrior = snapshot.getPublicKits().get("warrior");
        assertEquals("Warrior Kit", warrior.name());
        assertEquals("DIAMOND_SWORD", warrior.icon());
        assertEquals("warrior-blob", warrior.data());
        assertNull(snapshot.getPublicKits().get("archer").data());

        assertEquals("kit-1-blob", snapshot.getPlayerKits().get(IDUtil.getPlayerKitId(PLAYER, 1)));
        assertEquals("ec-3-blob", snapshot.getPlayerKits().get(IDUtil.getECId(PLAYER, 3)));
        assertEquals(6, snapshot.totalEntries());
    }

    @Test
    void roundTripWithSingleSection() throws Exception {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(2, "only-page");

        KitDataSnapshot restored = read(write(snapshot)).snapshot();
        assertEquals(1, restored.totalEntries());
        assertEquals("only-page", restored.getKitRoomPages().get(2));
        assertTrue(restored.getPublicKits().isEmpty());
        assertTrue(restored.getPlayerKits().isEmpty());
    }

    @Test
    void sanitizeFileNameAcceptsAndNormalizes() {
        assertEquals("kits.ppk", KitDataFile.sanitizeFileName("kits"));
        assertEquals("kits.ppk", KitDataFile.sanitizeFileName("kits.ppk"));
        assertEquals("kits.ppk", KitDataFile.sanitizeFileName("kits.PPK"));
        assertEquals("my-server_1.0.ppk", KitDataFile.sanitizeFileName("my-server_1.0"));
    }

    @Test
    void sanitizeFileNameRejectsUnsafeNames() {
        assertNull(KitDataFile.sanitizeFileName(null));
        assertNull(KitDataFile.sanitizeFileName(""));
        assertNull(KitDataFile.sanitizeFileName(".ppk"));
        assertNull(KitDataFile.sanitizeFileName("../escape"));
        assertNull(KitDataFile.sanitizeFileName("dir/file"));
        assertNull(KitDataFile.sanitizeFileName("dir\\file"));
        assertNull(KitDataFile.sanitizeFileName(".hidden"));
        assertNull(KitDataFile.sanitizeFileName("a..b"));
        assertNull(KitDataFile.sanitizeFileName("name with spaces"));
        assertNull(KitDataFile.sanitizeFileName("x".repeat(80)));
    }

    @Test
    void writeRejectsEmptySnapshot() {
        assertThrows(InvalidKitDataException.class, () -> write(new KitDataSnapshot()));
    }

    @Test
    void writeRejectsInvalidPublicKitId() {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getPublicKits().put("bad id!",
                new PublicKitEntry("bad id!", "Bad", "CHEST", "blob"));
        assertThrows(InvalidKitDataException.class, () -> write(snapshot));
    }

    @Test
    void writeRejectsInvalidPlayerKitId() {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getPlayerKits().put("not-a-player-id", "blob");
        assertThrows(InvalidKitDataException.class, () -> write(snapshot));
    }

    @Test
    void writeRejectsInvalidKitRoomPage() {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        snapshot.getKitRoomPages().put(99, "blob");
        assertThrows(InvalidKitDataException.class, () -> write(snapshot));
    }

    @Test
    void readRejectsWrongMagic() {
        byte[] bytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(bytes));
        assertTrue(e.getMessage().contains("not a PerPlayerKit"));
    }

    @Test
    void readRejectsNewerFormatVersion() throws Exception {
        byte[] bytes = write(sampleSnapshot());
        // format version is bytes 4..7 (big-endian int right after the magic)
        bytes[7] = 99;
        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(bytes));
        assertTrue(e.getMessage().contains("unsupported format version"));
    }

    @Test
    void readRejectsTruncatedFile() throws Exception {
        byte[] bytes = write(sampleSnapshot());
        byte[] truncated = Arrays.copyOf(bytes, bytes.length / 2);
        assertThrows(InvalidKitDataException.class, () -> read(truncated));
    }

    @Test
    void readRejectsCorruptedContent() throws Exception {
        byte[] bytes = write(sampleSnapshot());
        // flip one bit somewhere in the middle of the payload
        bytes[bytes.length / 2] ^= 0x40;
        assertThrows(InvalidKitDataException.class, () -> read(bytes));
    }

    @Test
    void readRejectsTrailingGarbage() throws Exception {
        byte[] bytes = write(sampleSnapshot());
        byte[] padded = Arrays.copyOf(bytes, bytes.length + 4);
        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(padded));
        assertTrue(e.getMessage().contains("trailing"));
    }

    @Test
    void readRejectsHugeEntryCountWithoutAllocating() throws IOException {
        // Hand-craft a hostile file claiming Integer.MAX_VALUE player kits.
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(KitDataFile.MAGIC);
        out.writeInt(KitDataFile.FORMAT_VERSION);
        out.writeUTF("1.7.1");
        out.writeLong(0L);
        out.writeInt(1);
        out.writeByte(KitDataFile.SECTION_PLAYER_KITS);
        out.writeInt(Integer.MAX_VALUE);

        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(raw.toByteArray()));
        assertTrue(e.getMessage().contains("invalid player kit count"));
    }

    @Test
    void readRejectsHugeBlobLengthWithoutAllocating() throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(KitDataFile.MAGIC);
        out.writeInt(KitDataFile.FORMAT_VERSION);
        out.writeUTF("1.7.1");
        out.writeLong(0L);
        out.writeInt(1);
        out.writeByte(KitDataFile.SECTION_KIT_ROOM);
        out.writeInt(1);
        out.writeByte(0);
        out.writeInt(Integer.MAX_VALUE); // blob length

        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(raw.toByteArray()));
        assertTrue(e.getMessage().contains("invalid data size"));
    }

    @Test
    void readRejectsDuplicateKitRoomPages() throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(KitDataFile.MAGIC);
        out.writeInt(KitDataFile.FORMAT_VERSION);
        out.writeUTF("1.7.1");
        out.writeLong(0L);
        out.writeInt(1);
        out.writeByte(KitDataFile.SECTION_KIT_ROOM);
        out.writeInt(2);
        for (int i = 0; i < 2; i++) {
            out.writeByte(3);
            byte[] blob = "data".getBytes();
            out.writeInt(blob.length);
            out.write(blob);
        }

        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(raw.toByteArray()));
        assertTrue(e.getMessage().contains("duplicate kit room page"));
    }

    @Test
    void readRejectsInvalidPlayerIdInFile() throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(KitDataFile.MAGIC);
        out.writeInt(KitDataFile.FORMAT_VERSION);
        out.writeUTF("1.7.1");
        out.writeLong(0L);
        out.writeInt(1);
        out.writeByte(KitDataFile.SECTION_PLAYER_KITS);
        out.writeInt(1);
        out.writeUTF("kitroom0"); // must not be smuggled in as a player kit
        byte[] blob = "data".getBytes();
        out.writeInt(blob.length);
        out.write(blob);

        InvalidKitDataException e = assertThrows(InvalidKitDataException.class, () -> read(raw.toByteArray()));
        assertTrue(e.getMessage().contains("invalid player kit ID"));
    }

    @Test
    void allNinetyNineKitRoomPagesRoundTrip() throws Exception {
        KitDataSnapshot snapshot = new KitDataSnapshot();
        for (int page = 0; page < 99; page++) snapshot.getKitRoomPages().put(page, "page-" + page);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        KitDataFile.write(bytes, snapshot, "1.8.0", 1);
        var restored = KitDataFile.read(new ByteArrayInputStream(bytes.toByteArray())).snapshot();
        assertEquals(snapshot.getKitRoomPages(), restored.getKitRoomPages());
    }
}
