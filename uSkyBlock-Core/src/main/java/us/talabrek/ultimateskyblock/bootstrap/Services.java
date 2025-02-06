package us.talabrek.ultimateskyblock.bootstrap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.event.EventLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.command.admin.DebugCommand;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.island.IslandLogic;
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

    @Inject
    public Services(
        @NotNull AnimationHandler animationHandler,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull EventLogic eventLogic,
        @NotNull PlayerLogic playerLogic,
        @NotNull IslandLogic islandLogic,
        @NotNull PlayerDB playerDB
    ) {
        this.animationHandler = animationHandler;
        this.challengeLogic = challengeLogic;
        this.eventLogic = eventLogic;
        this.playerLogic = playerLogic;
        this.islandLogic = islandLogic;
        this.playerDB = playerDB;
    }

    public void startup() {

    }

    public void delayedEnable() {

    }

    public void shutdown(uSkyBlock plugin) {
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
