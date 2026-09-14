package dev.noah.perplayerkit.storage;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Orders kit writes, deletes and subsequent join reads; drains before storage closes. */
public final class StorageWorkQueue implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PerPlayerKit-storage");
        thread.setDaemon(true);
        return thread;
    });
    private final Logger logger;

    public StorageWorkQueue(Logger logger) { this.logger = logger; }

    public CompletableFuture<Void> run(Runnable work) {
        return supply(() -> { work.run(); return null; });
    }

    public <T> CompletableFuture<T> supply(Supplier<T> work) {
        return CompletableFuture.supplyAsync(work, executor).whenComplete((value, error) -> {
            if (error == null) return;
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            logger.log(Level.SEVERE, "Storage operation failed: " + cause.getMessage(), cause);
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            // Closing the connection while writes are pending loses accepted saves.
            while (!executor.awaitTermination(30, TimeUnit.SECONDS))
                logger.warning("Waiting for pending kit storage operations before shutdown");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while draining kit storage", e);
        }
    }
}
