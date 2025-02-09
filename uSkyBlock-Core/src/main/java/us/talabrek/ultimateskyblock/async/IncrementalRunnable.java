package us.talabrek.ultimateskyblock.async;

import dk.lockfuglsang.minecraft.util.TimeUtil;
import org.bukkit.scheduler.BukkitRunnable;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.util.Scheduler;

/**
 * Convenience template class for executing heavy tasks on the main thread.
 *
 * <h2>Usage</h2>
 * <pre>
 *     new IncrementalRunnable() {
 *         boolean execute() {
 *             while (!isDone()) {
 *                 // Do something incrementially synchronously
 *                 if (!tick()) {
 *                     break;
 *                 }
 *             }
 *             return isDone();
 *         }
 *     }
 * </pre>
 */
public abstract class IncrementalRunnable extends BukkitRunnable {
    private final Scheduler scheduler;
    private Runnable onCompletion;
    /**
     * The maximum number of consecutive ms to execute a task.
     */
    private final int maxMs;
    private final int maxConsecutive;
    private final int yieldDelay;


    /**
     * The time of creation
     */
    private double tStart = 0;

    /**
     * The time of completion.
     */
    private double tCompleted = 0;

    /**
     * Millis used in processing.
     */
    private double tUsed = 0;

    /**
     * The time of the current incremental run.
     */
    private double tRunning = 0;

    private volatile boolean isCancelled = false;

    private int consecutiveRuns = 0;

    /**
     * Number of iterations in total (calls to tick())
     */
    private volatile int iterations = 0;

    /**
     * Number of server-ticks consumed.
     */
    private volatile int ticks = 0;

    public IncrementalRunnable(Scheduler scheduler, PluginConfig config) {
        this(scheduler, null,
            config.getYamlConfig().getInt("async.maxMs", 15),
            config.getYamlConfig().getInt("async.maxConsecutiveTicks", 20),
            config.getYamlConfig().getInt("async.yieldDelay", 2)
        );
    }

    public IncrementalRunnable(Scheduler scheduler, PluginConfig config, Runnable onCompletion) {
        this(scheduler, onCompletion,
            config.getYamlConfig().getInt("async.maxMs", 15),
            config.getYamlConfig().getInt("async.maxConsecutiveTicks", 20),
            config.getYamlConfig().getInt("async.yieldDelay", 2)
        );
    }

    public IncrementalRunnable(Scheduler scheduler, Runnable onCompletion, int maxMs, int maxConsecutive, int yieldDelay) {
        this.scheduler = scheduler;
        this.onCompletion = onCompletion;
        this.maxMs = maxMs;
        this.maxConsecutive = maxConsecutive;
        this.yieldDelay = yieldDelay;
    }

    protected boolean hasTime() {
        return millisActive() < maxMs && !isCancelled;
    }

    /**
     * Used by sub-classes to see how much time they have left.
     *
     * @return The number of ms the current #execute() has been running.
     */
    protected long millisActive() {
        return Math.round(t() - tRunning);
    }

    protected double millisLeft() {
        return maxMs - millisActive();
    }

    public boolean stillTime() {
        double millisPerTick = getTimeUsed() / (iterations != 0 ? iterations : 1);
        return millisPerTick < millisLeft();
    }

    protected boolean tick() {
        iterations++;
        return stillTime();
    }

    /**
     * Executes a potentially heavy task
     *
     * @return <code>true</code> if done, <code>false</code> otherwise.
     */
    protected abstract boolean execute();

    /**
     * Returns the number of ms the task has been active.
     *
     * @return the number of ms the task has been active.
     */
    public long getTimeElapsed() {
        if (tCompleted != 0d) {
            return Math.round(tCompleted - tStart);
        }
        if (tStart == 0) {
            return -1;
        }
        return Math.round(t() - tStart);
    }

    /**
     * Returns the number of ms the task has been actively executing.
     *
     * @return the number of ms the task has been actively executing.
     */
    public double getTimeUsed() {
        return tUsed + (tRunning != 0 ? t() - tRunning : 0);
    }

    public void cancel() {
        isCancelled = true;
    }

    @Override
    public final void run() {
        tRunning = t();
        if (tStart == 0d) {
            tStart = tRunning;
            JobManager.addJob(this);
        }
        try {
            consecutiveRuns++;
            if (!execute() && !isCancelled) {
                scheduler.sync(this, TimeUtil.ticksAsDuration(consecutiveRuns < maxConsecutive ? 0 : yieldDelay));
            } else {
                if (onCompletion != null && !isCancelled) {
                    scheduler.sync(onCompletion);
                }
                complete();
            }
        } finally {
            tUsed += (t() - tRunning);
            tRunning = 0;
            if (consecutiveRuns > maxConsecutive) {
                consecutiveRuns = 0;
            }
            ticks++;
        }
    }

    private static double t() {
        return System.nanoTime() / 1000000d;
    }

    protected void setOnCompletion(Runnable onCompletion) {
        this.onCompletion = onCompletion;
    }

    private void complete() {
        tCompleted = t();
        JobManager.completeJob(this);
    }

    public int getTicks() {
        return ticks;
    }
}
