package danielgmyers.minecraft.tracker;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.ReporterType;
import danielgmyers.minecraft.tracker.reporters.TickStatsReporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

public class TickStatsTrackerTest {

    private class InMemoryTickStatsReporter implements TickStatsReporter {

        private List<SecondTickStatsBlock> secondBlocks = new ArrayList<>();
        private List<MinuteTickStatsBlock> minuteBlocks = new ArrayList<>();

        @Override
        public void report(String tickSource, SecondTickStatsBlock stats) {
            this.secondBlocks.add(stats);
        }

        @Override
        public void report(String tickSource, MinuteTickStatsBlock stats) {
            this.minuteBlocks.add(stats);
        }

        public void clear() {
            secondBlocks.clear();
            minuteBlocks.clear();
        }

        public List<SecondTickStatsBlock> getSecondBlocks() {
            return secondBlocks;
        }

        public List<MinuteTickStatsBlock> getMinuteBlocks() {
            return minuteBlocks;
        }
    }

    @Test
    public void testInitialization() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);
    }

    @Test
    public void testConstantTickTimesForOneSecond() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
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
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondBlocks().get(0);

        Assertions.assertEquals(20, block.getTickCount());
        Assertions.assertEquals(5, block.getMinTickMillis());
        Assertions.assertEquals(5, block.getAvgTickMillis());
        Assertions.assertEquals(5, block.getMaxTickMillis());
    }

    @Test
    public void testRespectsPerSecondStatsConfig() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
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
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats would emitted if they're enabled
        tracker.startTick();
        tracker.endTick();

        // since per-second stats are disabled, we should still have no reported per-second stats.
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());
    }

    @Test
    public void testVariableTickTimesForOneSecond() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // For this test, we'll have two ticks take 5ms, sixteen ticks take 10ms,
        // and two more ticks take 15ms.
        // The average should be 10, min and max should be 5 and 15, respectively.
        for (int i = 0; i < 2; i++) {
            doTick(tracker, clock, 5, 45);
        }
        for (int i = 0; i < 16; i++) {
            doTick(tracker, clock, 10, 40);
        }
        for (int i = 0; i < 2; i++) {
            doTick(tracker, clock, 15, 35);
        }

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondBlocks().get(0);

        Assertions.assertEquals(20, block.getTickCount());
        Assertions.assertEquals(5, block.getMinTickMillis());
        Assertions.assertEquals(10, block.getAvgTickMillis());
        Assertions.assertEquals(15, block.getMaxTickMillis());
    }

    @Test
    public void testEndTickBeforeStartTick() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // This shouldn't cause an exception, and the subsequent tick should have data.
        tracker.endTick();

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        clock.forward(Duration.ofSeconds(1));
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondBlocks().get(0);

        // we should only have one data point for that second, a zero-second tick.
        Assertions.assertEquals(1, block.getTickCount());
        Assertions.assertEquals(0, block.getMinTickMillis());
        Assertions.assertEquals(0, block.getAvgTickMillis());
        Assertions.assertEquals(0, block.getMaxTickMillis());
    }

    @Test
    public void testTooManyTicksInOneSecond() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
        TestClock clock = new TestClock(Instant.now().with(ChronoField.NANO_OF_SECOND, 0));
        Config testConfig = StaticConfig.create(true, ReporterType.APPLICATION_LOG);
        TickStatsTracker tracker = new TickStatsTracker("test", testConfig, reporter, clock);

        // For this test, we'll have each tick take 1ms, with a 1ms gap between ticks.
        // That's room for 500 ticks in one second
        // This simulates the game running way too many ticks.
        Assertions.assertTrue(TickStatsTracker.MAX_TICKS * 2 < 500);
        for (int i = 0; i < TickStatsTracker.MAX_TICKS * 2; i++) {
            doTick(tracker, clock, 1, 1);
        }

        // we shouldn't have any reported stats yet, the second needs to tick over
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        clock.forward(Duration.ofSeconds(1));
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondBlocks().size());
        SecondTickStatsBlock block = reporter.getSecondBlocks().get(0);

        // strictly speaking, the implementation doesn't need to include data
        // for the overflow ticks in the duration metrics, only the first TickStatsTracker.MAX_TICKS,
        // but we're testing with constant-time ticks, so it makes no difference.
        Assertions.assertEquals(TickStatsTracker.MAX_TICKS * 2, block.getTickCount());
        Assertions.assertEquals(1, block.getMinTickMillis());
        Assertions.assertEquals(1, block.getAvgTickMillis());
        Assertions.assertEquals(1, block.getMaxTickMillis());
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
