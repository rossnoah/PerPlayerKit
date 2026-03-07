package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.core.CommandGuards;
import dev.noah.perplayerkit.util.DisabledCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class CommandGuardsTest {

    @Test
    void requirePlayerReturnsPlayerForPlayerSender() {
        Player player = mock(Player.class);

        Player result = CommandGuards.requirePlayer(player);

        assertSame(player, result);
    }

    @Test
    void requirePlayerReturnsNullAndSendsMessageForNonPlayerSender() {
        CommandSender sender = mock(CommandSender.class);

        Player result = CommandGuards.requirePlayer(sender, "Players only");

        assertNull(result);
        verify(sender).sendMessage("Players only");
    }

    @Test
    void requirePlayerInEnabledWorldReturnsNullWhenBlocked() {
        Player player = mock(Player.class);
        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(true);

            Player result = CommandGuards.requirePlayerInEnabledWorld(player);

            assertNull(result);
        }
    }

    @Test
    void requirePlayerInEnabledWorldReturnsPlayerWhenAllowed() {
        Player player = mock(Player.class);
        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            Player result = CommandGuards.requirePlayerInEnabledWorld(player);

            assertSame(player, result);
        }
    }
}
