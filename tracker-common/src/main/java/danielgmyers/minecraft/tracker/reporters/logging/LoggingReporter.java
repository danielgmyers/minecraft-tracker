package danielgmyers.minecraft.tracker.reporters.logging;

import danielgmyers.minecraft.tracker.MinuteTickStatsBlock;
import danielgmyers.minecraft.tracker.SecondTickStatsBlock;
import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.TickStatsReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingReporter implements TickStatsReporter {

    private static final Logger LOG = LogManager.getLogger();
    private final Config config;

    public LoggingReporter(Config config) {
        this.config = config;
    }

    @Override
    public void report(String tickSource, SecondTickStatsBlock stats) {
        if (config.isPerSecondEnabled()) {
            LOG.info("Last second {} stats: {} ticks. Durations: {}ms (min), {}ms (avg), {}ms (max).",
                    tickSource, stats.getTickCount(), stats.getMinTickMillis(),
                    stats.getAvgTickMillis(), stats.getMaxTickMillis());
        }
    }

    @Override
    public void report(String tickSource, MinuteTickStatsBlock stats) {
        LOG.info("Last minute {} stats: {} data points. TPS: {} (min), {} (avg), {} (max). Tick durations: {} ms (min), {} ms (avg), {} ms (max).",
                 tickSource, stats.getDatapointCount(),
                 stats.getMinTickCount(), stats.getAvgTickCount(), stats.getMaxTickCount(),
                 stats.getMinTickMillis(), stats.getAvgTickMillis(), stats.getMaxTickMillis());
    }
}
