package dev.noah.perplayerkit.starter;

import dev.noah.perplayerkit.util.Lang;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These go to an admin on a server that has no kit room yet, and they carry the
 * clickable autosetup offer. A broken MiniMessage tag would only show up at that
 * exact moment, so check them here.
 */
class WelcomeMessageTest {

    private static final String AUTOSETUP = "/perplayerkit autosetup";

    @BeforeEach
    void setUp() {
        Lang.installForTesting();
    }

    @AfterEach
    void tearDown() {
        Lang.resetForTesting();
    }

    private List<String> lines(String key) {
        List<String> lines = Lang.get().rawList(key);
        assertFalse(lines.isEmpty(), key + " is missing from en.yml");
        return lines;
    }

    @Test
    void welcomeLinesParseAsMiniMessage() {
        for (String key : List.of("welcome.first-admin", "welcome.setup-reminder")) {
            for (String line : lines(key)) {
                assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(line),
                        () -> key + " does not parse: " + line);
            }
        }
    }

    /** The offer is the whole point of the message, and it has to be clickable. */
    @Test
    void bothMessagesOfferAutosetupAsAClickableCommand() {
        for (String key : List.of("welcome.first-admin", "welcome.setup-reminder")) {
            String all = String.join("\n", lines(key));
            assertTrue(all.contains("click:run_command:'" + AUTOSETUP + "'"),
                    key + " should run " + AUTOSETUP + " on click");
        }
    }

    @Test
    void firstAdminMessageOrientsAsWellAsOffers() {
        String all = String.join("\n", lines("welcome.first-admin"));

        assertTrue(all.contains(StarterSetup.DOCS_URL), "welcome should link the docs");
        assertTrue(all.contains("/kit "), "welcome should mention the kit menu");
    }

    /**
     * /kitroom is the save/load admin command - it does not open an editor, and
     * with no arguments it just prints a usage error. The kit room GUI is only
     * reachable from inside /kit, so pointing an admin at /kitroom sends them
     * somewhere that looks broken.
     */
    @Test
    void welcomeDoesNotSendAdminsToTheKitroomCommand() {
        for (String key : List.of("welcome.first-admin", "welcome.setup-reminder")) {
            String all = String.join("\n", lines(key));
            assertFalse(all.contains("/kitroom"), key + " should not point at /kitroom");
        }
    }

    /**
     * Nothing in plugin.yml declares a default, so a non-op has no PerPlayerKit
     * access at all until a group is granted these. An admin who is not told
     * that ends up with a plugin only they can see.
     */
    @Test
    void firstAdminMessageNamesEveryPermissionTier() {
        String all = String.join("\n", lines("welcome.first-admin"));

        for (String node : List.of("perplayerkit.use", "perplayerkit.staff", "perplayerkit.admin")) {
            assertTrue(all.contains(node), "welcome should name " + node);
        }
    }
}
