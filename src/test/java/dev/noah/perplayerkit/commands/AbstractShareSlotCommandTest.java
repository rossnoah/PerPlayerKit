package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.commands.share.AbstractShareSlotCommand;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    static void setupLang() {
        Lang.installForTesting();
    }

    @AfterAll
    static void tearDownLang() {
        Lang.resetForTesting();
    }

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

        verify(sender).sendMessage(contains("Only players can use this command"));
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

    @Test
    void playerArgumentRoutesToRequestAction() {
        ShareRecorder recorder = new ShareRecorder();
        AbstractShareSlotCommand command = new TestShareSlotCommand(recorder);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Player target = mock(Player.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Steve")).thenReturn(target);

            command.onCommand(player, null, "sharekit", new String[]{"2", "Steve"});
        }

        assertEquals(2, recorder.lastRequestedSlot);
        assertEquals(target, recorder.lastRequestTarget);
        assertEquals(0, recorder.executionCount);
    }

    @Test
    void unknownTargetPlayerDoesNotExecuteAnyAction() {
        ShareRecorder recorder = new ShareRecorder();
        AbstractShareSlotCommand command = new TestShareSlotCommand(recorder);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SoundManager> soundManager = mockStatic(SoundManager.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Nobody")).thenReturn(null);

            command.onCommand(player, null, "sharekit", new String[]{"2", "Nobody"});

            verify(player).sendMessage(contains("isn't online"));
            soundManager.verify(() -> SoundManager.playFailure(player));
        }

        assertNull(recorder.lastRequestedSlot);
        assertNull(recorder.lastRequestTarget);
        assertEquals(0, recorder.executionCount);
    }

    private static class TestShareSlotCommand extends AbstractShareSlotCommand {
        private TestShareSlotCommand(ShareRecorder recorder) {
            super("error.missing-kit-slot-share",
                    (player, slot) -> {
                        recorder.lastSharedSlot = slot;
                        recorder.executionCount++;
                    },
                    (player, slot, target) -> {
                        recorder.lastRequestedSlot = slot;
                        recorder.lastRequestTarget = target;
                    });
        }
    }

    private static class ShareRecorder {
        private Integer lastSharedSlot;
        private int executionCount;
        private Integer lastRequestedSlot;
        private Player lastRequestTarget;
    }
}
