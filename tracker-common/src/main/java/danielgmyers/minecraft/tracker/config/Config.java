package danielgmyers.minecraft.tracker.config;

import java.util.EnumSet;

public interface Config {

    String PER_SECOND_ENABLED = "per-second-metrics-enabled";
    Boolean PER_SECOND_ENABLED_DEFAULT = false;
    String REPORTER_TYPE = "reporter-type";
    ReporterType REPORTER_TYPE_DEFAULT = ReporterType.NONE;

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

    default boolean isPerSecondEnabled() {
        return retrieveBoolean(PER_SECOND_ENABLED, PER_SECOND_ENABLED_DEFAULT);
    }

    default ReporterType getReporterType() {
        return retrieveEnumConfig(REPORTER_TYPE, REPORTER_TYPE_DEFAULT);
    }
}
