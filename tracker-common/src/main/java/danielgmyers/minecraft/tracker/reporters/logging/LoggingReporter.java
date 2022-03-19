package danielgmyers.minecraft.tracker.reporters.logging;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.StatsReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

public class LoggingReporter implements StatsReporter {

    private static final Logger LOG = LogManager.getLogger();
    private final Config config;

    public LoggingReporter(Config config) {
        this.config = config;
    }

    @Override
    public void reportTickStats(String tickSource, Instant timestamp, long secondsWithData,
                                long totalTickCount, long minTickCount, long maxTickCount,
                                long totalTickMillis, long minTickMillis, long maxTickMillis) {
        long avgTickCount = totalTickCount / secondsWithData;
        long avgTickMillis = totalTickMillis / secondsWithData;
        LOG.info("Last minute {} stats: {} data points. TPS: {} (min), {} (avg), {} (max). Tick durations: {} ms (min), {} ms (avg), {} ms (max).",
                 tickSource, secondsWithData,
                 minTickCount, avgTickCount, maxTickCount,
                 minTickMillis, avgTickMillis, maxTickMillis);
    }

    @Override
    public void reportPlayerCount(String tickSource, Instant timestamp, long secondsWithData,
                                  long playerCountSum, long minPlayerCount, long maxPlayerCount) {
        long avgPlayerCount = playerCountSum / secondsWithData;
        LOG.info("Last minute {} player counts: {} data points. {} (min), {} (avg), {} (max).",
                 tickSource, secondsWithData, minPlayerCount, avgPlayerCount, maxPlayerCount);
    }
}
