package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.ReporterType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;

public class TickStatsTrackerTest {

    @Test
    public void testInitialization() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);
    }

    @Test
    public void testConstantTickTimesForOneSecond() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // 20 ticks in 1 second allows 50ms per tick.
        // In practice, ticks often take less time than that, where the game waits a while
        // to start the next tick.

        // For this test, we'll have each tick take 5ms, and a 45ms gap between ticks.
        for (int i = 0; i < 20; i++) {
            doTick(tracker, clock, 5, 45);
        }

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondTickBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondTickBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondTickBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondTickBlocks().get(0);

        Assertions.assertEquals(20, block.tickCount);
        Assertions.assertEquals(5, block.minTickMillis);
        Assertions.assertEquals(20 * 5, block.totalTickMillis);
        Assertions.assertEquals(5, block.maxTickMillis);
    }

    @Test
    public void testRespectsPerSecondStatsConfig() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(false, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // 20 ticks in 1 second allows 50ms per tick.
        // In practice, ticks often take less time than that, where the game waits a while
        // to start the next tick.

        // For this test, we'll have each tick take 5ms, and a 45ms gap between ticks.
        for (int i = 0; i < 20; i++) {
            doTick(tracker, clock, 5, 45);
        }

        // we normally wouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondTickBlocks().isEmpty());

        // We need to trigger another tick so the stats would emitted if they're enabled
        tracker.startTick();
        tracker.endTick();

        // since per-second stats are disabled, we should still have no reported per-second stats.
        Assertions.assertTrue(reporter.getSecondTickBlocks().isEmpty());
    }

    @Test
    public void testVariableTickTimesForOneSecond() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // For this test, we'll have two ticks take 5ms, sixteen ticks take 10ms,
        // and two more ticks take 15ms.
        int totalTickTime = 0;
        for (int i = 0; i < 2; i++) {
            doTick(tracker, clock, 5, 45);
            totalTickTime += 5;
        }
        for (int i = 0; i < 16; i++) {
            doTick(tracker, clock, 10, 40);
            totalTickTime += 10;
        }
        for (int i = 0; i < 2; i++) {
            doTick(tracker, clock, 15, 35);
            totalTickTime += 15;
        }

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondTickBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondTickBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondTickBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondTickBlocks().get(0);

        Assertions.assertEquals(20, block.tickCount);
        Assertions.assertEquals(5, block.minTickMillis);
        Assertions.assertEquals(totalTickTime, block.totalTickMillis);
        Assertions.assertEquals(15, block.maxTickMillis);
    }

    @Test
    public void testEndTickBeforeStartTick() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // This shouldn't cause an exception, and the subsequent tick should have data.
        tracker.endTick();

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondTickBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        clock.forward(Duration.ofSeconds(1));
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondTickBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondTickBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondTickBlocks().get(0);

        // we should only have one data point for that second, a zero-second tick.
        Assertions.assertEquals(1, block.tickCount);
        Assertions.assertEquals(0, block.minTickMillis);
        Assertions.assertEquals(0, block.totalTickMillis);
        Assertions.assertEquals(0, block.maxTickMillis);
    }

    @Test
    public void testManyTicksInOneSecond() {
        InMemoryStatsReporter reporter = new InMemoryStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // For this test, we'll have each tick take 1ms, with a 1ms gap between ticks.
        // That's room for 500 ticks in one second
        // This simulates the game running significantly more ticks than usual.
        long tickCount = 400;
        for (int i = 0; i < tickCount; i++) {
            doTick(tracker, clock, 1, 1);
        }

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondTickBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        clock.forward(Duration.ofSeconds(1));
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondTickBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondTickBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondTickBlocks().get(0);

        Assertions.assertEquals(tickCount, block.tickCount);
        Assertions.assertEquals(1, block.minTickMillis);
        Assertions.assertEquals(400, block.totalTickMillis);
        Assertions.assertEquals(1, block.maxTickMillis);
    }

    private void doTick(TickStatsTracker tracker, TestClock clock, long tickMillis, long postTickWaitMillis) {
        tracker.startTick();
        clock.forward(Duration.ofMillis(tickMillis));
        tracker.endTick();
        if (postTickWaitMillis > 0) {
            clock.forward(Duration.ofMillis(postTickWaitMillis));
        }
    }
}
