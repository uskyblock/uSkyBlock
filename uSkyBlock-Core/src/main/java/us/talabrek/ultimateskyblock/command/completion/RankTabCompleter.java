package us.talabrek.ultimateskyblock.command.completion;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;

import java.util.ArrayList;
import java.util.List;

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
        for (RankDefinition rank : challengeLogic.getCatalog().ranks()) {
            rankNames.add(rank.id().value());
        }
        return rankNames;
    }
}
