package danielgmyers.minecraft.tracker.config;

public enum ReporterType {
    APPLICATION_LOG,
    CLOUDWATCH_DIRECT,
    CLOUDWATCH_LOGS_EMF;

    public static ReporterType fromString(String str) {
        for(ReporterType type : values()) {
            if (type.toString().equalsIgnoreCase(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unrecognized reporter type " + str);
    }
}
