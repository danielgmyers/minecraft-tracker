package danielgmyers.minecraft.tracker.forge;

import danielgmyers.minecraft.tracker.TickStatsMinuteLoggingReporter;
import danielgmyers.minecraft.tracker.TickStatsTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
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

    private final TickStatsMinuteLoggingReporter statsReporter;
    private final TickStatsTracker serverTickTracker;
    private final ConcurrentMap<String, TickStatsTracker> worldTickTracker;

    public TrackerForge() {
        this.statsReporter = new TickStatsMinuteLoggingReporter();
        this.serverTickTracker = new TickStatsTracker("server", statsReporter, Clock.systemUTC());
        this.worldTickTracker = new ConcurrentHashMap<>();

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
                                            (d) -> new TickStatsTracker(d, statsReporter, Clock.systemUTC()));
        if (event.phase == TickEvent.Phase.START) {
            trackerForDimension.startTick();
        } else {
            trackerForDimension.endTick();
        }
    }
}
