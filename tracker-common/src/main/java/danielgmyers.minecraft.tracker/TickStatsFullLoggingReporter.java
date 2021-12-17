package danielgmyers.minecraft.tracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TickStatsFullLoggingReporter implements TickStatsReporter {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public void report(String tickSource, SecondTickStatsBlock stats) {
        LOG.info("Last second {} stats: {} ticks. Durations: {}ms (min), {}ms (avg), {}ms (max).",
                 tickSource, stats.getTickCount(), stats.getMinTickMillis(),
                 stats.getAvgTickMillis(), stats.getMaxTickMillis());
    }

    @Override
    public void report(String tickSource, MinuteTickStatsBlock stats) {
        LOG.info("Last minute {} stats: {} data points. TPS: {} (min), {} (avg), {} (max). Tick durations: {} ms (min), {} ms (avg), {} ms (max).",
                 tickSource, stats.getDatapointCount(),
                 stats.getMinTickCount(), stats.getAvgTickCount(), stats.getMaxTickCount(),
                 stats.getMinTickMillis(), stats.getAvgTickMillis(), stats.getMaxTickMillis());
    }
}
