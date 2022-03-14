package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.ReporterType;

public class StaticConfig implements Config {

    private boolean perSecondEnabled;
    private ReporterType reporterType;

    public static StaticConfig create(boolean perSecondEnabled, ReporterType reporterType) {
        StaticConfig config = new StaticConfig();
        config.perSecondEnabled = perSecondEnabled;
        config.reporterType = reporterType;
        return config;
    }

    @Override
    public boolean isPerSecondEnabled() {
        return perSecondEnabled;
    }

    @Override
    public ReporterType getReporterType() {
        return reporterType;
    }
}
