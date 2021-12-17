package danielgmyers.minecraft.tracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.Arrays;

public class TickStatsTracker {

    private static final Logger LOGGER = LogManager.getLogger();

    // As it turns out, Minecraft isn't particularly strict about TPS.
    // At startup, it'll run 30 or more TPS for a few seconds, and later on it'll bounce up to 21 sometimes.
    // Just in case, we'll handle up to 60TPS, and drop data above that.
    // Visible for test access.
    static final int MAX_TICKS = 60;

    private final String tickSource;
    private long previousTickStartTimeMillis = 0;
    private long previousTickEndTimeMillis = 0;
    private long currentTickStartTimeMillis = 0;
    private int tickCountThisSecond = 0;
    private final long[] tickMillisForThisSecond = new long[MAX_TICKS];

    // these are all per-second summaries for the last minute
    private int lastMinuteSecondsCounter = 0;
    private final long[] tickCountsDuringLastMinute = new long[60];
    private final long[] minTickDurationsDuringLastMinute = new long[60];
    private final long[] avgTickDurationsDuringLastMinute = new long[60];
    private final long[] maxTickDurationsDuringLastMinute = new long[60];

    private final TickStatsReporter reporter;
    private final Clock clock;

    public TickStatsTracker(String tickSource, TickStatsReporter reporter, Clock clock) {
        this.tickSource = tickSource;
        this.reporter = reporter;
        this.clock = clock;
        LOGGER.info("Initializing tick stats tracker for source '{}'", tickSource);
    }

    public void startTick() {
        currentTickStartTimeMillis = clock.millis();
    }

    public void endTick() {
        long currentTickEndTimeMillis = clock.millis();

        // In this case, startTick was never called, so we'll treat it as a 0-ms tick.
        // Theoretically this can happen if the tracker wasn't initialized until partway through a tick,
        // and so endTick() was called first.
        if (currentTickStartTimeMillis == 0) {
            currentTickStartTimeMillis = currentTickEndTimeMillis;
        }

        // If we don't have a previous end tick time, this is the first tick.
        // We'll pretend the previous tick happened already by copying this tick's data.
        // It won't get used for anything except to determine if we're in the next second or minute,
        // which won't be the case if the values are equal.
        if (previousTickEndTimeMillis == 0) {
            previousTickStartTimeMillis = currentTickStartTimeMillis;
            previousTickEndTimeMillis = currentTickEndTimeMillis;
        }

        // we use the tick start times to figure out if we're in the next second, so that
        // variable-duration ticks don't confuse us
        if (inNextSecond(previousTickStartTimeMillis, currentTickStartTimeMillis) && tickCountThisSecond > 0) {
            long minTickDurationInLastSecond = Long.MAX_VALUE;
            long maxTickDurationInLastSecond = 0;
            long totalTicksDuration = 0;

            long ticksWithData = Math.min(tickCountThisSecond, MAX_TICKS);
            for (int i = 0; i < ticksWithData; i++) {
                long tickDuration = tickMillisForThisSecond[i];
                minTickDurationInLastSecond = Math.min(minTickDurationInLastSecond, tickDuration);
                maxTickDurationInLastSecond = Math.max(maxTickDurationInLastSecond, tickDuration);
                totalTicksDuration += tickDuration;
            }

            // for now, not going to worry about the integer division losing precision.
            long avgTickDurationInLastSecond = totalTicksDuration / ticksWithData;

            SecondTickStatsBlock secondStats = new SecondTickStatsBlock(tickCountThisSecond,
                    minTickDurationInLastSecond, avgTickDurationInLastSecond, maxTickDurationInLastSecond);
            reporter.report(tickSource, secondStats);

            if (lastMinuteSecondsCounter >= 60) {
                LOGGER.error("{} tick tracker - We got too many seconds during the last minute!", tickSource);
            } else {
                tickCountsDuringLastMinute[lastMinuteSecondsCounter] = tickCountThisSecond;
                minTickDurationsDuringLastMinute[lastMinuteSecondsCounter] = minTickDurationInLastSecond;
                avgTickDurationsDuringLastMinute[lastMinuteSecondsCounter] = avgTickDurationInLastSecond;
                maxTickDurationsDuringLastMinute[lastMinuteSecondsCounter] = maxTickDurationInLastSecond;
            }

            tickCountThisSecond = 0;
            lastMinuteSecondsCounter++;

            if (inNextMinute(previousTickStartTimeMillis, currentTickStartTimeMillis)) {
                // we may not have a record for all the seconds (e.g. if a server tick took more than a second),
                // so we have to work off of how many seconds we recorded data for rather than 60.
                // It's guaranteed to be non-zero, because we just put at least one entry into the list.

                // TODO -- I think this will generate wrong data if multiple seconds were skipped.
                long minTickCount = Long.MAX_VALUE;
                long maxTickCount = 0;
                long totalTickCount = 0;
                for (int i = 0; i < lastMinuteSecondsCounter; i++) {
                    long count = tickCountsDuringLastMinute[i];
                    minTickCount = Math.min(minTickCount, count);
                    maxTickCount = Math.max(maxTickCount, count);
                    totalTickCount += count;
                }
                // for now, not going to worry about the integer division losing precision
                long avgTickCount = totalTickCount / lastMinuteSecondsCounter;

                long minTickDurationLastMinute = Arrays.stream(minTickDurationsDuringLastMinute, 0, lastMinuteSecondsCounter).min().orElse(0);
                // for now, not going to worry about the integer division losing precision
                long avgTickDurationLastMinute = (long)(Arrays.stream(avgTickDurationsDuringLastMinute, 0, lastMinuteSecondsCounter).average().orElse(0.0));
                long maxTickDurationLastMinute = Arrays.stream(maxTickDurationsDuringLastMinute, 0, lastMinuteSecondsCounter).max().orElse(0);

                MinuteTickStatsBlock minuteStats = new MinuteTickStatsBlock(lastMinuteSecondsCounter,
                        minTickCount, avgTickCount, maxTickCount,
                        minTickDurationLastMinute, avgTickDurationLastMinute, maxTickDurationLastMinute);
                reporter.report(tickSource, minuteStats);

                lastMinuteSecondsCounter = 0;
            }
        }

        // finally, we add the current tick's duration to the tick duration queue.
        long currentTickDurationMillis = currentTickEndTimeMillis - currentTickStartTimeMillis;

        tickCountThisSecond++;
        if (tickCountThisSecond > MAX_TICKS) {
            LOGGER.warn("{} tick tracker - too many ticks during the last second ({})!", tickSource, tickCountThisSecond);
        } else {
            tickMillisForThisSecond[tickCountThisSecond-1] = currentTickDurationMillis;
        }
        previousTickStartTimeMillis = currentTickStartTimeMillis;
        previousTickEndTimeMillis = currentTickEndTimeMillis;
    }

    private boolean inNextSecond(long previousTimeMillis, long currentTimeMillis) {
        return (previousTimeMillis / 1000) < (currentTimeMillis / 1000);
    }

    private boolean inNextMinute(long previousTimeMillis, long currentTimeMillis) {
        // this won't line up with wall-clock minutes, but that's ok, we just need consistent 60-second windows.
        return (previousTimeMillis / 60000) < (currentTimeMillis / 60000);
    }
}
