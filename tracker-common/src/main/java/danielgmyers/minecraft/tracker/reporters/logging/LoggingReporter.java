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
    public void reportSecond(String tickSource, Instant timestamp,
                             long tickCount, long totalTickMillis, long minTickMillis, long maxTickMillis) {
        if (config.isPerSecondEnabled()) {
            long avgTickMillis = totalTickMillis / tickCount;
            LOG.info("Last second {} stats: {} ticks. Durations: {}ms (min), {}ms (avg), {}ms (max).",
                     tickSource, tickCount, minTickMillis, avgTickMillis, maxTickMillis);
        }
    }

    @Override
    public void reportMinute(String tickSource, Instant timestamp, long datapointCount,
                             long totalTickCount, long minTickCount, long maxTickCount,
                             long totalTickMillis, long minTickMillis, long maxTickMillis) {
        long avgTickCount = totalTickCount / datapointCount;
        long avgTickMillis = totalTickMillis / datapointCount;
        LOG.info("Last minute {} stats: {} data points. TPS: {} (min), {} (avg), {} (max). Tick durations: {} ms (min), {} ms (avg), {} ms (max).",
                 tickSource, datapointCount,
                 minTickCount, avgTickCount, maxTickCount,
                 minTickMillis, avgTickMillis, maxTickMillis);
    }

    @Override
    public void reportPlayerCount(String tickSource, Instant timestamp, long datapointCount,
                                  long playerCountSum, long minPlayerCount, long maxPlayerCount) {
        long avgPlayerCount = playerCountSum / datapointCount;
        LOG.info("Last minute {} player counts: {} data points. {} (min), {} (avg), {} (max).",
                 tickSource, datapointCount, minPlayerCount, avgPlayerCount, maxPlayerCount);
    }
}
