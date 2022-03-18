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

    private static class SecondStatsBlock {
        public final String tickSource;
        public final Instant timestamp;
        public final long tickCount;
        public final long totalTickMillis;
        public final long minTickMillis;
        public final long maxTickMillis;

        public SecondStatsBlock(String tickSource, Instant timestamp, long tickCount,
                                long totalTickMillis, long minTickMillis, long maxTickMillis) {
            this.tickSource = tickSource;
            this.timestamp = timestamp;
            this.tickCount = tickCount;
            this.totalTickMillis = totalTickMillis;
            this.minTickMillis = minTickMillis;
            this.maxTickMillis = maxTickMillis;
        }
    }

    private static class MinuteStatsBlock {
        public final String tickSource;
        public final Instant timestamp;
        public final long datapointCount;
        public final long totalTickCount;
        public final long minTickCount;
        public final long maxTickCount;
        public final long totalTickMillis;
        public final long minTickMillis;
        public final long maxTickMillis;

        public MinuteStatsBlock(String tickSource, Instant timestamp, long datapointCount,
                                long totalTickCount, long minTickCount, long maxTickCount,
                                long totalTickMillis, long minTickMillis, long maxTickMillis) {
            this.tickSource = tickSource;
            this.timestamp = timestamp;
            this.datapointCount = datapointCount;
            this.totalTickCount = totalTickCount;
            this.minTickCount = minTickCount;
            this.maxTickCount = maxTickCount;
            this.totalTickMillis = totalTickMillis;
            this.minTickMillis = minTickMillis;
            this.maxTickMillis = maxTickMillis;
        }
    }

    private static class InMemoryTickStatsReporter implements TickStatsReporter {

        private final List<SecondStatsBlock> secondBlocks = new ArrayList<>();
        private final List<MinuteStatsBlock> minuteBlocks = new ArrayList<>();

        public void clear() {
            secondBlocks.clear();
            minuteBlocks.clear();
        }

        public List<SecondStatsBlock> getSecondBlocks() {
            return secondBlocks;
        }

        public List<MinuteStatsBlock> getMinuteBlocks() {
            return minuteBlocks;
        }

        @Override
        public void reportSecond(String tickSource, Instant timestamp, long tickCount,
                                 long totalTickMillis, long minTickMillis, long maxTickMillis) {
            secondBlocks.add(new SecondStatsBlock(tickSource, timestamp, tickCount,
                                                  totalTickMillis, minTickMillis, maxTickMillis));
        }

        @Override
        public void reportMinute(String tickSource, Instant timestamp, long datapointCount,
                                 long totalTickCount, long minTickCount, long maxTickCount,
                                 long totalTickMillis, long minTickMillis, long maxTickMillis) {
            minuteBlocks.add(new MinuteStatsBlock(tickSource, timestamp, datapointCount,
                                                  totalTickCount, minTickCount, maxTickCount,
                                                  totalTickMillis, minTickMillis, maxTickMillis));
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
        SecondStatsBlock block = reporter.getSecondBlocks().get(0);

        Assertions.assertEquals(20, block.tickCount);
        Assertions.assertEquals(5, block.minTickMillis);
        Assertions.assertEquals(20 * 5, block.totalTickMillis);
        Assertions.assertEquals(5, block.maxTickMillis);
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
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondBlocks().size());
        SecondStatsBlock block = reporter.getSecondBlocks().get(0);

        Assertions.assertEquals(20, block.tickCount);
        Assertions.assertEquals(5, block.minTickMillis);
        Assertions.assertEquals(totalTickTime, block.totalTickMillis);
        Assertions.assertEquals(15, block.maxTickMillis);
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
        SecondStatsBlock block = reporter.getSecondBlocks().get(0);

        // we should only have one data point for that second, a zero-second tick.
        Assertions.assertEquals(1, block.tickCount);
        Assertions.assertEquals(0, block.minTickMillis);
        Assertions.assertEquals(0, block.totalTickMillis);
        Assertions.assertEquals(0, block.maxTickMillis);
    }

    @Test
    public void testManyTicksInOneSecond() {
        InMemoryTickStatsReporter reporter = new InMemoryTickStatsReporter();
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
        Assertions.assertTrue(reporter.getSecondBlocks().isEmpty());

        // We need to trigger another tick so the stats get emitted
        clock.forward(Duration.ofSeconds(1));
        tracker.startTick();
        tracker.endTick();

        // now that second has passed we should have a stats block
        Assertions.assertFalse(reporter.getSecondBlocks().isEmpty());

        Assertions.assertEquals(1, reporter.getSecondBlocks().size());
        SecondStatsBlock block = reporter.getSecondBlocks().get(0);

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
