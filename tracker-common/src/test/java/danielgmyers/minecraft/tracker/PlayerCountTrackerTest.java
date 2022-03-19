package danielgmyers.minecraft.tracker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class PlayerCountTrackerTest {

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
    public void testConstantPlayerCountsForOneMinute() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        StaticConfig testConfig = StaticConfig.create();
        PlayerCountTracker tracker = new PlayerCountTracker(TICK_SOURCE, testConfig, reporter, clock);

        // the tracker will get updated once per tick, but we only want counts every second.
        for (int i = 0; i < 20 * 60; i++) {
            tracker.update(2);
            clock.forward(Duration.ofMillis(50));
        }

        // we shouldn't have any reported counts yet, the didn't tick over until after the last update
        Assertions.assertTrue(reporter.getPlayerCounts().isEmpty());

        // We need to trigger another update so the stats get emitted
        tracker.update(2);

        // now that a minute has passed we should have a stats block
        Assertions.assertFalse(reporter.getPlayerCounts().isEmpty());

        Assertions.assertEquals(1, reporter.getPlayerCounts().size());
        PlayerCountStatsBlock block = reporter.getPlayerCounts().get(0);

        Assertions.assertEquals(TICK_SOURCE, block.tickSource);

        // the tracker should have used the timestamp of the latest update, which is now
        Assertions.assertEquals(clock.instant(), block.timestamp);

        Assertions.assertEquals(60, block.datapointCount);

        Assertions.assertEquals(2 * 60, block.playerCountSum);
        Assertions.assertEquals(2, block.minPlayerCount);
        Assertions.assertEquals(2, block.maxPlayerCount);
    }

}
