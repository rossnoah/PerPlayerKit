package dev.noah.perplayerkit;

import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateCheckerHttpTest {
    @Test void slowHttpResponseDoesNotBlockTheCaller() throws Exception {
        CountDownLatch received = new CountDownLatch(1), respond = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService executor = Executors.newSingleThreadExecutor(); server.setExecutor(executor);
        server.createContext("/latest", exchange -> {
            received.countDown();
            try {
                if (!respond.await(5, TimeUnit.SECONDS)) return;
                byte[] body = "1.9.0\n".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("PerPlayerKit", "1.8.0", "test.Plugin"));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("update-http-test"));
        OkHttpClient client = new OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build();
        UpdateChecker checker = new UpdateChecker(plugin, client, "http://127.0.0.1:" + server.getAddress().getPort() + "/latest", System::nanoTime);
        server.start();
        try {
            var result = assertTimeout(Duration.ofSeconds(2), checker::checkForUpdateAsync);
            assertTrue(received.await(3, TimeUnit.SECONDS));
            assertFalse(result.isDone(), "The caller returned while the HTTP response was still blocked");
            respond.countDown(); assertTrue(result.get(3, TimeUnit.SECONDS));
        } finally { respond.countDown(); checker.close(); server.stop(0); executor.shutdownNow(); }
    }
}
