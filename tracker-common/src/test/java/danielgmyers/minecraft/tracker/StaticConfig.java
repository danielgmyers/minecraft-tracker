package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.ReporterType;

public class StaticConfig implements Config {

    private ReporterType reporterType;
    private String cloudwatchMetricNamespace;

    public static StaticConfig create() {
        StaticConfig config = new StaticConfig();
        config.reporterType = ReporterType.APPLICATION_LOG;
        config.cloudwatchMetricNamespace = CLOUDWATCH_METRIC_NAMESPACE_DEFAULT;
        return config;
    }

    @Override
    public ReporterType getReporterType() {
        return reporterType;
    }

    public void setReporterType(ReporterType reporterType) {
        this.reporterType = reporterType;
    }

    @Override
    public String getCloudWatchMetricNamespace() {
        return cloudwatchMetricNamespace;
    }

    public void setCloudwatchMetricNamespace(String cloudwatchMetricNamespace) {
        this.cloudwatchMetricNamespace = cloudwatchMetricNamespace;
    }
}
