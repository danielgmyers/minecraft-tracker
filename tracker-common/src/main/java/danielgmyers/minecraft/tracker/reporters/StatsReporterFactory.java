package danielgmyers.minecraft.tracker.reporters;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.cloudwatchmetrics.CloudwatchMetricsReporter;
import danielgmyers.minecraft.tracker.reporters.logging.LoggingReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;

public final class StatsReporterFactory {

    private static final Logger LOG = LogManager.getLogger();

    public static StatsReporter create(Config config, Clock clock) {
        LOG.info("Creating tick stats reporter with type {}.", config.getReporterType());
        switch(config.getReporterType()) {
            case NONE:
                // we'll hand back a no-op reporter in this case.
                return new StatsReporter() {
                    @Override
                    public void reportSecond(String tickSource, Instant timestamp, long tickCount,
                                             long totalTickMillis, long minTickMillis, long maxTickMillis) {

                    }

                    @Override
                    public void reportMinute(String tickSource, Instant timestamp, long datapointCount,
                                             long totalTickCount, long minTickCount, long maxTickCount,
                                             long totalTickMillis, long minTickMillis, long maxTickMillis) {

                    }

                    @Override
                    public void reportPlayerCount(String tickSource, Instant timestamp, long datapointCount,
                                                  long playerCountSum, long minPlayerCount, long maxPlayerCount) {

                    }
                };
            case APPLICATION_LOG:
                return new LoggingReporter(config);
            case CLOUDWATCH_DIRECT:
                return new CloudwatchMetricsReporter(config, clock);
            case CLOUDWATCH_LOGS_EMF:
            default:
                throw new RuntimeException("Reporter type " + config.getReporterType() + " not implemented.");
        }
    }
}
