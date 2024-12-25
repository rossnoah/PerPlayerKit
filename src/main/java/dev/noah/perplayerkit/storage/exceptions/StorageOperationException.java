package dev.noah.perplayerkit.storage.exceptions;

public class StorageOperationException extends StorageException {
    public StorageOperationException(String message) {
        super(message);
    }

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
