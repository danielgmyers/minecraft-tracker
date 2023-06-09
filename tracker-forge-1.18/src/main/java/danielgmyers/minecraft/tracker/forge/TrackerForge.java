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
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;
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

        // Make sure the server doesn't show this mod as required on the client side
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                                                       () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY,
                                                                                             (a, b) -> true));
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

        // TODO -- make this configurable
        // Vault Hunters generates unique dimensions for each vault run, making these metrics hard to use.
        // We'll coalesce any dimensions that start with "the_vault:vault_", "the_vault:arena_", or "the_vault:the_other_side_"
        // to "the_vault:vault", "the_vault:arena", or "the_vault:the_other_side", respectively.
        dimension = trimNameIfTemporaryVaultDimension(dimension);

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

    private String trimNameIfTemporaryVaultDimension(String dimension) {
        if (dimension.startsWith("the_vault:vault_")) {
            return "the_vault:vault";
        } else if (dimension.startsWith("the_vault:arena_")) {
            return "the_vault:arena";
        } else if (dimension.startsWith("the_vault:the_other_side_")) {
            return "the_vault:the_other_side";
        }
        return dimension;
    }
}
