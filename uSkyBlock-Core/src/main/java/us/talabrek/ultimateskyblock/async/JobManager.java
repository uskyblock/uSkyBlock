package us.talabrek.ultimateskyblock.async;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Math.max;

/**
 * Responsible for holding ongoing jobs, and recording status.
 */
public enum JobManager {
    ;
    private static final ConcurrentMap<String, Stats> jobStats = new ConcurrentHashMap<>();

    public static void addJob(IncrementalRunnable runnable) {
        String jobName = runnable.getClass().getSimpleName();
        if (!jobStats.containsKey(jobName)) {
            jobStats.put(jobName, new Stats());
        }
        jobStats.get(jobName).add(runnable);
    }

    public static void completeJob(IncrementalRunnable runnable) {
        String jobName = runnable.getClass().getSimpleName();
        if (!jobStats.containsKey(jobName)) {
            jobStats.put(jobName, new Stats());
        }
        jobStats.get(jobName).complete(runnable);
    }

    public static Map<String, Stats> getStats() {
        return Collections.unmodifiableMap(jobStats);
    }

    public static class Stats {
        private int jobs;
        private int jobsRunning;
        private long ticks;
        private Duration timeActive;
        private Duration timeElapsed;

        public Stats() {
        }

        public synchronized void add(IncrementalRunnable runnable) {
            jobsRunning++;
            jobs++;
        }

        public synchronized void complete(IncrementalRunnable runnable) {
            jobsRunning--;
            ticks += runnable.getTicksConsumed();
            timeActive = timeActive.plus(runnable.getProcessingTimeUsed());
            timeElapsed = timeElapsed.plus(runnable.getTimeElapsed());
        }

        public int getJobs() {
            return jobs;
        }

        public long getTicks() {
            return ticks;
        }

        public Duration getTimeActive() {
            return timeActive;
        }

        public Duration getTimeElapsed() {
            return timeElapsed;
        }

        public Duration getAvgRunningTimePerTick() {
            return timeActive.dividedBy(max(1, ticks));
        }

        public Duration getAvgRunningTimePerJob() {
            return timeActive.dividedBy(max(1, jobs));
        }

        public Duration getAvgTimeElapsedPerJob() {
            return timeElapsed.dividedBy(max(1, jobs));
        }

        public int getRunningJobs() {
            return jobsRunning;
        }
    }
}
