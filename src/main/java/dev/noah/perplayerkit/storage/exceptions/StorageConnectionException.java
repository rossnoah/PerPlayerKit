package dev.noah.perplayerkit.storage.exceptions;

public class StorageConnectionException extends StorageException {
    public StorageConnectionException(String message) {
        super(message);
    }

    public StorageConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
