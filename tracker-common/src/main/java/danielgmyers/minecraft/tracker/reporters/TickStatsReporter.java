package danielgmyers.minecraft.tracker.reporters;

import danielgmyers.minecraft.tracker.MinuteTickStatsBlock;
import danielgmyers.minecraft.tracker.SecondTickStatsBlock;

public interface TickStatsReporter {

    void report(String tickSource, SecondTickStatsBlock stats);

    void report(String tickSource, MinuteTickStatsBlock stats);
}
