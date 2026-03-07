package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.shortcuts.AbstractShortSlotCommand;
import dev.noah.perplayerkit.util.DisabledCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class AbstractShortSlotCommandTest {

    @Test
    void executesForShortPrefixLabel() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "k4", new String[0]);
        }

        assertEquals(4, command.executedSlot);
    }

    @Test
    void executesForLongPrefixLabel() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "KIT9", new String[0]);
        }

        assertEquals(9, command.executedSlot);
    }

    @Test
    void doesNotExecuteWhenBlockedWorld() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(true);

            command.onCommand(player, null, "k3", new String[0]);
        }

        assertNull(command.executedSlot);
    }

    @Test
    void sendsErrorForInvalidLabel() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "k0", new String[0]);
        }

        verify(player).sendMessage("Invalid command label.");
        assertNull(command.executedSlot);
    }

    @Test
    void nonPlayerSenderGetsOnlyPlayersMessage() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        CommandSender sender = mock(CommandSender.class);

        command.onCommand(sender, null, "k1", new String[0]);

        verify(sender).sendMessage("Only players can use this command.");
        assertNull(command.executedSlot);
    }

    private static class TestShortSlotCommand extends AbstractShortSlotCommand {
        private Integer executedSlot;

        private TestShortSlotCommand() {
            super("k", "kit");
        }

        @Override
        protected void executeForSlot(Player player, int slot) {
            this.executedSlot = slot;
        }
    }
}
