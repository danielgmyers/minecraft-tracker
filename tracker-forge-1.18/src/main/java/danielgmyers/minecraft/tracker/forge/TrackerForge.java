package danielgmyers.minecraft.tracker.forge;

import danielgmyers.minecraft.tracker.PlayerCountTracker;
import danielgmyers.minecraft.tracker.TickStatsTracker;
import danielgmyers.minecraft.tracker.config.Config;
import danielgmyers.minecraft.tracker.config.PropertiesConfig;
import danielgmyers.minecraft.tracker.reporters.StatsReporterFactory;
import danielgmyers.minecraft.tracker.reporters.StatsReporter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("tracker_forge")
public class TrackerForge {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger("tracker_forge");

    private Config config;
    private StatsReporter statsReporter;
    private TickStatsTracker serverTickTracker;
    private final ConcurrentMap<String, TickStatsTracker> worldTickTracker;
    private PlayerCountTracker playerCountTracker;
    private final ConcurrentMap<String, PlayerCountTracker> worldPlayerCountTracker;

    public TrackerForge() {
        this.worldTickTracker = new ConcurrentHashMap<>();
        this.worldPlayerCountTracker = new ConcurrentHashMap<>();

        Path configPath = FMLPaths.CONFIGDIR.get().resolve("tracker.properties");
        config = PropertiesConfig.create(configPath);

        if (!config.isEnabled()) {
            LOGGER.warn("Stats tracking is disabled!");
            return;
        }

        statsReporter = StatsReporterFactory.create(config, Clock.systemUTC());
        this.serverTickTracker = new TickStatsTracker("server", config, statsReporter, Clock.systemUTC());
        this.playerCountTracker = new PlayerCountTracker("server", config, statsReporter, Clock.systemUTC());

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (!config.isEnabled()) {
            // if we're disabled, just unregister the tracker entirely
            MinecraftForge.EVENT_BUS.unregister(this);
        }

        if (event.phase == TickEvent.Phase.START) {
            serverTickTracker.startTick();
        } else {
            serverTickTracker.endTick();
            playerCountTracker.update(ServerLifecycleHooks.getCurrentServer().getPlayerCount());
        }
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        String dimension = event.world.dimension().location().toString();
        TickStatsTracker trackerForDimension = worldTickTracker.computeIfAbsent(dimension,
                    (d) -> new TickStatsTracker(d, config, statsReporter, Clock.systemUTC()));
        if (event.phase == TickEvent.Phase.START) {
            trackerForDimension.startTick();
        } else {
            trackerForDimension.endTick();

            PlayerCountTracker playerCountTracker = worldPlayerCountTracker.computeIfAbsent(dimension,
                    (d) -> new PlayerCountTracker(d, config, statsReporter, Clock.systemUTC()));
            playerCountTracker.update(event.world.players().size());
        }
    }
}
