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
package dev.noah.perplayerkit.logging;

import dev.noah.perplayerkit.exceptions.PerPlayerKitException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Structured logging facade for PerPlayerKit with context-aware logging capabilities.
 * Provides consistent logging patterns and metrics collection.
 */
public class PerPlayerKitLogger {
    
    private final Logger bukkitLogger;
    private final String pluginName;
    
    public PerPlayerKitLogger(@NotNull Logger bukkitLogger, @NotNull String pluginName) {
        this.bukkitLogger = bukkitLogger;
        this.pluginName = pluginName;
    }
    
    /**
     * Logs an info message.
     *
     * @param message the message to log
     */
    public void info(@NotNull String message) {
        bukkitLogger.info(formatMessage(message, null, null));
    }
    
    /**
     * Logs an info message with player context.
     *
     * @param message the message to log
     * @param playerId the player ID for context
     */
    public void info(@NotNull String message, @Nullable UUID playerId) {
        bukkitLogger.info(formatMessage(message, playerId, null));
    }
    
    /**
     * Logs a warning message.
     *
     * @param message the message to log
     */
    public void warn(@NotNull String message) {
        bukkitLogger.warning(formatMessage(message, null, null));
    }
    
    /**
     * Logs a warning message with player context.
     *
     * @param message the message to log
     * @param playerId the player ID for context
     */
    public void warn(@NotNull String message, @Nullable UUID playerId) {
        bukkitLogger.warning(formatMessage(message, playerId, null));
    }
    
    /**
     * Logs a warning message with exception.
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    public void warn(@NotNull String message, @NotNull Throwable throwable) {
        bukkitLogger.log(Level.WARNING, formatMessage(message, null, null), throwable);
    }
    
    /**
     * Logs an error message.
     *
     * @param message the message to log
     */
    public void error(@NotNull String message) {
        bukkitLogger.severe(formatMessage(message, null, null));
    }
    
    /**
     * Logs an error message with player context.
     *
     * @param message the message to log
     * @param playerId the player ID for context
     */
    public void error(@NotNull String message, @Nullable UUID playerId) {
        bukkitLogger.severe(formatMessage(message, playerId, null));
    }
    
    /**
     * Logs an error message with exception.
     *
     * @param message the message to log
     * @param throwable the exception to log
     */
    public void error(@NotNull String message, @NotNull Throwable throwable) {
        bukkitLogger.log(Level.SEVERE, formatMessage(message, null, null), throwable);
    }
    
    /**
     * Logs an error message with exception and player context.
     *
     * @param message the message to log
     * @param throwable the exception to log
     * @param playerId the player ID for context
     */
    public void error(@NotNull String message, @NotNull Throwable throwable, @Nullable UUID playerId) {
        bukkitLogger.log(Level.SEVERE, formatMessage(message, playerId, null), throwable);
    }
    
    /**
     * Logs a PerPlayerKit exception with full context.
     *
     * @param exception the PerPlayerKit exception to log
     */
    public void error(@NotNull PerPlayerKitException exception) {
        bukkitLogger.log(Level.SEVERE, 
            formatMessage(exception.getFormattedMessage(), exception.getPlayerId(), exception.getContext()), 
            exception);
    }
    
    /**
     * Logs a debug message (only when debug mode is enabled).
     *
     * @param message the message to log
     */
    public void debug(@NotNull String message) {
        if (isDebugEnabled()) {
            bukkitLogger.info("[DEBUG] " + formatMessage(message, null, null));
        }
    }
    
    /**
     * Logs a debug message with player context.
     *
     * @param message the message to log
     * @param playerId the player ID for context
     */
    public void debug(@NotNull String message, @Nullable UUID playerId) {
        if (isDebugEnabled()) {
            bukkitLogger.info("[DEBUG] " + formatMessage(message, playerId, null));
        }
    }
    
    /**
     * Logs an operation timing for performance monitoring.
     *
     * @param operation the operation name
     * @param durationMs the duration in milliseconds
     */
    public void timing(@NotNull String operation, long durationMs) {
        if (durationMs > 100) { // Only log slow operations
            warn(String.format("Slow operation: %s took %dms", operation, durationMs));
        } else {
            debug(String.format("Operation timing: %s took %dms", operation, durationMs));
        }
    }
    
    /**
     * Logs an operation timing with player context.
     *
     * @param operation the operation name
     * @param durationMs the duration in milliseconds
     * @param playerId the player ID for context
     */
    public void timing(@NotNull String operation, long durationMs, @Nullable UUID playerId) {
        String message = String.format("Operation timing: %s took %dms", operation, durationMs);
        if (durationMs > 100) { // Only log slow operations
            warn(message, playerId);
        } else {
            debug(message, playerId);
        }
    }
    
    /**
     * Checks if debug logging is enabled.
     *
     * @return true if debug logging is enabled
     */
    private boolean isDebugEnabled() {
        // This could be configurable via plugin config
        return System.getProperty("perplayerkit.debug", "false").equals("true");
    }
    
    /**
     * Formats a log message with consistent structure.
     *
     * @param message the base message
     * @param playerId the player ID for context
     * @param additionalContext any additional context
     * @return formatted message
     */
    @NotNull
    private String formatMessage(@NotNull String message, @Nullable UUID playerId, @Nullable String additionalContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        
        if (playerId != null) {
            sb.append(" [Player: ").append(playerId).append("]");
        }
        
        if (additionalContext != null && !additionalContext.trim().isEmpty()) {
            sb.append(" [").append(additionalContext).append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the underlying Bukkit logger.
     *
     * @return the Bukkit logger
     */
    @NotNull
    public Logger getBukkitLogger() {
        return bukkitLogger;
    }
}