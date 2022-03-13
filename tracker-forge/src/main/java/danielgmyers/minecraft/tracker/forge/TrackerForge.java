package danielgmyers.minecraft.tracker.forge;

import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.ReporterType;
import danielgmyers.minecraft.tracker.reporters.logging.LoggingReporter;
import danielgmyers.minecraft.tracker.TickStatsTracker;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("tracker_forge")
public class TrackerForge {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger("tracker_forge");

    private Config config;
    private TickStatsTracker serverTickTracker;
    private final ConcurrentMap<String, TickStatsTracker> worldTickTracker;

    public TrackerForge() {
        this.worldTickTracker = new ConcurrentHashMap<>();

        this.config = new Config();
        // TODO -- load from the actual mod config
        config.load(true, false, ReporterType.APPLICATION_LOG);
        LoggingReporter statsReporter = new LoggingReporter(config);
        this.serverTickTracker = new TickStatsTracker("server", config, statsReporter, Clock.systemUTC());

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            serverTickTracker.startTick();
        } else {
            serverTickTracker.endTick();
        }
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        String dimension = event.world.dimension().location().toString();
        TickStatsTracker trackerForDimension = worldTickTracker.computeIfAbsent(dimension,
                                            (d) -> new TickStatsTracker(d, config, new LoggingReporter(config),
                                                                        Clock.systemUTC()));
        if (event.phase == TickEvent.Phase.START) {
            trackerForDimension.startTick();
        } else {
            trackerForDimension.endTick();
        }
    }
}
