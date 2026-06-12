package us.talabrek.ultimateskyblock.challenge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ChallengeUnlockRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedChallengesRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedRankRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.IslandLevelRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.PermissionRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.RankUnlockRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Evaluates catalog unlock requirements against island progress. All context is passed in
 * explicitly, so evaluation is safe on async threads with pre-fetched values.
 */
public final class ChallengeUnlockEvaluator {
    private final ChallengeCatalog catalog;

    public ChallengeUnlockEvaluator(@NotNull ChallengeCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /**
     * @param permissionCheck pass {@code permission -> false} when no player context is available
     *                        (e.g. evaluating for an offline island leader).
     */
    public record UnlockContext(
        @NotNull Map<ChallengeId, ChallengeCompletion> progress,
        @NotNull Predicate<String> permissionCheck,
        double islandLevel
    ) {
    }

    public sealed interface MissingRequirement
        permits MissingRankCompletion, MissingChallenges, MissingPermission, MissingIslandLevel {
    }

    public record MissingRankCompletion(RankId rankId, int remaining) implements MissingRequirement {
    }

    public record MissingChallenges(List<ChallengeId> challengeIds) implements MissingRequirement {
    }

    public record MissingPermission(String permission) implements MissingRequirement {
    }

    public record MissingIslandLevel(double minimumLevel) implements MissingRequirement {
    }

    public boolean isRankUnlocked(@NotNull RankDefinition rank, @NotNull UnlockContext context) {
        return missingRankRequirements(rank, context).isEmpty();
    }

    public boolean isChallengeUnlocked(@NotNull ChallengeDefinition challenge, @NotNull UnlockContext context) {
        return missingChallengeRequirements(challenge, context).isEmpty();
    }

    public @NotNull List<MissingRequirement> missingRankRequirements(@NotNull RankDefinition rank, @NotNull UnlockContext context) {
        List<MissingRequirement> missing = new ArrayList<>();
        for (RankUnlockRequirement requirement : rank.unlockRequirements()) {
            switch (requirement) {
                case CompletedChallengesRequirement required -> checkChallenges(required.challengeIds(), context, missing);
                case CompletedRankRequirement required -> checkRankCompletion(required, context, missing);
                case PermissionRequirement required -> checkPermission(required.permission(), context, missing);
                case IslandLevelRequirement required -> {
                    if (context.islandLevel() < required.minimumLevel()) {
                        missing.add(new MissingIslandLevel(required.minimumLevel()));
                    }
                }
            }
        }
        return missing;
    }

    /**
     * Includes the owning rank's unlock requirements: a challenge is only available once its rank
     * is unlocked.
     */
    public @NotNull List<MissingRequirement> missingChallengeRequirements(@NotNull ChallengeDefinition challenge, @NotNull UnlockContext context) {
        List<MissingRequirement> missing = new ArrayList<>();
        RankId owner = catalog.index().challengeOwners().get(challenge.id());
        if (owner != null) {
            catalog.rank(owner).ifPresent(rank -> missing.addAll(missingRankRequirements(rank, context)));
        }
        for (ChallengeUnlockRequirement requirement : challenge.unlockRequirements()) {
            switch (requirement) {
                case CompletedChallengesRequirement required -> checkChallenges(required.challengeIds(), context, missing);
                case CompletedRankRequirement required -> checkRankCompletion(required, context, missing);
                case PermissionRequirement required -> checkPermission(required.permission(), context, missing);
                case IslandLevelRequirement required -> {
                    if (context.islandLevel() < required.minimumLevel()) {
                        missing.add(new MissingIslandLevel(required.minimumLevel()));
                    }
                }
            }
        }
        return missing;
    }

    public @NotNull Set<RankId> unlockedRanks(@NotNull UnlockContext context) {
        Set<RankId> unlocked = new LinkedHashSet<>();
        for (RankDefinition rank : catalog.ranks()) {
            if (isRankUnlocked(rank, context)) {
                unlocked.add(rank.id());
            }
        }
        return unlocked;
    }

    public int completedChallenges(@NotNull RankDefinition rank, @NotNull Map<ChallengeId, ChallengeCompletion> progress) {
        int completed = 0;
        for (ChallengeDefinition challenge : rank.challenges()) {
            ChallengeCompletion completion = progress.get(challenge.id());
            if (completion != null && completion.getTimesCompleted() > 0) {
                completed++;
            }
        }
        return completed;
    }

    public @NotNull List<Component> describe(@NotNull List<MissingRequirement> missing) {
        List<Component> lines = new ArrayList<>();
        for (MissingRequirement requirement : missing) {
            lines.add(switch (requirement) {
                case MissingRankCompletion missingRank -> tr("Complete <remaining> more <rank> challenges", MUTED,
                    number("remaining", missingRank.remaining()),
                    component("rank", rankDisplayName(missingRank.rankId()), PRIMARY));
                case MissingChallenges missingChallenges -> tr("Complete <requirements>", MUTED,
                    component("requirements", challengeList(missingChallenges.challengeIds()), PRIMARY));
                case MissingPermission missingPermission -> tr("Requires permission <permission>", MUTED,
                    unparsed("permission", missingPermission.permission(), PRIMARY));
                case MissingIslandLevel missingLevel -> tr("Requires island level <level>", MUTED,
                    number("level", missingLevel.minimumLevel(), PRIMARY));
            });
        }
        return lines;
    }

    private void checkChallenges(@NotNull List<ChallengeId> challengeIds, @NotNull UnlockContext context, @NotNull List<MissingRequirement> missing) {
        List<ChallengeId> notCompleted = new ArrayList<>();
        for (ChallengeId challengeId : challengeIds) {
            ChallengeCompletion completion = context.progress().get(challengeId);
            if (completion == null || completion.getTimesCompleted() == 0) {
                notCompleted.add(challengeId);
            }
        }
        if (!notCompleted.isEmpty()) {
            missing.add(new MissingChallenges(notCompleted));
        }
    }

    private void checkRankCompletion(@NotNull CompletedRankRequirement required, @NotNull UnlockContext context, @NotNull List<MissingRequirement> missing) {
        // Unknown rank references are reported by the catalog validator at load time.
        catalog.rank(required.rankId()).ifPresent(rank -> {
            int completed = completedChallenges(rank, context.progress());
            if (completed < required.minimumCompletedChallenges()) {
                missing.add(new MissingRankCompletion(rank.id(), required.minimumCompletedChallenges() - completed));
            }
        });
    }

    private void checkPermission(@NotNull String permission, @NotNull UnlockContext context, @NotNull List<MissingRequirement> missing) {
        if (!context.permissionCheck().test(permission)) {
            missing.add(new MissingPermission(permission));
        }
    }

    private @NotNull Component rankDisplayName(@NotNull RankId rankId) {
        return catalog.rank(rankId)
            .map(ChallengeText::displayName)
            .orElseGet(() -> Component.text(rankId.value()));
    }

    private @NotNull Component challengeList(@NotNull List<ChallengeId> challengeIds) {
        List<Component> names = new ArrayList<>();
        for (ChallengeId challengeId : challengeIds) {
            names.add(catalog.challenge(challengeId)
                .map(ChallengeText::displayName)
                .orElseGet(() -> Component.text(challengeId.value())));
        }
        return Component.join(JoinConfiguration.commas(true), names);
    }
}
