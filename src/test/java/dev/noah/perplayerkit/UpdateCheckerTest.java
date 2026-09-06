package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.Lang;
import okhttp3.*;
import okhttp3.MediaType;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateCheckerTest {
    Plugin plugin;
    OkHttpClient client;
    UpdateChecker checker;
    AtomicLong clock;
    Player player;
    YamlConfiguration config;
    Logger logger;
    Lang messages;
    MockedStatic<Lang> language;
    final List<Call> calls = new ArrayList<>();
    final List<Callback> callbacks = new ArrayList<>();
    final List<Runnable> mainTasks = new ArrayList<>();

    @BeforeEach void setup() {
        plugin = mock(Plugin.class); logger = mock(Logger.class); when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("PerPlayerKit", "1.8.0", "test.Plugin"));
        when(plugin.isEnabled()).thenReturn(true);
        config = new YamlConfiguration(); when(plugin.getConfig()).thenReturn(config);
        Server server = mock(Server.class); BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server); when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(i -> { mainTasks.add(i.getArgument(1)); return null; }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        player = mock(Player.class); UUID uuid = UUID.randomUUID(); when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true); when(player.hasPermission("perplayerkit.admin")).thenReturn(true);
        when(server.getPlayer(uuid)).thenReturn(player);
        client = mock(OkHttpClient.class, RETURNS_DEEP_STUBS);
        when(client.newCall(any())).thenAnswer(i -> {
            Call call = mock(Call.class); calls.add(call);
            doAnswer(c -> { callbacks.add(c.getArgument(0)); return null; }).when(call).enqueue(any());
            return call;
        });
        clock = new AtomicLong(); checker = new UpdateChecker(plugin, client, "https://example.invalid/latest", clock::get);
        messages = mock(Lang.class); language = mockStatic(Lang.class); language.when(Lang::get).thenReturn(messages);
    }
    @AfterEach void close() { checker.close(); language.close(); }

    Response reply(int code, String version) throws IOException {
        ResponseBody body = spy(ResponseBody.create(version, MediaType.get("text/plain")));
        Response response = spy(new Response.Builder().request(new Request.Builder().url("https://example.invalid/latest").build())
                .protocol(Protocol.HTTP_1_1).code(code).message("fixture").body(body).build());
        callbacks.get(callbacks.size()-1).onResponse(calls.get(calls.size()-1), response);
        verify(response).close();
        return response;
    }

    @Test void startupAndJoinsShareOneNonblockingRequestAndNotifyOnTheServerThread() throws Exception {
        checker.printStartupStatus(); checker.sendUpdateMessage(player);
        var future = checker.checkForUpdateAsync(); assertFalse(checker.checkForUpdate());
        assertEquals(1, calls.size()); assertFalse(future.isDone()); verify(calls.get(0), never()).execute();
        verifyNoInteractions(messages); assertTrue(mainTasks.isEmpty());
        reply(200, "  1.9.0\n");
        assertTrue(future.join()); assertTrue(checker.checkForUpdate()); assertEquals(1, calls.size());
        verifyNoInteractions(messages);
        mainTasks.forEach(Runnable::run);
        verify(messages).send(player, "update.new-version-available", "current", "1.8.0", "latest", "1.9.0");
    }
    @ParameterizedTest @ValueSource(strings={"offline", "permission", "setting", "rejoined", "disabled", "closed"})
    void delayedNotificationsRecheckPlayerAndPluginState(String reason) throws Exception {
        checker.sendUpdateMessage(player); reply(200, "1.9.0"); assertEquals(1, mainTasks.size());
        switch (reason) {
            case "offline" -> when(player.isOnline()).thenReturn(false);
            case "permission" -> when(player.hasPermission("perplayerkit.admin")).thenReturn(false);
            case "setting" -> config.set("updates.notify-admins-on-join", false);
            case "rejoined" -> when(plugin.getServer().getPlayer(player.getUniqueId())).thenReturn(mock(Player.class));
            case "disabled" -> when(plugin.isEnabled()).thenReturn(false);
            case "closed" -> checker.close();
        }
        mainTasks.forEach(Runnable::run); verifyNoInteractions(messages);
    }
    @Test void failedChecksDoNotClaimLatestAndRetryAfterOneMinute() throws Exception {
        checker.printStartupStatus();
        callbacks.get(0).onFailure(calls.get(0), new IOException("timed out"));
        verify(logger, never()).info(anyString()); verify(logger).warning(contains("timed out"));
        assertFalse(checker.checkForUpdateAsync().join()); assertEquals(1, calls.size());
        clock.set(TimeUnit.MINUTES.toNanos(1));
        var retry = checker.checkForUpdateAsync(); assertEquals(2, calls.size());
        reply(200, "1.9.0"); assertTrue(retry.join());
    }
    @ParameterizedTest @ValueSource(strings={"", "<html>maintenance</html>", "1.9.0-preview", "1.9.0 trailing data"})
    void invalidResponsesAreClosedAndRemainRetryable(String body) throws Exception {
        var result = checker.checkForUpdateAsync(); reply(200, body); assertFalse(result.join());
        clock.set(TimeUnit.MINUTES.toNanos(1)); assertFalse(checker.checkForUpdateAsync().isDone()); assertEquals(2, calls.size());
    }
    @Test void httpErrorsCloseTheirResponseAndDoNotSendMessages() throws Exception {
        checker.printStartupStatus(); checker.sendUpdateMessage(player);
        reply(503, "maintenance"); verify(logger, never()).info(anyString());
        assertTrue(mainTasks.isEmpty()); verifyNoInteractions(messages);
    }
    @Test void successfulChecksRefreshAfterOneHour() throws Exception {
        var result = checker.checkForUpdateAsync(); reply(200, "1.8.0"); assertFalse(result.join());
        clock.set(TimeUnit.MINUTES.toNanos(59)); assertFalse(checker.checkForUpdateAsync().join()); assertEquals(1, calls.size());
        clock.set(TimeUnit.HOURS.toNanos(1)); var refreshed = checker.checkForUpdateAsync();
        reply(200, "1.9.0"); assertTrue(refreshed.join()); assertEquals(2, calls.size());
    }
    @Test void disablingCancelsPendingWorkAndIgnoresLateResponses() throws Exception {
        checker.printStartupStatus(); checker.sendUpdateMessage(player); var future = checker.checkForUpdateAsync();
        checker.close(); assertFalse(future.join()); verify(calls.get(0)).cancel();
        reply(200, "1.9.0"); assertTrue(mainTasks.isEmpty()); verifyNoInteractions(messages); verify(logger, never()).info(anyString());
        assertFalse(checker.checkForUpdateAsync().join()); assertEquals(1, calls.size());
    }
    @ParameterizedTest @CsvSource({
        "1.8.0,1.9.0,true", "1.8.0,1.7.1,false", "1.8,1.8.0,false", "1.8.0,1.8,false",
        "1.8.0,1.8.0.1,true", "1.8.0-SNAPSHOT,1.8.0,true", "1.9.0-SNAPSHOT,1.8.0,false",
        "1.8.0+build.4,1.8.0,false", "1.8.0-rc.1+build.4,1.8.0,true", "1.8.0,999999999999999999999.0,true"})
    void comparesPublishedNumericVersionsWithoutOverflowOrFalsePatchUpdates(String current, String latest, boolean expected) {
        assertEquals(expected, UpdateChecker.isSemanticallyNewer(current, latest));
    }
}
