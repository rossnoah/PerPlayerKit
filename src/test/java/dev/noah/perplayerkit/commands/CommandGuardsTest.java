package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.core.CommandGuards;
import dev.noah.perplayerkit.util.LocationAccess;
import dev.noah.perplayerkit.util.LocationFeature;
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
    void requirePlayerAtLocationReturnsNullWhenBlocked() {
        Player player = mock(Player.class);
        try (MockedStatic<LocationAccess> disabledCommand = mockStatic(LocationAccess.class)) {
            LocationAccess access = mock(LocationAccess.class);
            disabledCommand.when(LocationAccess::get).thenReturn(access);
            org.mockito.Mockito.when(access.require(player, LocationFeature.KITS)).thenReturn(false);

            Player result = CommandGuards.requirePlayerAtLocation(player, LocationFeature.KITS);

            assertNull(result);
        }
    }

    @Test
    void requirePlayerAtLocationReturnsPlayerWhenAllowed() {
        Player player = mock(Player.class);
        try (MockedStatic<LocationAccess> disabledCommand = mockStatic(LocationAccess.class)) {
            LocationAccess access = mock(LocationAccess.class);
            disabledCommand.when(LocationAccess::get).thenReturn(access);
            org.mockito.Mockito.when(access.require(player, LocationFeature.KITS)).thenReturn(true);

            Player result = CommandGuards.requirePlayerAtLocation(player, LocationFeature.KITS);

            assertSame(player, result);
        }
    }
}
