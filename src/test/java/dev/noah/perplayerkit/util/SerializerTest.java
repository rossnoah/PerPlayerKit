package dev.noah.perplayerkit.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {
    private String encoded(int count, Object... items) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(count);
            for (Object item : items) output.writeObject(item);
            output.flush();
            return Base64.getMimeEncoder().encodeToString(bytes.toByteArray());
        }
    }

    @Test void emptySlotsKeepTheirLayoutThroughARoundTrip() throws IOException {
        ItemStack[] original = new ItemStack[45];
        assertArrayEquals(original, Serializer.itemStackArrayFromBase64(Serializer.itemStackArrayToBase64(original)));
    }

    @Test void legacyMimeLineBreaksRemainReadable() throws IOException {
        String data = encoded(100, new Object[100]);
        assertTrue(data.contains("\r\n"));
        assertEquals(100, Serializer.itemStackArrayFromBase64(data).length);
    }

    @Test void invalidLengthsFailWithoutAllocatingTheRequestedArray() throws IOException {
        for (int size : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            String data = encoded(size);
            assertThrows(IOException.class, () -> Serializer.itemStackArrayFromBase64(data));
        }
    }

    @Test void missingAndTruncatedDataReportACheckedLoadFailure() throws IOException {
        for (String data : new String[]{null, "", "x", "definitely not a serialized inventory", encoded(2, (Object) null)}) {
            assertThrows(IOException.class, () -> Serializer.itemStackArrayFromBase64(data));
        }
    }

    @Test void nonItemObjectsAreRejectedAsCorruptKitData() throws IOException {
        String data = encoded(1, "not an item");
        assertThrows(IOException.class, () -> Serializer.itemStackArrayFromBase64(data));
    }
}
