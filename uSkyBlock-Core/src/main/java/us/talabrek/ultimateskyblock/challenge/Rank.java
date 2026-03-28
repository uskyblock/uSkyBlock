package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;

public class Rank {
    private final Rank previousRank;
    private final ChallengeDefaults defaults;
    private final List<Challenge> challenges;
    private final String rankKey;
    private final String displayName;
    private final ItemStackSpec displayItem;
    private final Duration resetDuration;
    private final @Nullable Integer minimumCompletedChallengesInPreviousRank;
    private final List<String> requiredChallenges;
    private final List<String> requiredPermissions;

    public Rank(ConfigurationSection section, Rank previousRank, ChallengeDefaults defaults, GameObjectFactory gameObjects, ChallengeFactory challengeFactory) {
        this(
            section.getName(),
            FormatUtil.normalize(section.getString("name", section.getName())),
            gameObjects.itemStack(section.getString("displayItem", "DIRT")),
            Duration.ofHours(section.getLong("resetInHours", defaults.resetDuration.toHours())),
            previousRank,
            defaults,
            resolveMinimumCompletedChallenges(section, previousRank, defaults),
            resolveRequiredChallenges(section),
            List.of()
        );
        ConfigurationSection challengeSection = section.getConfigurationSection("challenges");
        for (String challengeName : challengeSection.getKeys(false)) {
            Challenge challenge = challengeFactory.createChallenge(this, challengeSection.getConfigurationSection(challengeName), defaults);
            if (challenge != null) {
                challenges.add(challenge);
            }
        }
    }

    public Rank(
        @NotNull String rankKey,
        @NotNull String displayName,
        @NotNull ItemStackSpec displayItem,
        @NotNull Duration resetDuration,
        @Nullable Rank previousRank,
        @NotNull ChallengeDefaults defaults,
        @Nullable Integer minimumCompletedChallengesInPreviousRank,
        @NotNull List<String> requiredChallenges,
        @NotNull List<String> requiredPermissions
    ) {
        this.challenges = new ArrayList<>();
        this.previousRank = previousRank;
        this.defaults = defaults;
        this.rankKey = rankKey;
        this.displayName = displayName;
        this.displayItem = displayItem;
        this.resetDuration = resetDuration;
        this.minimumCompletedChallengesInPreviousRank = minimumCompletedChallengesInPreviousRank;
        this.requiredChallenges = List.copyOf(requiredChallenges);
        this.requiredPermissions = List.copyOf(requiredPermissions);
    }

    public boolean isAvailable(PlayerInfo playerInfo) {
        return getMissingRequirements(playerInfo).isEmpty();
    }

    public List<Challenge> getChallenges() {
        return challenges;
    }

    public ItemStack getDisplayItem() {
        return ItemStackUtil.asDisplayItem(displayItem.create(getName(), null));
    }

    public String getName() {
        return displayName;
    }

    public String getRankKey() {
        return rankKey;
    }

    public Rank getPreviousRank() {
        return previousRank;
    }

    @Override
    public String toString() {
        return getName();
    }

    public List<String> getMissingRequirements(PlayerInfo playerInfo) {
        List<String> missing = new ArrayList<>();
        if (previousRank != null && minimumCompletedChallengesInPreviousRank != null) {
            int completed = previousRank.getChallenges().size() - previousRank.getLeeway(playerInfo);
            if (completed < minimumCompletedChallengesInPreviousRank) {
                missing.add(trLegacy("Complete <remaining> more <rank> challenges",
                    MUTED,
                    unparsed("remaining", String.valueOf(minimumCompletedChallengesInPreviousRank - completed)),
                    legacyArg("rank", previousRank.getName())));
            }
        }
        String missingChallenges = ChallengeFormat.getMissingRequirement(playerInfo, requiredChallenges, uSkyBlock.getInstance().getChallengeLogic());
        if (missingChallenges != null) {
            missing.add(trLegacy("Complete <requirements>", MUTED, legacyArg("requirements", missingChallenges)));
        }
        for (String permission : requiredPermissions) {
            if (!playerInfo.getPlayer().hasPermission(permission)) {
                missing.add(trLegacy("Requires permission <permission>", MUTED, legacyArg("permission", permission)));
            }
        }
        if (!missing.isEmpty()) {
            missing.add(0, trLegacy("To unlock this rank, complete:", MUTED));
        }
        return missing;
    }

    private int getLeeway(PlayerInfo playerInfo) {
        int leeway = challenges.size();
        for (Challenge challenge : challenges) {
            ChallengeCompletion challengeCompletion = uSkyBlock.getInstance().getChallengeLogic().getChallengeCompletion(playerInfo, challenge.getId());
            if (challengeCompletion.getTimesCompleted() > 0) {
                leeway--;
            }
        }
        return leeway;
    }

    public Duration getResetDuration() {
        return resetDuration;
    }

    private static @Nullable Integer resolveMinimumCompletedChallenges(
        @NotNull ConfigurationSection section,
        @Nullable Rank previousRank,
        @NotNull ChallengeDefaults defaults
    ) {
        if (previousRank == null) {
            return null;
        }
        ConfigurationSection requires = section.getConfigurationSection("requires");
        if (requires != null) {
            int rankLeeway = requires.getInt("rankLeeway", defaults.rankLeeway);
            return Math.max(0, previousRank.getChallenges().size() - rankLeeway);
        }
        if (defaults.requiresPreviousRank) {
            return Math.max(0, previousRank.getChallenges().size() - defaults.rankLeeway);
        }
        return null;
    }

    private static @NotNull List<String> resolveRequiredChallenges(@NotNull ConfigurationSection section) {
        ConfigurationSection requires = section.getConfigurationSection("requires");
        return requires != null ? List.copyOf(requires.getStringList("challenges")) : List.of();
    }
}
