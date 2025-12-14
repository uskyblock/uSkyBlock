package us.talabrek.ultimateskyblock.command.completion;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeKey;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.List;

/**
 * TabCompleter for challenge-names
 */
public class AvailableChallengeTabCompleter extends AbstractTabCompleter {

    private final PlayerLogic playerLogic;
    private final ChallengeLogic challengeLogic;

    @Inject
    public AvailableChallengeTabCompleter(
        @NotNull PlayerLogic playerLogic,
        @NotNull ChallengeLogic challengeLogic
    ) {
        this.playerLogic = playerLogic;
        this.challengeLogic = challengeLogic;
    }

    @Override
    protected List<String> getTabList(CommandSender commandSender, String term) {
        List<ChallengeKey> validChallenges = null;
        if (commandSender instanceof Player player) {
            PlayerInfo pi = playerLogic.getPlayerInfo(player);
            if (pi != null) {
                validChallenges = challengeLogic.getAvailableChallenges(pi);
            }
        }
        if (validChallenges == null) {
            validChallenges = challengeLogic.getAllChallengeIds();
        }
        return validChallenges.stream().map(ChallengeKey::id).toList();
    }
}
