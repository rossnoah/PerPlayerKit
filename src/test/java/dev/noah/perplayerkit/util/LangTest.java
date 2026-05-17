package dev.noah.perplayerkit.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LangTest {

    @BeforeEach
    void setUp() {
        Lang.installForTesting();
    }

    @AfterEach
    void tearDown() {
        Lang.resetForTesting();
    }

    @Test
    void rawReturnsKeyWhenMissing() {
        String missing = Lang.get().raw("nonexistent.key");
        assertEquals("nonexistent.key", missing);
    }

    @Test
    void rawSubstitutesNamedPlaceholders() {
        String result = Lang.get().raw("success.kit-saved", "slot", "3");
        assertEquals("<green>Kit 3 saved!", result);
    }

    @Test
    void rawSubstitutesMultiplePlaceholders() {
        String result = Lang.get().raw("success.kits-swapped", "slot1", "1", "slot2", "5");
        assertEquals("<green>Kits 1 and 5 have been swapped!", result);
    }

    @Test
    void splitTemplateReturnsPrefixAndSuffix() {
        String[] parts = Lang.get().splitTemplate("gui.kit-editor-title", "slot");
        assertEquals("Kit: ", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitTemplateOnUnknownPlaceholderReturnsWholeTemplate() {
        String[] parts = Lang.get().splitTemplate("gui.kit-editor-title", "bogus");
        assertEquals("Kit: {slot}", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void componentRendersMiniMessage() {
        // Just verify it doesn't throw and returns non-null
        assertNotNull(Lang.get().component("error.players-only"));
    }
}
