package danielgmyers.minecraft.tracker;

import java.time.Instant;

public class SecondTickStatsBlock {
    public final String tickSource;
    public final Instant timestamp;
    public final long tickCount;
    public final long totalTickMillis;
    public final long minTickMillis;
    public final long maxTickMillis;

    public SecondTickStatsBlock(String tickSource, Instant timestamp, long tickCount,
                                long totalTickMillis, long minTickMillis, long maxTickMillis) {
        this.tickSource = tickSource;
        this.timestamp = timestamp;
        this.tickCount = tickCount;
        this.totalTickMillis = totalTickMillis;
        this.minTickMillis = minTickMillis;
        this.maxTickMillis = maxTickMillis;
    }
}
