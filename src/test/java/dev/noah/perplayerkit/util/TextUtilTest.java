package dev.noah.perplayerkit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextUtilTest {

    @Test
    void toLegacyKeepsGradientHexColors() {
        assertEquals("§x§9§f§4§3§e§f§lL§x§e§d§a§1§f§f§lT§x§9§f§4§3§e§f§lC",
                TextUtil.toLegacy("<b><gradient:#9F43EF:#EDA1FF:#9F43EF>LTC</gradient></b>"));
    }

    @Test
    void toLegacyKeepsTextContainingTheOldMarkerWord() {
        assertEquals("§cContest", TextUtil.toLegacy("<red>Contest"));
    }

    @Test
    void toLegacyKeepsLeadingReset() {
        assertEquals("§rCrystal", TextUtil.toLegacy("<reset>Crystal"));
    }

    @Test
    void formattingPrefixReturnsCodesWithoutText() {
        assertEquals("§9§l", TextUtil.formattingPrefix("<blue><bold>"));
        assertEquals("§x§1§2§3§4§5§6", TextUtil.formattingPrefix("<#123456>"));
    }

    @Test
    void parseAcceptsSectionSignCodes() {
        assertEquals("<reset><red>Noah<reset><#ff0000>!<bold>x",
                TextUtil.legacyCodesToTags("§cNoah§x§F§F§0§0§0§0!§lx"));
        assertEquals("Noah loaded a kit", TextUtil.LEGACY
                .serialize(TextUtil.parse("<gray>§cNoah§r loaded a kit§")).replaceAll("§.", ""));
    }
}
