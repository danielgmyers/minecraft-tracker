package danielgmyers.minecraft.tracker.reporters;

import java.time.Instant;

public interface StatsReporter {

    // we need to know how many seconds we have datapoints for, because if a tick took (for example) two seconds,
    // then we won't get data for that second.
    void reportTickStats(String tickSource, Instant timestamp, long secondsWithData,
                         long totalTickCount, long minTickCount, long maxTickCount,
                         long totalTickMillis, long minTickMillis, long maxTickMillis);

    void reportPlayerCount(String tickSource, Instant timestamp, long secondsWithData,
                           long playerCountSum, long minPlayerCount, long maxPlayerCount);
}
