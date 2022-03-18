package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.TickStatsReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;

public class TickStatsTracker {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String tickSource;
    private long previousTickStartTimeMillis = 0;
    private long previousTickEndTimeMillis = 0;
    private long currentTickStartTimeMillis = 0;

    private int tickCountThisSecond = 0;
    private long totalTickMillisThisSecond = 0;
    private long minTickMillisThisSecond = Long.MAX_VALUE;
    private long maxTickMillisThisSecond = 0;

    private long totalTickCountThisMinute = 0;
    private long minTickCountThisMinute = Long.MAX_VALUE;
    private long maxTickCountThisMinute = 0;

    private long totalTickMillisThisMinute = 0;
    private long minTickMillisThisMinute = Long.MAX_VALUE;
    private long maxTickMillisThisMinute = 0;

    private final Config config;
    private final TickStatsReporter reporter;
    private final Clock clock;

    public TickStatsTracker(String tickSource, Config config, TickStatsReporter reporter, Clock clock) {
        this.tickSource = tickSource;
        this.config = config;
        this.reporter = reporter;
        this.clock = clock;
        LOGGER.info("Initializing tick stats tracker for source '{}'", tickSource);
    }

    public void startTick() {
        currentTickStartTimeMillis = clock.millis();
    }

    public void endTick() {
        Instant now = clock.instant();
        long currentTickEndTimeMillis = now.toEpochMilli();

        // In this case, startTick was never called, so we'll treat it as a 0-ms tick.
        // Theoretically this can happen if the tracker wasn't initialized until partway through a tick,
        // and so endTick() was called first.
        if (currentTickStartTimeMillis == 0) {
            currentTickStartTimeMillis = currentTickEndTimeMillis;
        }

        // If we don't have a previous end tick time, this is the first tick.
        // We'll pretend the previous tick happened already by copying this tick's data.
        // It won't get used for anything except to determine if we're in the next second or minute,
        // which won't be the case if the values are equal.
        if (previousTickEndTimeMillis == 0) {
            previousTickStartTimeMillis = currentTickStartTimeMillis;
            previousTickEndTimeMillis = currentTickEndTimeMillis;
        }

        // we use the tick start times to figure out if we're in the next second, so that
        // variable-duration ticks don't confuse us
        if (inNextSecond(previousTickStartTimeMillis, currentTickStartTimeMillis) && tickCountThisSecond > 0) {
            if (config.isPerSecondEnabled()) {
                reporter.reportSecond(tickSource, now, tickCountThisSecond,
                                      totalTickMillisThisSecond, minTickMillisThisSecond, maxTickMillisThisSecond);
            }

            totalTickCountThisMinute += tickCountThisSecond;
            minTickCountThisMinute = Math.min(minTickCountThisMinute, tickCountThisSecond);
            maxTickCountThisMinute = Math.max(maxTickCountThisMinute, tickCountThisSecond);

            totalTickMillisThisMinute += totalTickMillisThisSecond;
            minTickMillisThisMinute = Math.min(minTickMillisThisMinute, minTickMillisThisSecond);
            maxTickMillisThisMinute = Math.max(maxTickMillisThisMinute, maxTickMillisThisSecond);

            tickCountThisSecond = 0;
            totalTickMillisThisSecond = 0;
            minTickMillisThisSecond = Long.MAX_VALUE;
            maxTickMillisThisSecond = 0;

            if (inNextMinute(previousTickStartTimeMillis, currentTickStartTimeMillis)) {
                reporter.reportMinute(tickSource, now, totalTickCountThisMinute,
                                      totalTickCountThisMinute, minTickCountThisMinute, maxTickCountThisMinute,
                                      totalTickMillisThisMinute, minTickMillisThisMinute, maxTickMillisThisMinute);

                totalTickCountThisMinute = 0;
                minTickCountThisMinute = Long.MAX_VALUE;
                maxTickCountThisMinute = 0;

                totalTickMillisThisMinute = 0;
                minTickMillisThisMinute = Long.MAX_VALUE;
                maxTickMillisThisMinute = 0;
            }
        }

        // finally, we add the current tick's duration to the tick duration queue.
        long currentTickDurationMillis = currentTickEndTimeMillis - currentTickStartTimeMillis;

        tickCountThisSecond++;
        totalTickMillisThisSecond += currentTickDurationMillis;
        minTickMillisThisSecond = Math.min(minTickMillisThisSecond, currentTickDurationMillis);
        maxTickMillisThisSecond = Math.max(maxTickMillisThisSecond, currentTickDurationMillis);

        previousTickStartTimeMillis = currentTickStartTimeMillis;
        previousTickEndTimeMillis = currentTickEndTimeMillis;
    }

    private boolean inNextSecond(long previousTimeMillis, long currentTimeMillis) {
        return (previousTimeMillis / 1000) < (currentTimeMillis / 1000);
    }

    private boolean inNextMinute(long previousTimeMillis, long currentTimeMillis) {
        // this won't line up with wall-clock minutes, but that's ok, we just need consistent 60-second windows.
        return (previousTimeMillis / 60000) < (currentTimeMillis / 60000);
    }
}
