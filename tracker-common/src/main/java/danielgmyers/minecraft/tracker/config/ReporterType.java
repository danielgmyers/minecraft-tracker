package danielgmyers.minecraft.tracker.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum ReporterType {
    NONE,
    APPLICATION_LOG,
    CLOUDWATCH_DIRECT,
    CLOUDWATCH_LOGS_EMF;

    private static final Logger LOG = LogManager.getLogger();

}
