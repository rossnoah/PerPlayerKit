package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.shortcuts.AbstractShortSlotCommand;
import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.KitSlots;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class AbstractShortSlotCommandTest {

    @BeforeAll
    static void setupLang() {
        Lang.installForTesting();
    }

    @AfterAll
    static void tearDownLang() {
        Lang.resetForTesting();
    }

    @AfterEach
    void resetKitSlots() {
        KitSlots.resetForTesting();
    }

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

        verify(player).sendMessage(contains("Invalid command label"));
        assertNull(command.executedSlot);
    }

    @Test
    void executesForMultiDigitLabelWithinConfiguredMax() {
        KitSlots.setForTesting(20);
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "k12", new String[0]);
        }

        assertEquals(12, command.executedSlot);
    }

    @Test
    void executesForNamespacedLabel() {
        KitSlots.setForTesting(20);
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "perplayerkit:k10", new String[0]);
        }

        assertEquals(10, command.executedSlot);
    }

    @Test
    void rejectsSlotAboveConfiguredMaxWithRangeError() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "k10", new String[0]);
        }

        verify(player).sendMessage(contains("between 1 and 9"));
        assertNull(command.executedSlot);
    }

    @Test
    void rejectsLeadingZeroAndOversizedSuffixes() {
        KitSlots.setForTesting(99);
        TestShortSlotCommand command = new TestShortSlotCommand();
        Player player = mock(Player.class);

        try (MockedStatic<DisabledCommand> disabledCommand = mockStatic(DisabledCommand.class)) {
            disabledCommand.when(() -> DisabledCommand.isBlockedInWorld(player)).thenReturn(false);

            command.onCommand(player, null, "k012", new String[0]);
            command.onCommand(player, null, "k05", new String[0]);
            command.onCommand(player, null, "k100", new String[0]);
        }

        assertNull(command.executedSlot);
    }

    @Test
    void nonPlayerSenderGetsOnlyPlayersMessage() {
        TestShortSlotCommand command = new TestShortSlotCommand();
        CommandSender sender = mock(CommandSender.class);

        command.onCommand(sender, null, "k1", new String[0]);

        verify(sender).sendMessage(contains("Only players can use this command"));
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
