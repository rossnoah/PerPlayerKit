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
package dev.noah.perplayerkit.metrics;

import dev.noah.perplayerkit.logging.PerPlayerKitLogger;
import dev.noah.perplayerkit.validation.Validator;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collection system for monitoring plugin performance and usage patterns.
 * Thread-safe and designed for high-throughput environments like Folia.
 */
public class MetricsCollector {
    
    private final PerPlayerKitLogger logger;
    private final ConcurrentHashMap<String, LongAdder> counters;
    private final ConcurrentHashMap<String, TimingData> timings;
    private final AtomicLong startTime;
    
    public MetricsCollector(@NotNull PerPlayerKitLogger logger) {
        this.logger = Validator.requireNonNull(logger, "logger");
        this.counters = new ConcurrentHashMap<>();
        this.timings = new ConcurrentHashMap<>();
        this.startTime = new AtomicLong(System.currentTimeMillis());
    }
    
    /**
     * Increments a counter metric.
     *
     * @param name the metric name
     */
    public void incrementCounter(@NotNull String name) {
        incrementCounter(name, 1);
    }
    
    /**
     * Increments a counter metric by a specific amount.
     *
     * @param name the metric name
     * @param amount the amount to increment
     */
    public void incrementCounter(@NotNull String name, long amount) {
        Validator.requireNonEmpty(name, "metric name");
        counters.computeIfAbsent(name, k -> new LongAdder()).add(amount);
    }
    
    /**
     * Gets the current value of a counter metric.
     *
     * @param name the metric name
     * @return the current value
     */
    public long getCounterValue(@NotNull String name) {
        LongAdder counter = counters.get(name);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * Records a timing measurement.
     *
     * @param name the metric name
     * @param durationMs the duration in milliseconds
     */
    public void recordTiming(@NotNull String name, long durationMs) {
        Validator.requireNonEmpty(name, "metric name");
        timings.computeIfAbsent(name, k -> new TimingData()).addTiming(durationMs);
        
        // Log slow operations
        if (durationMs > 100) {
            logger.timing(name, durationMs);
        }
    }
    
    /**
     * Gets timing statistics for a metric.
     *
     * @param name the metric name
     * @return the timing statistics, or null if not found
     */
    public TimingStats getTimingStats(@NotNull String name) {
        TimingData data = timings.get(name);
        return data != null ? data.getStats() : null;
    }
    
    /**
     * Creates a timer for measuring operation duration.
     *
     * @param name the metric name
     * @return a new timer
     */
    @NotNull
    public Timer startTimer(@NotNull String name) {
        return new Timer(name);
    }
    
    /**
     * Gets a summary of all collected metrics.
     *
     * @return metrics summary
     */
    @NotNull
    public MetricsSummary getSummary() {
        return new MetricsSummary(counters, timings, startTime.get());
    }
    
    /**
     * Resets all metrics.
     */
    public void reset() {
        counters.clear();
        timings.clear();
        startTime.set(System.currentTimeMillis());
        logger.info("Metrics reset");
    }
    
    /**
     * Timer class for measuring operation duration.
     */
    public final class Timer implements AutoCloseable {
        private final String name;
        private final long startTime;
        
        private Timer(@NotNull String name) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * Stops the timer and records the duration.
         *
         * @return the recorded duration in milliseconds
         */
        public long stop() {
            long duration = System.currentTimeMillis() - startTime;
            recordTiming(name, duration);
            return duration;
        }
        
        @Override
        public void close() {
            stop();
        }
    }
    
    /**
     * Thread-safe timing data collection.
     */
    private static final class TimingData {
        private final LongAdder count = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = Long.MIN_VALUE;
        
        private void addTiming(long duration) {
            count.increment();
            totalTime.add(duration);
            
            // Update min/max atomically using compare-and-swap pattern
            updateMin(duration);
            updateMax(duration);
        }
        
        private void updateMin(long duration) {
            long current;
            do {
                current = minTime;
                if (duration >= current) break;
            } while (!compareAndSetMin(current, duration));
        }
        
        private void updateMax(long duration) {
            long current;
            do {
                current = maxTime;
                if (duration <= current) break;
            } while (!compareAndSetMax(current, duration));
        }
        
        private boolean compareAndSetMin(long expect, long update) {
            // Simplified atomic operation for demonstration
            synchronized (this) {
                if (minTime == expect) {
                    minTime = update;
                    return true;
                }
                return false;
            }
        }
        
        private boolean compareAndSetMax(long expect, long update) {
            // Simplified atomic operation for demonstration
            synchronized (this) {
                if (maxTime == expect) {
                    maxTime = update;
                    return true;
                }
                return false;
            }
        }
        
        private TimingStats getStats() {
            long countValue = count.sum();
            long totalValue = totalTime.sum();
            long average = countValue > 0 ? totalValue / countValue : 0;
            long min = minTime != Long.MAX_VALUE ? minTime : 0;
            long max = maxTime != Long.MIN_VALUE ? maxTime : 0;
            
            return new TimingStats(countValue, totalValue, average, min, max);
        }
    }
    
    /**
     * Immutable timing statistics.
     */
    public static final class TimingStats {
        private final long count;
        private final long totalTime;
        private final long averageTime;
        private final long minTime;
        private final long maxTime;
        
        private TimingStats(long count, long totalTime, long averageTime, long minTime, long maxTime) {
            this.count = count;
            this.totalTime = totalTime;
            this.averageTime = averageTime;
            this.minTime = minTime;
            this.maxTime = maxTime;
        }
        
        public long getCount() { return count; }
        public long getTotalTime() { return totalTime; }
        public long getAverageTime() { return averageTime; }
        public long getMinTime() { return minTime; }
        public long getMaxTime() { return maxTime; }
        
        @Override
        public String toString() {
            return String.format("TimingStats{count=%d, total=%dms, avg=%dms, min=%dms, max=%dms}",
                count, totalTime, averageTime, minTime, maxTime);
        }
    }
    
    /**
     * Complete metrics summary.
     */
    public static final class MetricsSummary {
        private final ConcurrentHashMap<String, Long> counterSnapshot;
        private final ConcurrentHashMap<String, TimingStats> timingSnapshot;
        private final long startTime;
        private final long uptime;
        
        private MetricsSummary(ConcurrentHashMap<String, LongAdder> counters,
                              ConcurrentHashMap<String, TimingData> timings,
                              long startTime) {
            this.startTime = startTime;
            this.uptime = System.currentTimeMillis() - startTime;
            
            // Create snapshots
            this.counterSnapshot = new ConcurrentHashMap<>();
            counters.forEach((key, value) -> counterSnapshot.put(key, value.sum()));
            
            this.timingSnapshot = new ConcurrentHashMap<>();
            timings.forEach((key, value) -> timingSnapshot.put(key, value.getStats()));
        }
        
        public ConcurrentHashMap<String, Long> getCounters() { return new ConcurrentHashMap<>(counterSnapshot); }
        public ConcurrentHashMap<String, TimingStats> getTimings() { return new ConcurrentHashMap<>(timingSnapshot); }
        public long getStartTime() { return startTime; }
        public long getUptime() { return uptime; }
        
        @Override
        public String toString() {
            return String.format("MetricsSummary{uptime=%dms, counters=%d, timings=%d}",
                uptime, counterSnapshot.size(), timingSnapshot.size());
        }
    }
}