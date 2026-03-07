package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.core.SlotArgumentParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SlotArgumentParserTest {

    @Test
    void parseSlotReturnsIntegerForNumericInput() {
        assertEquals(7, SlotArgumentParser.parseSlot("7"));
    }

    @Test
    void parseSlotReturnsNullForNonNumericInput() {
        assertNull(SlotArgumentParser.parseSlot("abc"));
    }

    @Test
    void parseSlotInRangeReturnsIntegerInsideRange() {
        assertEquals(3, SlotArgumentParser.parseSlotInRange("3", 1, 9));
    }

    @Test
    void parseSlotInRangeReturnsNullOutsideRange() {
        assertNull(SlotArgumentParser.parseSlotInRange("10", 1, 9));
    }
}
