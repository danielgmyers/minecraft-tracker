package danielgmyers.minecraft.tracker.config;

public class Config {
    private boolean enabled;
    private boolean perSecondEnabled;
    private ReporterType reporterType;

    public Config() {
        enabled = false;
        perSecondEnabled = false;
        reporterType = ReporterType.APPLICATION_LOG;
    }

    public void load(boolean enabled, boolean perSecondEnabled, String reporterType) {
        this.load(enabled, perSecondEnabled, ReporterType.fromString(reporterType));
    }

    public void load(boolean enabled, boolean perSecondEnabled, ReporterType reporterType) {
        this.enabled = enabled;
        this.perSecondEnabled = perSecondEnabled;
        this.reporterType = reporterType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPerSecondEnabled() {
        return perSecondEnabled;
    }

    public ReporterType getReporterType() {
        return reporterType;
    }
}
