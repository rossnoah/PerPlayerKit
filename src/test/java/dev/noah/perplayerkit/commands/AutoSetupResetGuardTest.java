package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.admin.PerPlayerKitCommand;
import dev.noah.perplayerkit.starter.StarterSetup;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code autosetup reset} replaces every kit room page and public kit, so it is
 * restricted to the server console: not reachable in game by accident, and not
 * reachable at all by whoever holds an admin account.
 */
class AutoSetupResetGuardTest {

    private final Command command = mock(Command.class);

    @BeforeEach
    void setUp() {
        Lang.installForTesting();
    }

    @AfterEach
    void tearDown() {
        Lang.resetForTesting();
    }

    private void runReset(CommandSender sender) {
        new PerPlayerKitCommand(null)
                .onCommand(sender, command, "perplayerkit", new String[]{"autosetup", "reset", "confirm"});
    }

    @Test
    void playerCannotReset() {
        Player player = mock(Player.class);

        try (MockedStatic<StarterSetup> starter = mockStatic(StarterSetup.class)) {
            runReset(player);
            // Refused before anything is touched.
            starter.verify(StarterSetup::get, never());
        }

        verify(player).sendMessage(any(String.class));
    }

    /** A non-player, non-console sender - a command block or RCON - is refused too. */
    @Test
    void otherSendersCannotReset() {
        CommandSender other = mock(CommandSender.class);

        try (MockedStatic<StarterSetup> starter = mockStatic(StarterSetup.class)) {
            runReset(other);
            starter.verify(StarterSetup::get, never());
        }
    }

    @Test
    void consoleReachesTheReset() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);

        try (MockedStatic<StarterSetup> starter = mockStatic(StarterSetup.class)) {
            StarterSetup setup = mock(StarterSetup.class);
            when(setup.apply(true)).thenReturn(new StarterSetup.Result(List.of(0), List.of("crystal")));
            starter.when(StarterSetup::get).thenReturn(setup);

            runReset(console);

            verify(setup).apply(true);
        }
    }

    @Test
    void resetIsNotSuggestedToPlayers() {
        PerPlayerKitCommand cmd = new PerPlayerKitCommand(null);
        String[] args = {"autosetup", ""};

        List<String> forPlayer = cmd.onTabComplete(mock(Player.class), command, "perplayerkit", args);
        List<String> forConsole = cmd.onTabComplete(mock(ConsoleCommandSender.class), command, "perplayerkit", args);

        assertFalse(forPlayer.contains("reset"), "players should not be offered reset");
        assertEquals(List.of("reset"), forConsole);
    }
}
