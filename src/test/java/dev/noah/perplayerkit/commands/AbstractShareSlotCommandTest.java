package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.share.AbstractShareSlotCommand;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class AbstractShareSlotCommandTest {

    @Test
    void executesActionForValidSlot() {
        ShareRecorder recorder = new ShareRecorder();
        AbstractShareSlotCommand command = new TestShareSlotCommand(recorder);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        command.onCommand(player, null, "sharekit", new String[]{"2"});

        assertEquals(2, recorder.lastSharedSlot);
        assertEquals(1, recorder.executionCount);
    }

    @Test
    void nonPlayerSenderGetsOnlyPlayersMessage() {
        ShareRecorder recorder = new ShareRecorder();
        AbstractShareSlotCommand command = new TestShareSlotCommand(recorder);
        CommandSender sender = mock(CommandSender.class);

        command.onCommand(sender, null, "sharekit", new String[]{"2"});

        verify(sender).sendMessage("Only players can use this command");
        assertNull(recorder.lastSharedSlot);
        assertEquals(0, recorder.executionCount);
    }

    @Test
    void invalidSlotDoesNotExecuteAction() {
        ShareRecorder recorder = new ShareRecorder();
        AbstractShareSlotCommand command = new TestShareSlotCommand(recorder);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        try (MockedStatic<SoundManager> soundManager = mockStatic(SoundManager.class)) {
            command.onCommand(player, null, "sharekit", new String[]{"12"});

            verify(player).sendMessage(contains("Select a valid kit slot"));
            soundManager.verify(() -> SoundManager.playFailure(player));
        }

        assertNull(recorder.lastSharedSlot);
        assertEquals(0, recorder.executionCount);
    }

    @Test
    void cooldownPreventsImmediateSecondExecution() {
        ShareRecorder recorder = new ShareRecorder();
        AbstractShareSlotCommand command = new TestShareSlotCommand(recorder);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        command.onCommand(player, null, "sharekit", new String[]{"3"});

        try (MockedStatic<SoundManager> soundManager = mockStatic(SoundManager.class)) {
            command.onCommand(player, null, "sharekit", new String[]{"3"});

            verify(player).sendMessage(contains("Please don't spam the command"));
            soundManager.verify(() -> SoundManager.playFailure(player));
        }

        assertEquals(3, recorder.lastSharedSlot);
        assertEquals(1, recorder.executionCount);
    }

    private static class TestShareSlotCommand extends AbstractShareSlotCommand {
        private TestShareSlotCommand(ShareRecorder recorder) {
            super("Error, missing slot", (player, slot) -> {
                recorder.lastSharedSlot = slot;
                recorder.executionCount++;
            });
        }
    }

    private static class ShareRecorder {
        private Integer lastSharedSlot;
        private int executionCount;
    }
}
