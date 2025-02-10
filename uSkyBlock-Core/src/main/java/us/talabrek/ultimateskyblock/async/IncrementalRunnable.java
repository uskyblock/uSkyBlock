package us.talabrek.ultimateskyblock.async;

import dk.lockfuglsang.minecraft.util.TimeUtil;
import dk.lockfuglsang.minecraft.util.Timer;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.PluginConfig;
import us.talabrek.ultimateskyblock.util.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;

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
    private final Duration maxIterationTime;
    private final long maxConsecutiveRuns;
    private final Duration yieldDelay;
    private Timer lifetimeTimer = null;

    /**
     * The time of completion.
     */
    private Instant completed = null;

    /**
     * Time used in processing.
     */
    private Duration processingTimeUsed = Duration.ZERO;

    /**
     * The time of the current incremental run.
     */
    private Timer iterationTimer = null;

    private volatile boolean isCancelled = false;

    private final AtomicInteger consecutiveRuns = new AtomicInteger(0);

    /**
     * Number of iterations in total (calls to tick())
     */
    private final AtomicInteger iterations = new AtomicInteger(0);

    /**
     * Number of server-ticks consumed.
     */
    private final AtomicInteger ticksConsumed = new AtomicInteger(0);

    public IncrementalRunnable(@NotNull Scheduler scheduler, @NotNull PluginConfig config) {
        this(scheduler, config, null);
    }

    public IncrementalRunnable(@NotNull Scheduler scheduler, @NotNull PluginConfig config, @Nullable Runnable onCompletion) {
        this(scheduler, onCompletion,
            Duration.ofMillis(config.getYamlConfig().getInt("async.maxMs", 15)),
            config.getYamlConfig().getLong("async.maxConsecutiveTicks", 20),
            TimeUtil.ticksAsDuration(config.getYamlConfig().getLong("async.yieldDelay", 2))
        );
    }

    public IncrementalRunnable(@NotNull Scheduler scheduler, @Nullable Runnable onCompletion, @NotNull Duration maxIterationTime, long maxConsecutiveRuns, @NotNull Duration yieldDelay) {
        this.scheduler = scheduler;
        this.onCompletion = onCompletion;
        this.maxIterationTime = maxIterationTime;
        this.maxConsecutiveRuns = maxConsecutiveRuns;
        this.yieldDelay = yieldDelay;
    }

    /**
     * Used by subclasses to see how much time they have left.
     *
     * @return The number of ms the current #execute() has been running.
     */
    protected @NotNull Duration millisActive() {
        return iterationTimer != null ? iterationTimer.elapsed() : Duration.ZERO;
    }

    protected @NotNull Duration millisLeft() {
        return maxIterationTime.minus(millisActive());
    }

    public boolean stillTime() {
        Duration millisPerTick = getProcessingTimeUsed().dividedBy(max(iterations.get(), 1));
        return millisLeft().minus(millisPerTick).isPositive();
    }

    protected boolean tick() {
        iterations.incrementAndGet();
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
    public @NotNull Duration getTimeElapsed() {
        if (completed != null) {
            assert lifetimeTimer != null;
            return Duration.between(lifetimeTimer.getStart(), completed);
        }
        if (lifetimeTimer == null) {
            return Duration.ZERO;
        }
        return lifetimeTimer.elapsed();
    }

    /**
     * Returns the number of ms the task has been actively executing.
     *
     * @return the number of ms the task has been actively executing.
     */
    public @NotNull Duration getProcessingTimeUsed() {
        return processingTimeUsed.plus(millisActive());
    }

    public void cancel() {
        isCancelled = true;
    }

    @Override
    public final void run() {
        iterationTimer = Timer.start();
        if (lifetimeTimer == null) {
            lifetimeTimer = Timer.start();
            JobManager.addJob(this);
        }
        int consecutiveRuns = this.consecutiveRuns.incrementAndGet();
        try {
            if (!execute() && !isCancelled) {
                scheduler.sync(this, consecutiveRuns < maxConsecutiveRuns ? Duration.ZERO : yieldDelay);
            } else {
                if (onCompletion != null && !isCancelled) {
                    scheduler.sync(onCompletion);
                }
                complete();
            }
        } finally {
            processingTimeUsed = processingTimeUsed.plus(iterationTimer.elapsed());
            iterationTimer = null;
            if (consecutiveRuns > maxConsecutiveRuns) {
                this.consecutiveRuns.set(0);
            }
            ticksConsumed.incrementAndGet();
        }
    }

    protected void setOnCompletion(Runnable onCompletion) {
        this.onCompletion = onCompletion;
    }

    private void complete() {
        completed = Instant.now();
        JobManager.completeJob(this);
    }

    public int getTicksConsumed() {
        return ticksConsumed.get();
    }
}
