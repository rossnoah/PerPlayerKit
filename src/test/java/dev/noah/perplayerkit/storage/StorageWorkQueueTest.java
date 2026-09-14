package dev.noah.perplayerkit.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(5)
class StorageWorkQueueTest {
    @Test void laterReadsObserveEarlierWritesAndCloseDrainsTheQueue() throws Exception {
        StorageWorkQueue queue = new StorageWorkQueue(mock(Logger.class));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> order = new ArrayList<>();
        queue.run(() -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException e) { throw new IllegalStateException(e); }
            order.add("write");
        });
        assertTrue(entered.await(1, TimeUnit.SECONDS));
        queue.run(() -> order.add("delete"));
        CompletableFuture<List<String>> read = queue.supply(() -> List.copyOf(order));
        CountDownLatch closing = new CountDownLatch(1);
        CompletableFuture<Void> closed = new CompletableFuture<>();
        Thread closer = new Thread(() -> {
            closing.countDown();
            try { queue.close(); closed.complete(null); }
            catch (Throwable error) { closed.completeExceptionally(error); }
        });
        closer.start();
        try {
            assertTrue(closing.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> closed.get(100, TimeUnit.MILLISECONDS), "close returned while a write was pending");
            assertFalse(read.isDone());
            release.countDown();
            closer.join(2_000);
            assertFalse(closer.isAlive(), "close must finish after draining accepted work");
            assertEquals(List.of("write", "delete"), read.get(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            queue.close();
        }
    }

    @Test void failedWorkIsReportedWithoutBlockingLaterOperations() {
        Logger logger = mock(Logger.class);
        try (StorageWorkQueue queue = new StorageWorkQueue(logger)) {
            IllegalStateException failed = new IllegalStateException("fixture failure");
            CompletableFuture<Void> failure = queue.run(() -> { throw failed; });
            assertThrows(CompletionException.class, failure::join);
            assertEquals("next read", queue.supply(() -> "next read").join());
            verify(logger).log(eq(Level.SEVERE), contains("fixture failure"), same(failed));
        }
    }

    @Test void aClosedQueueCannotSilentlyAcceptMoreWrites() {
        StorageWorkQueue queue = new StorageWorkQueue(mock(Logger.class));
        queue.close();
        assertThrows(RejectedExecutionException.class, () -> queue.run(() -> fail("write ran after close")));
    }
}
