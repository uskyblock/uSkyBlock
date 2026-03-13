package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
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
    private static final GameObjectFactory GAME_OBJECTS = new GameObjectFactory();
    private final Rank previousRank;
    private final ChallengeDefaults defaults;
    private final List<Challenge> challenges;
    private final ConfigurationSection config;
    private final ItemStackSpec displayItem;

    public Rank(ConfigurationSection section, Rank previousRank, ChallengeDefaults defaults) {
        this.challenges = new ArrayList<>();
        this.previousRank = previousRank;
        this.defaults = defaults;
        this.config = section;
        this.displayItem = GAME_OBJECTS.itemStack(section.getString("displayItem", "DIRT"));
        ConfigurationSection challengeSection = section.getConfigurationSection("challenges");
        for (String challengeName : challengeSection.getKeys(false)) {
            Challenge challenge = ChallengeFactory.createChallenge(this, challengeSection.getConfigurationSection(challengeName), defaults);
            if (challenge != null) {
                challenges.add(challenge);
            }
        }
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
        return FormatUtil.normalize(config.getString("name", config.getName()));
    }

    public String getRankKey() {
        return config.getName();
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
        ConfigurationSection requires = config.getConfigurationSection("requires");
        if (requires != null) {
            if (previousRank != null) {
                int leeway = previousRank.getLeeway(playerInfo);
                int rankLeeway = requires.getInt("rankLeeway", defaults.rankLeeway);
                if (leeway > rankLeeway) {
                    missing.add(trLegacy("Complete <remaining> more <rank> challenges",
                        MUTED,
                        number("remaining", leeway - rankLeeway),
                        legacyArg("rank", previousRank.getName())));
                }
            }
            String missingChallenges = ChallengeFormat.getMissingRequirement(playerInfo, requires.getStringList("challenges"), uSkyBlock.getInstance().getChallengeLogic());
            if (missingChallenges != null) {
                missing.add(trLegacy("Complete <requirements>", MUTED, legacyArg("requirements", missingChallenges)));
            }
            if (!missing.isEmpty()) {
                missing.add(0, trLegacy("To unlock this rank, complete:", MUTED));
            }
        } else if (defaults.requiresPreviousRank) {
            if (previousRank != null) {
                int leeway = previousRank.getLeeway(playerInfo);
                if (leeway > defaults.rankLeeway) {
                    missing.add(trLegacy("Complete <remaining> more <rank> challenges",
                        MUTED,
                        unparsed("remaining", String.valueOf(leeway - defaults.rankLeeway)),
                        legacyArg("rank", previousRank.getName())));
                }
            }
            if (!missing.isEmpty()) {
                missing.add(0, trLegacy("To unlock this rank, complete:", MUTED));
            }
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
        return Duration.ofHours(config.getLong("resetInHours", defaults.resetDuration.toHours()));
    }
}
