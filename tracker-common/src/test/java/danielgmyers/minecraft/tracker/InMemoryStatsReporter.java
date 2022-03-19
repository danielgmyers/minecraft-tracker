package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.reporters.StatsReporter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InMemoryStatsReporter implements StatsReporter {

    private final List<SecondTickStatsBlock> secondTickBlocks = new ArrayList<>();
    private final List<MinuteTickStatsBlock> minuteTickBlocks = new ArrayList<>();
    private final List<MinutePlayerCountStatsBlock> minutePlayerCountsBlock = new ArrayList<>();

    public void clear() {
        secondTickBlocks.clear();
        minuteTickBlocks.clear();
        minutePlayerCountsBlock.clear();
    }

    public List<SecondTickStatsBlock> getSecondTickBlocks() {
        return secondTickBlocks;
    }

    public List<MinuteTickStatsBlock> getMinuteTickBlocks() {
        return minuteTickBlocks;
    }

    public List<MinutePlayerCountStatsBlock> getMinutePlayerCountsBlock() {
        return minutePlayerCountsBlock;
    }

    @Override
    public void reportSecond(String tickSource, Instant timestamp, long tickCount,
                             long totalTickMillis, long minTickMillis, long maxTickMillis) {
        secondTickBlocks.add(new SecondTickStatsBlock(tickSource, timestamp, tickCount,
                                                      totalTickMillis, minTickMillis, maxTickMillis));
    }

    @Override
    public void reportMinute(String tickSource, Instant timestamp, long datapointCount,
                             long totalTickCount, long minTickCount, long maxTickCount,
                             long totalTickMillis, long minTickMillis, long maxTickMillis) {
        minuteTickBlocks.add(new MinuteTickStatsBlock(tickSource, timestamp, datapointCount,
                                                      totalTickCount, minTickCount, maxTickCount,
                                                      totalTickMillis, minTickMillis, maxTickMillis));
    }

    @Override
    public void reportPlayerCount(String tickSource, Instant timestamp, long datapointCount,
                                  long playerCountSum, long minPlayerCount, long maxPlayerCount) {
        minutePlayerCountsBlock.add(new MinutePlayerCountStatsBlock(tickSource, timestamp, datapointCount,
                                                                    playerCountSum, minPlayerCount, maxPlayerCount));
    }
}
