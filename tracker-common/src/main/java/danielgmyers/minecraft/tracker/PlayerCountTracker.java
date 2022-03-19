package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.StatsReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;

public class PlayerCountTracker {

    private static final Logger LOG = LogManager.getLogger();

    private final String tickSource;

    private final Config config;
    private final StatsReporter reporter;
    private final Clock clock;

    private long lastMinuteDatapointSum = 0;
    private long lastMinuteDatapointMin = Long.MAX_VALUE;
    private long lastMinuteDatapointMax = 0;
    private long lastMinuteDatapointCount = 0;

    private long lastDatapointTimeMillis = 0;

    public PlayerCountTracker(String tickSource, Config config, StatsReporter reporter, Clock clock) {
        this.tickSource = tickSource;
        this.config = config;
        this.reporter = reporter;
        this.clock = clock;
        LOG.info("Initializing player count tracker for source '{}'", tickSource);
    }

    // Player counts are tracked per second for statistical purposes, but only ever reported once per minute.
    public void update(long playerCount) {
        Instant now = clock.instant();

        long currentTimeMillis = now.toEpochMilli();

        if (lastDatapointTimeMillis > 0 && !inNextSecond(lastDatapointTimeMillis, currentTimeMillis)) {
            // want to record a datapoint no more than once per second,
            // so if our last recorded datapoint was during this same second, we'll stop here.
            return;
        }

        // if we're in the next minute, report our existing data before we include the new data point.
        if (lastDatapointTimeMillis > 0 && inNextMinute(lastDatapointTimeMillis, currentTimeMillis)) {
            reporter.reportPlayerCount(tickSource, now, lastMinuteDatapointCount,
                                       lastMinuteDatapointSum, lastMinuteDatapointMin, lastMinuteDatapointMax);
            lastMinuteDatapointCount = 0;
            lastMinuteDatapointSum = 0;
            lastMinuteDatapointMin = Long.MAX_VALUE;
            lastMinuteDatapointMax = 0;
        }

        // Yes, it's weird to sum up the player counts for each data point, but this gives us a proper average over
        // the minute when we generate per-minute statistics.
        lastMinuteDatapointSum += playerCount;
        lastMinuteDatapointMin = Math.min(lastMinuteDatapointMin, playerCount);
        lastMinuteDatapointMax = Math.max(lastMinuteDatapointMax, playerCount);
        lastMinuteDatapointCount++;

        lastDatapointTimeMillis = currentTimeMillis;
    }

    private boolean inNextSecond(long previousTimeMillis, long currentTimeMillis) {
        return (previousTimeMillis / 1000) < (currentTimeMillis / 1000);
    }

    private boolean inNextMinute(long previousTimeMillis, long currentTimeMillis) {
        // this won't line up with wall-clock minutes, but that's ok, we just need consistent 60-second windows.
        return (previousTimeMillis / 60000) < (currentTimeMillis / 60000);
    }
}
