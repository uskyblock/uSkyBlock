package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.MetricsManager;
import us.talabrek.ultimateskyblock.api.event.EventLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.command.admin.DebugCommand;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderModule;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.island.level.AutoIslandLevelRefresh;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.uuid.PlayerDB;

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
    private final PlaceholderModule placeholderModule;

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
        @NotNull PlaceholderModule placeholderModule
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
        this.placeholderModule = placeholderModule;
    }

    public void startup(uSkyBlock plugin) {
        metricsManager.setup();
        autoIslandLevelRefresh.startup();
        placeholderModule.startup(plugin);
    }

    public void delayedEnable(uSkyBlock plugin) {
        hookManager.setupHooks();

        // TODO: make these non-static objects
        AsyncWorldEditHandler.onEnable(plugin);
        WorldGuardHandler.setupGlobal(plugin.getWorldManager().getWorld());
        if (plugin.getWorldManager().getNetherWorld() != null) {
            WorldGuardHandler.setupGlobal(plugin.getWorldManager().getNetherWorld());
        }
    }

    public void shutdown(uSkyBlock plugin) {
        autoIslandLevelRefresh.shutdown();
        animationHandler.stop();
        challengeLogic.shutdown();
        eventLogic.shutdown();
        playerLogic.shutdown();
        islandLogic.shutdown();
        playerDB.shutdown();
        AsyncWorldEditHandler.onDisable(plugin);
        DebugCommand.disableLogging(null);
    }
}
