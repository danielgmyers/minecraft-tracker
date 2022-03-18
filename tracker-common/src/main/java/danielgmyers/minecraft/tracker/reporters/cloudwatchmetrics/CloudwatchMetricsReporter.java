package danielgmyers.minecraft.tracker.reporters.cloudwatchmetrics;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.reporters.TickStatsReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

public class CloudwatchMetricsReporter implements TickStatsReporter {

    // visible for testing
    static final String SECOND = ".second.";
    static final String MINUTE = ".minute.";
    static final String TICK_COUNT = "tick-count";
    static final String TICK_MILLIS = "tick-millis";

    static final int SECOND_STORAGE_RESOLUTION = 1;
    static final int MINUTE_STORAGE_RESOLUTION = 60;

    private static final Logger LOG = LogManager.getLogger();
    private final CloudWatchAsyncClient cw;
    private final Config config;
    private final String metricNamespace;

    public CloudwatchMetricsReporter(Config config) {
        this(config, CloudWatchAsyncClient.create());
    }

    // visible for testing
    CloudwatchMetricsReporter(Config config, CloudWatchAsyncClient client) {
        this.config = config;
        cw = client;
        metricNamespace = config.getCloudWatchMetricNamespace();
    }

    @Override
    public void reportSecond(String tickSource, Instant timestamp, long tickCount,
                             long totalTickMillis, long minTickMillis, long maxTickMillis) {
        if (!config.isPerSecondEnabled()) {
            return;
        }
        LOG.error("Per-second reporting is not yet implemented!");
        // TODO -- batch and submit to CW once per minute
    }

    @Override
    public void reportMinute(String tickSource, Instant timestamp, long datapointCount,
                             long totalTickCount, long minTickCount, long maxTickCount,
                             long totalTickMillis, long minTickMillis, long maxTickMillis) {
        // The CloudWatch SDK is not nice enough to do this for us.
        Instant timestampTruncated = timestamp.truncatedTo(ChronoUnit.MILLIS);

        MetricDatum.Builder tickCount = MetricDatum.builder();
        tickCount.storageResolution(MINUTE_STORAGE_RESOLUTION);
        tickCount.timestamp(timestampTruncated);
        tickCount.metricName(tickSource + MINUTE + TICK_COUNT);
        tickCount.statisticValues(buildSet(datapointCount, totalTickCount, minTickCount, maxTickCount));
        tickCount.unit(StandardUnit.COUNT_SECOND);

        MetricDatum.Builder tickMillis = MetricDatum.builder();
        tickMillis.storageResolution(MINUTE_STORAGE_RESOLUTION);
        tickMillis.timestamp(timestampTruncated);
        tickMillis.metricName(tickSource + MINUTE + TICK_MILLIS);
        tickMillis.statisticValues(buildSet(datapointCount, totalTickMillis, minTickMillis, maxTickMillis));
        tickMillis.unit(StandardUnit.MILLISECONDS);

        putMetrics(tickCount.build(), tickMillis.build());
    }

    private StatisticSet buildSet(long sampleCount, long total, long min, long max) {
        StatisticSet.Builder stats = StatisticSet.builder();
        stats.sampleCount((double) sampleCount);
        stats.sum((double) total);
        stats.minimum((double) min);
        stats.maximum((double) max);
        return stats.build();
    }

    private void putMetrics(MetricDatum... datum) {
        PutMetricDataRequest request
                = PutMetricDataRequest.builder()
                .namespace(metricNamespace)
                .metricData(datum)
                .build();

        CompletableFuture<PutMetricDataResponse> response = cw.putMetricData(request);
        response.whenCompleteAsync((putMetricDataResponse, throwable) -> {
            if (throwable != null) {
                LOG.warn("Got an exception submitting metrics to CloudWatch", throwable);
            } else {
                String requestId = putMetricDataResponse.responseMetadata().requestId();
                LOG.trace("Metrics submitted. RequestId={}", requestId);
            }
        });
    }
}
