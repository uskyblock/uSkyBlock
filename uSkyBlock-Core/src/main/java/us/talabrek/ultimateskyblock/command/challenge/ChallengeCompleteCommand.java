package us.talabrek.ultimateskyblock.command.challenge;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Complete Challenge Command
 */
public class ChallengeCompleteCommand extends AbstractCommand {
    private final ChallengeLogic challengeLogic;

    @Inject
    public ChallengeCompleteCommand(@NotNull ChallengeLogic challengeLogic) {
        super("complete|c", "usb.island.challenges", "challenge", marktr("try to complete a challenge"));
        this.challengeLogic = challengeLogic;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(tr("\u00a7cCommand only available for players."));
            return false;
        }
        if (args == null || args.length == 0) {
            return false;
        }
        String challengeName = String.join(" ", args);
        challengeLogic.completeChallenge((Player) sender, challengeName);
        return true;
    }
}
