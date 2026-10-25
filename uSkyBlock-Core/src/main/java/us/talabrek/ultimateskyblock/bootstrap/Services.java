package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.MetricsManager;
import us.talabrek.ultimateskyblock.api.event.EventLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.command.admin.DebugCommand;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.level.AutoIslandLevelRefresh;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderIntegrations;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.upgrade.IslandRegionRepair;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

import java.time.Duration;

@Singleton
public class Services {

    private final AnimationHandler animationHandler;
    private final ChallengeLogic challengeLogic;
    private final EventLogic eventLogic;
    private final PlayerLogic playerLogic;
    private final IslandLogic islandLogic;
    private final PlayerDB playerDB;
    private final MetricsManager metricsManager;
    private final HookManager hookManager;
    private final AutoIslandLevelRefresh autoIslandLevelRefresh;
    private final PlaceholderIntegrations placeholderIntegrations;
    private final SchematicHandler schematicHandler;
    private final IslandRegionRepair islandRegionRepair;
    private final Scheduler scheduler;

    @Inject
    public Services(
        @NotNull AnimationHandler animationHandler,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull EventLogic eventLogic,
        @NotNull PlayerLogic playerLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull PlayerDB playerDB,
        @NotNull MetricsManager metricsManager,
        @NotNull HookManager hookManager,
        @NotNull AutoIslandLevelRefresh autoIslandLevelRefresh,
        @NotNull PlaceholderIntegrations placeholderIntegrations,
        @NotNull SchematicHandler schematicHandler,
        @NotNull IslandRegionRepair islandRegionRepair,
        @NotNull Scheduler scheduler
        ) {
        this.animationHandler = animationHandler;
        this.challengeLogic = challengeLogic;
        this.eventLogic = eventLogic;
        this.playerLogic = playerLogic;
        this.islandLogic = islandLogic;
        this.playerDB = playerDB;
        this.metricsManager = metricsManager;
        this.hookManager = hookManager;
        this.autoIslandLevelRefresh = autoIslandLevelRefresh;
        this.placeholderIntegrations = placeholderIntegrations;
        this.schematicHandler = schematicHandler;
        this.islandRegionRepair = islandRegionRepair;
        this.scheduler = scheduler;
    }

    public void startup(uSkyBlock plugin) {
        schematicHandler.initialize(plugin);
        metricsManager.setup();
        autoIslandLevelRefresh.startup();
        // At every start of the services, a /usb reload's too: its shutdown cancels the plugin's tasks, the update
        // checks among them.
        scheduler.async(plugin::checkForUpdatesIfEnabled, Duration.ZERO, Duration.ofHours(4));
    }

    public void delayedEnable(uSkyBlock plugin) {
        hookManager.setupHooks();
        placeholderIntegrations.enableIntegrations(plugin);

        // TODO: make these non-static objects
        WorldGuardHandler.setupGlobal(plugin.getWorldManager().getWorld());
        if (plugin.getWorldManager().getNetherWorld() != null) {
            WorldGuardHandler.setupGlobal(plugin.getWorldManager().getNetherWorld());
        }
        islandRegionRepair.start();
    }

    public void shutdown(uSkyBlock plugin) {
        autoIslandLevelRefresh.shutdown();
        animationHandler.stop();
        challengeLogic.shutdown();
        eventLogic.shutdown();
        playerLogic.shutdown();
        islandLogic.shutdown();
        playerDB.shutdown();
        islandRegionRepair.shutdown();
        DebugCommand.disableLogging(null);
    }
}
