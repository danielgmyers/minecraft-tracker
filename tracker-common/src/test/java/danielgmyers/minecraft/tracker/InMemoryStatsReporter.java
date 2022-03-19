package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.reporters.StatsReporter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InMemoryStatsReporter implements StatsReporter {

    private final List<TickStatsBlock> tickStats = new ArrayList<>();
    private final List<PlayerCountStatsBlock> playerCounts = new ArrayList<>();

    public void clear() {
        tickStats.clear();
        playerCounts.clear();
    }

    public List<TickStatsBlock> getTickStats() {
        return tickStats;
    }

    public List<PlayerCountStatsBlock> getPlayerCounts() {
        return playerCounts;
    }

    @Override
    public void reportTickStats(String tickSource, Instant timestamp, long secondsWithData,
                                long totalTickCount, long minTickCount, long maxTickCount,
                                long totalTickMillis, long minTickMillis, long maxTickMillis) {
        tickStats.add(new TickStatsBlock(tickSource, timestamp, secondsWithData,
                                         totalTickCount, minTickCount, maxTickCount,
                                         totalTickMillis, minTickMillis, maxTickMillis));
    }

    @Override
    public void reportPlayerCount(String tickSource, Instant timestamp, long secondsWithData,
                                  long playerCountSum, long minPlayerCount, long maxPlayerCount) {
        playerCounts.add(new PlayerCountStatsBlock(tickSource, timestamp, secondsWithData,
                                                   playerCountSum, minPlayerCount, maxPlayerCount));
    }
}
