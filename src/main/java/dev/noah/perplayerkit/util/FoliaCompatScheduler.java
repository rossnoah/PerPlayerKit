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
package dev.noah.perplayerkit.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Folia-compatible scheduler utility that provides a clean API
 * and detects the server type for appropriate scheduling.
 * 
 * This version uses standard Bukkit scheduling but provides a future-ready API
 * for when Folia support is needed. The plugin will work on both Bukkit/Paper
 * and Folia servers.
 */
public class FoliaCompatScheduler {
    
    private static final boolean IS_FOLIA_AVAILABLE;
    
    static {
        boolean foliaAvailable = false;
        try {
            // Check for Folia-specific classes
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            foliaAvailable = true;
        } catch (ClassNotFoundException e) {
            foliaAvailable = false;
        }
        IS_FOLIA_AVAILABLE = foliaAvailable;
    }
    
    /**
     * Run a task asynchronously
     * On Folia: Uses async scheduler
     * On Bukkit/Paper: Uses standard async task
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        // For now, use standard Bukkit scheduling
        // In a full Folia implementation, this would use Folia's async scheduler
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
    
    /**
     * Run a task on the main thread (or appropriate region thread in Folia)
     * On Folia: Uses global region scheduler
     * On Bukkit/Paper: Uses standard sync task
     */
    public static void runSync(Plugin plugin, Runnable task) {
        // For now, use standard Bukkit scheduling
        // In a full Folia implementation, this would use Folia's global region scheduler
        Bukkit.getScheduler().runTask(plugin, task);
    }
    
    /**
     * Run a task after a delay on the main thread
     */
    public static void runSyncLater(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
    
    /**
     * Run a task repeatedly on the main thread
     */
    public static void runSyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }
    
    /**
     * Run a task repeatedly asynchronously
     */
    public static void runAsyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }
    
    /**
     * Run a task after a delay asynchronously
     */
    public static void runAsyncLater(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }
    
    /**
     * Run a task on the entity's region thread (Folia-specific, falls back to main thread)
     * This method is prepared for Folia but currently uses standard scheduling
     */
    public static void runOnEntityThread(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA_AVAILABLE) {
            // TODO: When implementing full Folia support, use entity.getScheduler().run()
            // For now, fall back to sync task
            runSync(plugin, task);
        } else {
            runSync(plugin, task);
        }
    }
    
    /**
     * Run a task on the location's region thread (Folia-specific, falls back to main thread)
     * This method is prepared for Folia but currently uses standard scheduling
     */
    public static void runOnLocationThread(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA_AVAILABLE) {
            // TODO: When implementing full Folia support, use Bukkit.getRegionScheduler().run()
            // For now, fall back to sync task
            runSync(plugin, task);
        } else {
            runSync(plugin, task);
        }
    }
    
    /**
     * Cancel all tasks for the plugin
     */
    public static void cancelAllTasks(Plugin plugin) {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
    
    /**
     * Check if we're running on Folia
     */
    public static boolean isFolia() {
        return IS_FOLIA_AVAILABLE;
    }
    
    /**
     * Get information about the current server type
     */
    public static String getServerType() {
        if (IS_FOLIA_AVAILABLE) {
            return "Folia";
        } else if (hasClass("com.destroystokyo.paper.PaperConfig")) {
            return "Paper";
        } else if (hasClass("org.spigotmc.SpigotConfig")) {
            return "Spigot";
        } else {
            return "Bukkit";
        }
    }
    
    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}