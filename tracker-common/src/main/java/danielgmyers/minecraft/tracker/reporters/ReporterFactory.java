package danielgmyers.minecraft.tracker.reporters;

import danielgmyers.minecraft.tracker.MinuteTickStatsBlock;
import danielgmyers.minecraft.tracker.SecondTickStatsBlock;
import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.logging.LoggingReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ReporterFactory {

    private static final Logger LOG = LogManager.getLogger();

    public static TickStatsReporter create(Config config) {
        LOG.info("Creating tick stats reporter with type {}.", config.getReporterType());
        switch(config.getReporterType()) {
            case NONE:
                // we'll hand back a no-op reporter in this case.
                return new TickStatsReporter() {
                    @Override
                    public void report(String tickSource, SecondTickStatsBlock stats) {

                    }

                    @Override
                    public void report(String tickSource, MinuteTickStatsBlock stats) {

                    }
                };
            case APPLICATION_LOG:
                return new LoggingReporter(config);
            case CLOUDWATCH_DIRECT:
            case CLOUDWATCH_LOGS_EMF:
            default:
                throw new RuntimeException("Unknown reporter type: " + config.getReporterType().toString());
        }
    }
}
