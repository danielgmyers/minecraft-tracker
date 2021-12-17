package danielgmyers.minecraft.tracker;

public class MinuteTickStatsBlock {

    public MinuteTickStatsBlock(long datapointCount,
                                long minTickCount, long avgTickCount, long maxTickCount,
                                long minTickMillis, long avgTickMillis, long maxTickMillis) {
        this.datapointCount = datapointCount;
        this.minTickCount = minTickCount;
        this.avgTickCount = avgTickCount;
        this.maxTickCount = maxTickCount;
        this.minTickMillis = minTickMillis;
        this.avgTickMillis = avgTickMillis;
        this.maxTickMillis = maxTickMillis;
    }

    private final long datapointCount;

    private final long minTickCount;
    private final long avgTickCount;
    private final long maxTickCount;

    private final long minTickMillis;
    private final long avgTickMillis;
    private final long maxTickMillis;

    public long getDatapointCount() {
        return datapointCount;
    }

    public long getMinTickCount() {
        return minTickCount;
    }

    public long getAvgTickCount() {
        return avgTickCount;
    }

    public long getMaxTickCount() {
        return maxTickCount;
    }

    public long getMinTickMillis() {
        return minTickMillis;
    }

    public long getAvgTickMillis() {
        return avgTickMillis;
    }

    public long getMaxTickMillis() {
        return maxTickMillis;
    }
}
