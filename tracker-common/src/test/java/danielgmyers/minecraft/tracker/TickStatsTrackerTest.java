package danielgmyers.minecraft.tracker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class TickStatsTrackerTest {

    private static final String TICK_SOURCE = "test-tick-source";

    private TestClock clock;

    @BeforeEach
    public void setupClock() {
        // set up the clock so that it's clamped to the beginning of the current minute.
        // this will make sure we're able to consistently test crossing minute boundaries.
        Instant startTime = Instant.now().with(ChronoField.NANO_OF_SECOND, 0);
        LocalDateTime ldt = LocalDateTime.ofInstant(startTime, ZoneId.systemDefault());
        startTime = startTime.minusSeconds(ldt.getSecond());
        clock = new TestClock(startTime);
    }

    @Test
    public void testConstantTickTimesForOneMinute() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        StaticConfig testConfig = StaticConfig.create();
        TickStatsTracker tracker = new TickStatsTracker(TICK_SOURCE, testConfig, reporter, clock);

        // 20 ticks in 1 second allows 50ms per tick.
        // In practice, ticks often take less time than that, where the game waits a while
        // to start the next tick.

        Instant previousEndTickTime = null;
        // For this test, we'll have each tick take 5ms, and a 45ms gap between ticks.
        for (int i = 0; i < 20 * 60; i++) {
            previousEndTickTime = doTick(tracker, clock, 5, 45);
        }

        // we shouldn't have any reported stats yet, the minute needs to tick over
        Assertions.assertTrue(reporter.getTickStats().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        clock.forward(Duration.ofMillis(5)); // let's say this tick took 5ms, shouldn't affect the stats
        tracker.endTick();

        // now that a minute has passed we should have a stats block
        Assertions.assertFalse(reporter.getTickStats().isEmpty());

        Assertions.assertEquals(1, reporter.getTickStats().size());
        TickStatsBlock block = reporter.getTickStats().get(0);

        Assertions.assertEquals(TICK_SOURCE, block.tickSource);

        // the tracker should have used the timestamp of the _previous_ end tick, which is previousEndTickTime
        Assertions.assertEquals(previousEndTickTime, block.timestamp);

        Assertions.assertEquals(60, block.secondsWithData);

        Assertions.assertEquals(20 * 60, block.totalTickCount);
        Assertions.assertEquals(20, block.minTickCount);
        Assertions.assertEquals(20, block.maxTickCount);

        Assertions.assertEquals(20 * 5 * 60, block.totalTickMillis);
        Assertions.assertEquals(5, block.minTickMillis);
        Assertions.assertEquals(5, block.maxTickMillis);
    }

    @Test
    public void testVariableTickTimesForOneMinute() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        StaticConfig testConfig = StaticConfig.create();
        TickStatsTracker tracker = new TickStatsTracker(TICK_SOURCE, testConfig, reporter, clock);

        Instant previousEndTickTime = null;
        // For this test, for each second we'll have two ticks take 5ms, sixteen ticks take 10ms,
        // and two more ticks take 15ms.
        int totalTickTime = 0;
        for (int j = 0; j < 60; j++) {
            for (int i = 0; i < 2; i++) {
                doTick(tracker, clock, 5, 45);
                totalTickTime += 5;
            }
            for (int i = 0; i < 16; i++) {
                doTick(tracker, clock, 10, 40);
                totalTickTime += 10;
            }
            for (int i = 0; i < 2; i++) {
                previousEndTickTime = doTick(tracker, clock, 15, 35);
                totalTickTime += 15;
            }
        }

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getTickStats().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        clock.forward(Duration.ofMillis(5)); // let's say this tick took 5ms, shouldn't affect the stats
        tracker.endTick();

        // now that the minute has passed we should have a stats block
        Assertions.assertFalse(reporter.getTickStats().isEmpty());

        Assertions.assertEquals(1, reporter.getTickStats().size());
        TickStatsBlock block = reporter.getTickStats().get(0);

        Assertions.assertEquals(TICK_SOURCE, block.tickSource);

        // the tracker should have used the timestamp of the _previous_ end tick, which is previousEndTickTime
        Assertions.assertEquals(previousEndTickTime, block.timestamp);

        Assertions.assertEquals(60, block.secondsWithData);

        Assertions.assertEquals(20 * 60, block.totalTickCount);
        Assertions.assertEquals(20, block.minTickCount);
        Assertions.assertEquals(20, block.maxTickCount);

        Assertions.assertEquals(20 * 10 * 60, block.totalTickMillis);
        Assertions.assertEquals(5, block.minTickMillis);
        Assertions.assertEquals(15, block.maxTickMillis);
    }

    @Test
    public void testEndTickBeforeStartTick() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        StaticConfig testConfig = StaticConfig.create();
        TickStatsTracker tracker = new TickStatsTracker(TICK_SOURCE, testConfig, reporter, clock);

        // This shouldn't cause an exception, and the subsequent tick should have data.
        tracker.endTick();
        Instant previousEndTickTime = clock.instant();

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getTickStats().isEmpty());

        // We need to trigger another tick so the stats get emitted
        clock.forward(Duration.ofSeconds(60));
        tracker.startTick();
        clock.forward(Duration.ofMillis(5));
        tracker.endTick();

        // now that a minute has passed we should have a stats block
        Assertions.assertFalse(reporter.getTickStats().isEmpty());

        Assertions.assertEquals(1, reporter.getTickStats().size());
        TickStatsBlock block = reporter.getTickStats().get(0);

        Assertions.assertEquals(TICK_SOURCE, block.tickSource);

        // the tracker should have used the timestamp of the _previous_ end tick, which is previousEndTickTime
        Assertions.assertEquals(previousEndTickTime, block.timestamp);

        // we should see a single zero-duration tick
        Assertions.assertEquals(1, block.secondsWithData);

        Assertions.assertEquals(1, block.totalTickCount);
        Assertions.assertEquals(1, block.minTickCount);
        Assertions.assertEquals(1, block.maxTickCount);

        Assertions.assertEquals(0, block.totalTickMillis);
        Assertions.assertEquals(0, block.minTickMillis);
        Assertions.assertEquals(0, block.maxTickMillis);
    }


    /**
     * Returns the timestamp of the endTick call
     */
    private Instant doTick(TickStatsTracker tracker, TestClock clock, long tickMillis, long postTickWaitMillis) {
        tracker.startTick();
        clock.forward(Duration.ofMillis(tickMillis));
        tracker.endTick();
        Instant endTickTime = clock.instant();
        if (postTickWaitMillis > 0) {
            clock.forward(Duration.ofMillis(postTickWaitMillis));
        }
        return endTickTime;
    }
}
