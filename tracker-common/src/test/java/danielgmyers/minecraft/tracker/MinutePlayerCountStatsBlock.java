package danielgmyers.minecraft.tracker;

import java.time.Instant;

public class MinutePlayerCountStatsBlock {
    public final String tickSource;
    public final Instant timestamp;
    public final long datapointCount;
    public final long playerCountSum;
    public final long minPlayerCount;
    public final long maxPlayerCount;

    public MinutePlayerCountStatsBlock(String tickSource, Instant timestamp, long datapointCount,
                                       long playerCountSum, long minPlayerCount, long maxPlayerCount) {
        this.tickSource = tickSource;
        this.timestamp = timestamp;
        this.datapointCount = datapointCount;
        this.playerCountSum = playerCountSum;
        this.minPlayerCount = minPlayerCount;
        this.maxPlayerCount = maxPlayerCount;
    }
}
