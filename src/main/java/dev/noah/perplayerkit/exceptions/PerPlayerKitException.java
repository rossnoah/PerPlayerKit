/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base exception class for all PerPlayerKit-related exceptions.
 * Provides structured error handling with context information.
 */
public abstract class PerPlayerKitException extends RuntimeException {
    
    private final String errorCode;
    private final UUID playerId;
    private final String context;
    
    /**
     * Constructs a new PerPlayerKit exception with the specified detail message.
     *
     * @param message the detail message
     * @param errorCode unique error code for this exception type
     * @param playerId the player ID associated with this exception, if any
     * @param context additional context information
     */
    protected PerPlayerKitException(@NotNull String message, 
                                   @NotNull String errorCode, 
                                   @Nullable UUID playerId, 
                                   @Nullable String context) {
        super(message);
        this.errorCode = errorCode;
        this.playerId = playerId;
        this.context = context;
    }
    
    /**
     * Constructs a new PerPlayerKit exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     * @param errorCode unique error code for this exception type
     * @param playerId the player ID associated with this exception, if any
     * @param context additional context information
     */
    protected PerPlayerKitException(@NotNull String message, 
                                   @Nullable Throwable cause, 
                                   @NotNull String errorCode, 
                                   @Nullable UUID playerId, 
                                   @Nullable String context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.playerId = playerId;
        this.context = context;
    }
    
    /**
     * Gets the unique error code for this exception.
     *
     * @return the error code
     */
    @NotNull
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the player ID associated with this exception.
     *
     * @return the player ID, or null if not applicable
     */
    @Nullable
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets additional context information for this exception.
     *
     * @return the context, or null if not available
     */
    @Nullable
    public String getContext() {
        return context;
    }
    
    /**
     * Creates a structured error message including all available context.
     *
     * @return formatted error message
     */
    @NotNull
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode).append("] ").append(getMessage());
        
        if (playerId != null) {
            sb.append(" [Player: ").append(playerId).append("]");
        }
        
        if (context != null && !context.trim().isEmpty()) {
            sb.append(" [Context: ").append(context).append("]");
        }
        
        return sb.toString();
    }
}