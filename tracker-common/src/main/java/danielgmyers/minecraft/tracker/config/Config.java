package danielgmyers.minecraft.tracker.config;

import java.util.EnumSet;

public interface Config {

    String REPORTER_TYPE = "reporter-type";
    ReporterType REPORTER_TYPE_DEFAULT = ReporterType.NONE;

    String CLOUDWATCH_METRIC_NAMESPACE = "cloudwatch-metric-namespace";
    String CLOUDWATCH_METRIC_NAMESPACE_DEFAULT = "minecraft-tracker";

    default String retrieveConfig(String propertyName, String defaultValue) {
        return defaultValue;
    }

    default boolean retrieveBoolean(String propertyName, boolean defaultValue) {
        String value = retrieveConfig(propertyName, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    default <T extends Enum> T retrieveEnumConfig(String propertyName, T defaultValue) {
        String rawValue = retrieveConfig(propertyName, null);
        if (rawValue == null) {
            return defaultValue;
        }
        for (Object e : EnumSet.allOf(defaultValue.getClass())) {
            if (e.toString().equalsIgnoreCase(rawValue)) {
                return (T)e;
            }
        }
        return defaultValue;
    }

    default boolean isEnabled() {
        return getReporterType() != ReporterType.NONE;
    }

    default ReporterType getReporterType() {
        return retrieveEnumConfig(REPORTER_TYPE, REPORTER_TYPE_DEFAULT);
    }

    default String getCloudWatchMetricNamespace() {
        return retrieveConfig(CLOUDWATCH_METRIC_NAMESPACE, CLOUDWATCH_METRIC_NAMESPACE_DEFAULT);
    }
}
