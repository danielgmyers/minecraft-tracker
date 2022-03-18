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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
    private final Clock clock;
    private final String metricNamespace;
    private final List<MetricDatum> queuedMetrics;

    private Instant lastMetricSubmission;

    public CloudwatchMetricsReporter(Config config, Clock clock) {
        this(config, clock, CloudWatchAsyncClient.create());
    }

    // visible for testing
    CloudwatchMetricsReporter(Config config, Clock clock, CloudWatchAsyncClient client) {
        this.config = config;
        this.clock = clock;
        cw = client;
        metricNamespace = config.getCloudWatchMetricNamespace();
        queuedMetrics = new ArrayList<>();
        lastMetricSubmission = Instant.EPOCH;
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
        // we use totalTickCount instead of datapointCount because average totalTickMillis is divided among all ticks.
        tickMillis.statisticValues(buildSet(totalTickCount, totalTickMillis, minTickMillis, maxTickMillis));
        tickMillis.unit(StandardUnit.MILLISECONDS);

        putMetric(tickCount.build());
        putMetric(tickMillis.build());
    }

    private StatisticSet buildSet(long sampleCount, long total, long min, long max) {
        StatisticSet.Builder stats = StatisticSet.builder();
        stats.sampleCount((double) sampleCount);
        stats.sum((double) total);
        stats.minimum((double) min);
        stats.maximum((double) max);
        return stats.build();
    }

    // We will queue metrics for asynchronous submission.
    // The actual API call will happen when we have accumulated 20 metrics or after one minute, whichever happens first.
    synchronized private void putMetric(MetricDatum datum) {
        queuedMetrics.add(datum);
        Instant now = clock.instant();
        final int queueSize = queuedMetrics.size();
        LOG.debug("{} metrics queued for submission.", queueSize);
        if (queueSize < 20 && lastMetricSubmission.isAfter(now.minus(Duration.ofMinutes(1)))) {
            return;
        }

        PutMetricDataRequest request
                = PutMetricDataRequest.builder()
                .namespace(metricNamespace)
                .metricData(new ArrayList<>(queuedMetrics))
                .build();

        CompletableFuture<PutMetricDataResponse> response = cw.putMetricData(request);
        response.whenCompleteAsync((putMetricDataResponse, throwable) -> {
            if (throwable != null) {
                LOG.warn("Got an exception submitting metrics to CloudWatch", throwable);
            } else {
                String requestId = putMetricDataResponse.responseMetadata().requestId();
                LOG.debug("{} metric(s) submitted. RequestId={}", queueSize, requestId);
            }
        });

        lastMetricSubmission = now;
        queuedMetrics.clear();
        LOG.debug("Metrics queue is clear.");
    }
}
