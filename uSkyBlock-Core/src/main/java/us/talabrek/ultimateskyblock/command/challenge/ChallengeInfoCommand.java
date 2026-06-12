package us.talabrek.ultimateskyblock.command.challenge;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.challenge.ChallengeText;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.view.ChallengeLore;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.MUTED;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Shows information about a challenge
 */
public class ChallengeInfoCommand extends AbstractCommand {

    private final ChallengeLogic challengeLogic;
    private final PlayerLogic playerLogic;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public ChallengeInfoCommand(
        @NotNull ChallengeLogic challengeLogic,
        @NotNull PlayerLogic playerLogic,
        @NotNull RuntimeConfigs runtimeConfigs
    ) {
        super("info|i", null, "challenge", marktr("show information about the challenge"));
        this.challengeLogic = challengeLogic;
        this.playerLogic = playerLogic;
        this.runtimeConfigs = runtimeConfigs;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (!(sender instanceof Player player)) {
            sendPlayerOnly(sender);
            return false;
        }
        if (args == null || args.length == 0) {
            return false;
        }
        String challengeQuery = String.join(" ", args);
        var result = challengeLogic.resolveChallenge(challengeQuery);
        PlayerInfo playerInfo = playerLogic.getPlayerInfo(player);
        switch (result.getStatus()) {
            case FOUND -> {
                ChallengeDefinition challenge = result.getChallenge();
                boolean unlocked = challengeLogic.getUnlockEvaluator()
                    .isChallengeUnlocked(challenge, challengeLogic.unlockContextFor(playerInfo));
                if (!unlocked) {
                    sendErrorTr(player, "The <challenge> challenge is not available yet!",
                        component("challenge", ChallengeText.displayName(challenge)));
                    return true;
                }
                ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, result.getChallengeKey());
                sendTr(player, "Challenge name: <challenge>",
                    component("challenge", ChallengeText.displayName(challenge), PRIMARY));
                boolean economyEnabled = runtimeConfigs.current().challenges().enableEconomyRewards();
                for (Component line : ChallengeLore.describe(challenge, completion, economyEnabled)) {
                    send(player, line);
                }
                if (challenge.properties().consumeItemsOnCompletion() && hasItemRequirements(challenge)) {
                    sendErrorTr(player, "You will lose all required items when you complete this challenge!");
                }
                sendTr(player, "To complete this challenge, use <cmd>/c complete [challenge]</cmd>.", MUTED);
            }
            case AMBIGUOUS -> {
                String hint = result.getSuggestions().isEmpty() ? "" : " " + String.join(", ", result.getSuggestions());
                sendErrorTr(player, "Ambiguous challenge name: <input>. Did you mean:<suggestions>",
                    unparsed("input", result.getNormalizedInput()),
                    unparsed("suggestions", hint));
            }
            case NOT_FOUND -> sendErrorTr(player, "Invalid challenge name! Use /c help for more information");
        }
        return true;
    }

    private static boolean hasItemRequirements(ChallengeDefinition challenge) {
        return challenge.completionRequirements().stream()
            .anyMatch(InventoryItemsRequirement.class::isInstance);
    }
}
