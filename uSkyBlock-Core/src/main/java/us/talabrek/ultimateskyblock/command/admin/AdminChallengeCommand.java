package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.view.ChallengeMenu;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;

/**
 * The challenge admin command.
 */
public class AdminChallengeCommand extends CompositeCommand {

    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;
    private final ChallengeMenu challengeMenu;

    @Inject
    public AdminChallengeCommand(@NotNull uSkyBlock plugin, @NotNull ChallengeLogic challengeLogic, @NotNull ChallengeMenu challengeMenu) {
        super("challenge|ch", "usb.mod.challenges", "player", marktr("Manage challenges for a player"));
        this.plugin = plugin;
        this.challengeLogic = challengeLogic;
        this.challengeMenu = challengeMenu;
        add(new ChallengeCommand("complete", null, "completes the challenge for the player") {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, ChallengeCompletion completion) {
                completeChallenge(sender, playerInfo, completion.getId());
            }
        });
        add(new ChallengeCommand("reset", null, marktr("resets the challenge for the player")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo pi, ChallengeCompletion completion) {
                String playerName = pi.getPlayerName();
                if (completion.getTimesCompleted() == 0) {
                    sendErrorTr(sender, "This challenge has never been completed");
                } else {
                    challengeLogic.resetChallengeForAdmin(pi, completion.getId(),
                        () -> sendTr(sender, "Challenge <challenge-id> has been reset for <player>.",
                            unparsed("challenge-id", completion.getId().value(), PRIMARY),
                            unparsed("player", playerName, PRIMARY)),
                        error -> sendErrorTr(sender, "Unable to save challenge progress. Check the server log."));
                }
            }
        });
        add(new AbstractCommand("resetall", null, marktr("resets all challenges for the player")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
                if (playerInfo != null) {
                    challengeLogic.resetAllChallengesForAdmin(playerInfo,
                        () -> sendTr(sender, "<player> has had all challenges reset.", unparsed("player", playerInfo.getPlayerName(), PRIMARY)),
                        error -> sendErrorTr(sender, "Unable to save challenge progress. Check the server log."));
                    return true;
                }
                return false;
            }
        });
        add(new RankCommand("rank", null, marktr("complete all challenges in the rank")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, String rankName, List<ChallengeDefinition> challenges) {
                List<ChallengeId> incomplete = challenges.stream()
                    .map(challenge -> challenge.id())
                    .filter(id -> {
                        ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, id);
                        return completion == null || completion.getTimesCompleted() == 0;
                    })
                    .toList();
                if (incomplete.isEmpty()) {
                    sendErrorTr(sender, "All challenges in rank <rank> are already completed", unparsed("rank", rankName));
                    return;
                }
                challengeLogic.completeChallengesForAdmin(playerInfo, incomplete,
                    () -> sendTr(sender, "Completed <count> challenges in rank <rank> for <player>.",
                        number("count", incomplete.size()),
                        unparsed("rank", rankName, PRIMARY),
                        unparsed("player", playerInfo.getPlayerName(), PRIMARY)),
                    error -> sendErrorTr(sender, "Unable to save challenge progress. Check the server log."));
            }
        });
        add(new AbstractCommand("show", null, "?page", "show challenges for the chosen player") {
            @Override
            public boolean execute(CommandSender commandSender, String alias, Map<String, Object> data, String... args) {
                PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
                if (playerInfo == null) {
                    sendErrorTr(commandSender, "No player named <player> was found!", unparsed("player", String.valueOf(data.get("player"))));
                    return false;
                }
                if (commandSender instanceof Player player) {
                    int page = args.length > 0 && args[0].matches("[0-9]+") ? Integer.parseInt(args[0], 10) : 1;
                    challengeMenu.open(player, playerInfo, page);
                    return true;
                }
                return false;
            }
        });
    }

    private void completeChallenge(CommandSender sender, PlayerInfo playerInfo, ChallengeId challengeId) {
        ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, challengeId);
        Objects.requireNonNull(completion);
        if (completion.getTimesCompleted() > 0) {
            sendErrorTr(sender, "Challenge <challenge-id> has already been completed", unparsed("challenge-id", challengeId.value()));
        } else {
            challengeLogic.completeChallengeForAdmin(playerInfo, challengeId,
                () -> sendTr(sender, "Challenge <challenge-id> has been completed for <player>.",
                    unparsed("challenge-id", challengeId.value(), PRIMARY),
                    unparsed("player", playerInfo.getPlayerName(), PRIMARY)),
                error -> sendErrorTr(sender, "Unable to save challenge progress. Check the server log."));
        }
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        if (args.length > 0) {
            PlayerInfo playerInfo = plugin.getPlayerInfo(args[0]);
            if (playerInfo != null) {
                data.put("playerInfo", playerInfo);
            }
            data.put("playerName", args[0]);
        }
        return super.execute(sender, alias, data, args);
    }

    private abstract class ChallengeCommand extends AbstractCommand {
        public ChallengeCommand(String name, String permission, String description) {
            super(name, permission, "challenge", description);
        }

        protected abstract void doExecute(CommandSender sender, PlayerInfo playerInfo, ChallengeCompletion challenge);

        @Override
        public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
            PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
            if (playerInfo != null && args.length > 0) {
                // Join all remaining args to support multi-word names and display names
                String userInput = String.join(" ", args);
                // Resolve using the unified fuzzy resolver, then operate by canonical id
                var result = challengeLogic.resolveChallenge(userInput);
                switch (result.getStatus()) {
                    case FOUND -> {
                        ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, result.getChallengeId());
                        if (completion != null) {
                            doExecute(sender, playerInfo, completion);
                            return true;
                        }
                    }
                    case AMBIGUOUS -> {
                        String hint = result.getSuggestions().isEmpty() ? "" : " " + String.join(", ", result.getSuggestions());
                        sendErrorTr(sender, "Ambiguous challenge name: <input>. Did you mean:<suggestions>", unparsed("input", result.getNormalizedInput()), unparsed("suggestions", hint));
                        return false;
                    }
                    case NOT_FOUND -> {
                        sendErrorTr(sender, "No challenge named <challenge> was found!", unparsed("challenge", result.getNormalizedInput()));
                        return false;
                    }
                }
            } else {
                sendErrorTr(sender, "No player named <player> was found!", unparsed("player", String.valueOf(data.get("player"))));
            }
            return false;
        }
    }

    private abstract class RankCommand extends AbstractCommand {
        public RankCommand(String name, String permission, String description) {
            super(name, permission, "rank", description);
        }

        protected abstract void doExecute(CommandSender sender, PlayerInfo playerInfo, String rankName, List<ChallengeDefinition> challenge);

        @Override
        public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
            PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
            if (playerInfo != null && args.length > 0) {
                String rankName = String.join(" ", args);
                List<ChallengeDefinition> challenges = plugin.getChallengeLogic().getChallengesForRank(rankName);
                if (challenges == null || challenges.isEmpty()) {
                    sendErrorTr(sender, "No rank named <rank> was found!", unparsed("rank", rankName));
                } else {
                    doExecute(sender, playerInfo, rankName, challenges);
                    return true;
                }
            } else {
                sendErrorTr(sender, "No player named <player> was found!", unparsed("player", String.valueOf(data.get("player"))));
            }
            return false;
        }
    }
}
