package us.talabrek.ultimateskyblock.command.challenge;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.Challenge;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.MUTED;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendLegacy;
import static us.talabrek.ultimateskyblock.util.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Shows information about a challenge
 */
public class ChallengeInfoCommand extends AbstractCommand {

    private final ChallengeLogic challengeLogic;
    private final PlayerLogic playerLogic;

    @Inject
    public ChallengeInfoCommand(
        @NotNull ChallengeLogic challengeLogic,
        @NotNull PlayerLogic playerLogic
    ) {
        super("info|i", null, "challenge", marktr("show information about the challenge"));
        this.challengeLogic = challengeLogic;
        this.playerLogic = playerLogic;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (!(sender instanceof Player player)) {
            sendPlayerOnly(sender);
            return false;
        }
        String challengeQuery = String.join(" ", args);
        var result = challengeLogic.resolveChallenge(challengeQuery);
        PlayerInfo playerInfo = playerLogic.getPlayerInfo(player);
        if (result.getStatus() == ChallengeLogic.ChallengeLookupResult.Status.FOUND && result.getChallenge().getRank().isAvailable(playerInfo)) {
            Challenge challenge = result.getChallenge();
            sendTr(player, "Challenge name: <primary><challenge></primary>", legacyArg("challenge", challenge.getDisplayName()));
            if (challengeLogic.getRanks().size() > 1) {
                sendTr(player, "Rank: <primary><rank></primary>", unparsed("rank", challenge.getRank().getName()));
            }
            ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, challenge.getId());
            if (completion.getTimesCompleted() > 0 && !challenge.isRepeatable()) {
                sendErrorTr(player, "This challenge is not repeatable!");
            }
            ItemStack item = challenge.getDisplayItem(completion, challengeLogic.defaults.enableEconomyPlugin);
            for (String lore : item.getItemMeta().getLore()) {
                if (lore != null && !lore.trim().isEmpty()) {
                    sendLegacy(player, lore);
                }
            }
            if (challenge.getType() == Challenge.Type.PLAYER) {
                if (challenge.isTakeItems()) {
                    sendErrorTr(player, "You will lose all required items when you complete this challenge!");
                }
            } else if (challenge.getType() == Challenge.Type.ISLAND) {
                sendErrorTr(player, "All required items must be placed on your island, within <radius> blocks of you.",
                    unparsed("radius", String.valueOf(challenge.getRadius())));
            }
            sendTr(player, "To complete this challenge, use <cmd>/c complete [challenge]</cmd>.", MUTED);
        } else {
            switch (result.getStatus()) {
                case AMBIGUOUS -> {
                    String hint = result.getSuggestions().isEmpty() ? "" : " " + String.join(", ", result.getSuggestions());
                    sendErrorTr(player, "Ambiguous challenge name: <input>. Did you mean:<suggestions>",
                        unparsed("input", result.getNormalizedInput()),
                        unparsed("suggestions", hint));
                }
                case NOT_FOUND -> sendErrorTr(player, "Invalid challenge name! Use /c help for more information");
                case FOUND -> // FOUND but rank not available
                    sendErrorTr(player, "The <challenge> challenge is not available yet!",
                        legacyArg("challenge", result.getChallenge().getDisplayName()));
            }
        }
        return true;
    }
}
