package us.talabrek.ultimateskyblock.challenge.view;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeUnlockEvaluator;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gui.GuiManager;
import us.talabrek.ultimateskyblock.player.PlayerInfo;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;

/**
 * Opens the challenge menu: loads the target's island progress asynchronously, projects the
 * catalog through the unlock evaluator, and renders a {@link ChallengeGui} page.
 */
@Singleton
public class ChallengeMenu {
    private final ChallengeLogic challengeLogic;
    private final RuntimeConfigs runtimeConfigs;
    private final GuiManager guiManager;
    private final ChallengeMenuViewAssembler assembler = new ChallengeMenuViewAssembler();

    @Inject
    public ChallengeMenu(
        @NotNull ChallengeLogic challengeLogic,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull GuiManager guiManager
    ) {
        this.challengeLogic = Objects.requireNonNull(challengeLogic, "challengeLogic");
        this.runtimeConfigs = Objects.requireNonNull(runtimeConfigs, "runtimeConfigs");
        this.guiManager = Objects.requireNonNull(guiManager, "guiManager");
    }

    /**
     * @param target whose island progress to show; admins may view other players.
     */
    public void open(@NotNull Player viewer, @NotNull PlayerInfo target, int page) {
        challengeLogic.whenChallengesLoaded(target,
            () -> show(viewer, target, page),
            error -> sendErrorTr(viewer, "Unable to load challenge progress right now. Please try again."));
    }

    private void show(@NotNull Player viewer, @NotNull PlayerInfo target, int page) {
        ChallengeUnlockEvaluator.UnlockContext context = challengeLogic.unlockContextFor(target);
        ChallengePresentationState state = presentationState(context);
        Map<ChallengeId, ChallengeCompletion> progress = context.progress();
        boolean economyEnabled = runtimeConfigs.current().challenges().enableEconomyRewards();
        int totalPages = Math.max(1, (int) Math.ceil(
            challengeLogic.getCatalog().ranks().size() / (double) ChallengeMenuViewAssembler.RANKS_PER_PAGE));
        int clampedPage = Math.clamp(page, 1, totalPages);
        ChallengePageView pageView = assembler.assemblePage(
            challengeLogic.getCatalog(), state, progress, economyEnabled, clampedPage);
        ChallengeGui gui = new ChallengeGui(pageView,
            newPage -> show(viewer, target, newPage),
            (player, slotView) -> {
                player.closeInventory();
                player.performCommand("challenges complete " + slotView.challenge().id().value());
            });
        guiManager.openGui(gui, viewer);
    }

    private @NotNull ChallengePresentationState presentationState(@NotNull ChallengeUnlockEvaluator.UnlockContext context) {
        ChallengeUnlockEvaluator evaluator = challengeLogic.getUnlockEvaluator();
        Set<RankId> unlockedRanks = evaluator.unlockedRanks(context);
        Set<ChallengeId> unlockedChallenges = new LinkedHashSet<>();
        for (RankDefinition rank : challengeLogic.getCatalog().ranks()) {
            for (ChallengeDefinition challenge : rank.challenges()) {
                if (evaluator.isChallengeUnlocked(challenge, context)) {
                    unlockedChallenges.add(challenge.id());
                }
            }
        }
        return new ChallengePresentationSnapshot(unlockedRanks, unlockedChallenges);
    }
}
