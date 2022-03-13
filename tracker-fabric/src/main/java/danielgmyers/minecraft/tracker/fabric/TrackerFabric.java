package danielgmyers.minecraft.tracker.fabric;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.ReporterType;
import danielgmyers.minecraft.tracker.reporters.logging.LoggingReporter;
import danielgmyers.minecraft.tracker.TickStatsTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TrackerFabric implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LogManager.getLogger("tracker-fabric");

    private TickStatsTracker serverTickTracker;
    private final ConcurrentMap<String, TickStatsTracker> worldTickTracker;

    public TrackerFabric() {
        this.worldTickTracker = new ConcurrentHashMap<>();
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        Config config = new Config();
        // TODO -- load from the actual mod config
        config.load(true, false, ReporterType.APPLICATION_LOG);

        LoggingReporter statsReporter = new LoggingReporter(config);
        this.serverTickTracker = new TickStatsTracker("server", config, statsReporter, Clock.systemUTC());

        ServerTickEvents.START_SERVER_TICK.register(s -> { serverTickTracker.startTick(); });
        ServerTickEvents.END_SERVER_TICK.register(s -> { serverTickTracker.endTick(); });
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            String dimension = world.getRegistryKey().getValue().toString();
            worldTickTracker.computeIfAbsent(dimension,
                                             d -> new TickStatsTracker(d, config, statsReporter,Clock.systemUTC()))
                    .startTick();
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            String dimension = world.getRegistryKey().getValue().toString();
            worldTickTracker.computeIfAbsent(dimension,
                                             d -> new TickStatsTracker(d, config, statsReporter, Clock.systemUTC()))
                    .endTick();
        });
    }


}
