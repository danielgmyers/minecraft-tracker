package danielgmyers.minecraft.tracker;

import java.time.Instant;

public class MinuteTickStatsBlock {
    public final String tickSource;
    public final Instant timestamp;
    public final long datapointCount;
    public final long totalTickCount;
    public final long minTickCount;
    public final long maxTickCount;
    public final long totalTickMillis;
    public final long minTickMillis;
    public final long maxTickMillis;

    public MinuteTickStatsBlock(String tickSource, Instant timestamp, long datapointCount,
                                long totalTickCount, long minTickCount, long maxTickCount,
                                long totalTickMillis, long minTickMillis, long maxTickMillis) {
        this.tickSource = tickSource;
        this.timestamp = timestamp;
        this.datapointCount = datapointCount;
        this.totalTickCount = totalTickCount;
        this.minTickCount = minTickCount;
        this.maxTickCount = maxTickCount;
        this.totalTickMillis = totalTickMillis;
        this.minTickMillis = minTickMillis;
        this.maxTickMillis = maxTickMillis;
    }
}
