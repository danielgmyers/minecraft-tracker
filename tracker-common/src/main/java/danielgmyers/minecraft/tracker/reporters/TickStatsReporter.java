package danielgmyers.minecraft.tracker.reporters;

import java.time.Instant;

public interface TickStatsReporter {

    void reportSecond(String tickSource, Instant timestamp,
                      long tickCount, long totalTickMillis, long minTickMillis, long maxTickMillis);

    // we need to know how many seconds we have datapoints for, because if a tick took (for example) two seconds,
    // then we won't get data for that second.
    void reportMinute(String tickSource, Instant timestamp, long datapointCount,
                      long totalTickCount, long minTickCount, long maxTickCount,
                      long totalTickMillis, long minTickMillis, long maxTickMillis);
}
