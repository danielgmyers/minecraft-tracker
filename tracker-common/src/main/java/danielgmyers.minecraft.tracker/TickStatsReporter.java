package danielgmyers.minecraft.tracker;

public interface TickStatsReporter {

    void report(String tickSource, SecondTickStatsBlock stats);

    void report(String tickSource, MinuteTickStatsBlock stats);
}
