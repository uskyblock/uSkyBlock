package us.talabrek.ultimateskyblock.command.completion;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.challenge.Rank;

import java.util.ArrayList;
import java.util.List;

import static dk.lockfuglsang.minecraft.util.FormatUtil.stripFormatting;

/**
 * Rank name tab completer
 */
public class RankTabCompleter extends AbstractTabCompleter {
    private final ChallengeLogic challengeLogic;

    @Inject
    public RankTabCompleter(@NotNull ChallengeLogic challengeLogic) {
        this.challengeLogic = challengeLogic;
    }

    @Override
    protected List<String> getTabList(CommandSender commandSender, String term) {
        List<String> rankNames = new ArrayList<>();
        for (Rank rank : challengeLogic.getRanks()) {
            rankNames.add(stripFormatting(rank.getRankKey()));
        }
        return rankNames;
    }
}
