package danielgmyers.minecraft.tracker;

public class SecondTickStatsBlock {

    public SecondTickStatsBlock(long tickCount, long minTickMillis, long avgTickMillis, long maxTickMillis) {
        this.tickCount = tickCount;
        this.minTickMillis = minTickMillis;
        this.avgTickMillis = avgTickMillis;
        this.maxTickMillis = maxTickMillis;
    }

    private final long tickCount;

    private final long minTickMillis;
    private final long avgTickMillis;
    private final long maxTickMillis;

    public long getTickCount() {
        return tickCount;
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
