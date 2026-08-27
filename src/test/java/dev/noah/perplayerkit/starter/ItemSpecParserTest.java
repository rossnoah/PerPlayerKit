package dev.noah.perplayerkit.starter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemSpecParserTest {

    @Test
    void parsesBareMaterial() {
        ItemSpec spec = ItemSpecParser.parse("TOTEM_OF_UNDYING");

        assertEquals("TOTEM_OF_UNDYING", spec.getMaterial());
        assertEquals(1, spec.getAmount());
        assertNull(spec.getPotionType());
        assertNull(spec.getDisplayName());
        assertEquals(Map.of(), spec.getEnchantments());
    }

    @Test
    void parsesAmount() {
        assertEquals(64, ItemSpecParser.parse("END_CRYSTAL x64").getAmount());
        assertEquals(16, ItemSpecParser.parse("ENDER_PEARL X16").getAmount());
    }

    @Test
    void lowercaseMaterialIsNormalised() {
        assertEquals("END_CRYSTAL", ItemSpecParser.parse("end_crystal x8").getMaterial());
    }

    @Test
    void parsesEnchantmentsInOrder() {
        ItemSpec spec = ItemSpecParser.parse("NETHERITE_BOOTS { protection=4, unbreaking=3, mending=1 }");

        assertEquals(List.of("protection", "unbreaking", "mending"), List.copyOf(spec.getEnchantments().keySet()));
        assertEquals(4, spec.getEnchantments().get("protection"));
        assertEquals(1, spec.getEnchantments().get("mending"));
    }

    @Test
    void parsesPotionAndAmountTogether() {
        ItemSpec spec = ItemSpecParser.parse("TIPPED_ARROW x64 { potion=strong_harming }");

        assertEquals("TIPPED_ARROW", spec.getMaterial());
        assertEquals(64, spec.getAmount());
        assertEquals("STRONG_HARMING", spec.getPotionType());
        assertEquals(Map.of(), spec.getEnchantments());
    }

    @Test
    void quotedNameKeepsCommas() {
        ItemSpec spec = ItemSpecParser.parse("DIAMOND_SWORD { name=\"<red>Sharp, Very\", sharpness=5 }");

        assertEquals("<red>Sharp, Very", spec.getDisplayName());
        assertEquals(Map.of("sharpness", 5), spec.getEnchantments());
    }

    @Test
    void rejectsMalformedDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse("STONE x"));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse("STONE x0"));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse("STONE 64"));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse("STONE { protection }"));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse("STONE { protection=high }"));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parse("STONE { protection=4"));
    }

    @Test
    void parsesSingleSlot() {
        assertEquals(List.of(12), ItemSpecParser.parseSlots("12", Map.of()));
    }

    @Test
    void parsesInclusiveRange() {
        assertEquals(List.of(9, 10, 11), ItemSpecParser.parseSlots("9-11", Map.of()));
    }

    @Test
    void parsesNamedSlot() {
        assertEquals(List.of(39), ItemSpecParser.parseSlots("HELMET", Map.of("helmet", 39)));
    }

    @Test
    void parsesFallbackChainBestFirst() {
        List<ItemSpec> options = ItemSpecParser.parseAll(
                "MACE { density=5 } || NETHERITE_AXE { sharpness=5 } || IRON_AXE");

        assertEquals(3, options.size());
        assertEquals("MACE", options.get(0).getMaterial());
        assertEquals(Map.of("density", 5), options.get(0).getEnchantments());
        assertEquals("NETHERITE_AXE", options.get(1).getMaterial());
        assertEquals("IRON_AXE", options.get(2).getMaterial());
    }

    /** A plain item is just a chain of one, so callers only need the one path. */
    @Test
    void parsesSingleItemAsOneOption() {
        List<ItemSpec> options = ItemSpecParser.parseAll("END_CRYSTAL x64");

        assertEquals(1, options.size());
        assertEquals("END_CRYSTAL", options.get(0).getMaterial());
        assertEquals(64, options.get(0).getAmount());
    }

    /** A display name may contain anything, including the separator. */
    @Test
    void doesNotSplitInsideQuotedNames() {
        List<ItemSpec> options = ItemSpecParser.parseAll("STICK { name=\"a || b\" }");

        assertEquals(1, options.size());
        assertEquals("a || b", options.get(0).getDisplayName());
    }

    @Test
    void rejectsBadSlotKeys() {
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parseSlots("", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parseSlots("helmet", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parseSlots("11-9", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ItemSpecParser.parseSlots("-4", Map.of()));
    }
}
