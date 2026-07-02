package dev.noah.perplayerkit.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KitSlotsTest {

    @AfterEach
    void reset() {
        KitSlots.resetForTesting();
    }

    private Plugin pluginWithMaxKits(int value) {
        Plugin plugin = mock(Plugin.class);
        FileConfiguration config = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("KitSlotsTest"));
        when(config.getInt("max-kits", KitSlots.DEFAULT_LIMIT)).thenReturn(value);
        return plugin;
    }

    @Test
    void defaultsToNineWhenNeverInitialized() {
        assertEquals(9, KitSlots.maxKits());
        assertEquals(1, KitSlots.pageCount());
    }

    @Test
    void readsConfiguredValue() {
        KitSlots.init(pluginWithMaxKits(25));
        assertEquals(25, KitSlots.maxKits());
    }

    @Test
    void clampsValuesBelowMinimum() {
        KitSlots.init(pluginWithMaxKits(0));
        assertEquals(1, KitSlots.maxKits());
    }

    @Test
    void clampsValuesAboveMaximum() {
        KitSlots.init(pluginWithMaxKits(150));
        assertEquals(99, KitSlots.maxKits());
    }

    @Test
    void parsesSlotSuffixes() {
        assertEquals(1, KitSlots.parseSlotSuffix("1"));
        assertEquals(9, KitSlots.parseSlotSuffix("9"));
        assertEquals(10, KitSlots.parseSlotSuffix("10"));
        assertEquals(99, KitSlots.parseSlotSuffix("99"));

        assertNull(KitSlots.parseSlotSuffix(null));
        assertNull(KitSlots.parseSlotSuffix(""));
        assertNull(KitSlots.parseSlotSuffix("0"));
        assertNull(KitSlots.parseSlotSuffix("05"));
        assertNull(KitSlots.parseSlotSuffix("100"));
        assertNull(KitSlots.parseSlotSuffix("1a"));
        assertNull(KitSlots.parseSlotSuffix("-1"));
    }

    @Test
    void computesPageOfSlot() {
        assertEquals(0, KitSlots.pageOf(1));
        assertEquals(0, KitSlots.pageOf(9));
        assertEquals(1, KitSlots.pageOf(10));
        assertEquals(1, KitSlots.pageOf(18));
        assertEquals(10, KitSlots.pageOf(99));
    }

    @Test
    void computesPageCount() {
        KitSlots.setForTesting(9);
        assertEquals(1, KitSlots.pageCount());
        KitSlots.setForTesting(10);
        assertEquals(2, KitSlots.pageCount());
        KitSlots.setForTesting(25);
        assertEquals(3, KitSlots.pageCount());
        KitSlots.setForTesting(99);
        assertEquals(11, KitSlots.pageCount());
    }
}
