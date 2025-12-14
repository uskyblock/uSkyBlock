package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.Challenge;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeKey;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;

/**
 * The challenge admin command.
 */
public class AdminChallengeCommand extends CompositeCommand {

    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;

    @Inject
    public AdminChallengeCommand(
        @NotNull uSkyBlock plugin,
        @NotNull ChallengeLogic challengeLogic
    ) {
        super("challenge|ch", "usb.mod.challenges", "player", marktr("Manage challenges for a player"));
        this.plugin = plugin;
        this.challengeLogic = challengeLogic;
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
                    sender.sendMessage(I18nUtil.tr("\u00a74Challenge has never been completed"));
                } else {
                    challengeLogic.resetChallenge(pi, completion.getId());
                    pi.save(); // TODO: is it really PlayerInfo that should be saved?
                    sender.sendMessage(I18nUtil.tr("\u00a7echallenge: {0} has been reset for {1}", completion.getId().id(), playerName));
                }
            }
        });
        add(new AbstractCommand("resetall", null, marktr("resets all challenges for the player")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
                if (playerInfo != null) {
                    challengeLogic.resetAllChallenges(playerInfo);
                    playerInfo.save(); // TODO: is it really PlayerInfo that should be saved?
                    sender.sendMessage(I18nUtil.tr("\u00a7e{0} has had all challenges reset.", playerInfo.getPlayerName()));
                    return true;
                }
                return false;
            }
        });
        add(new RankCommand("rank", null, marktr("complete all challenges in the rank")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, String rankName, List<Challenge> challenges) {
                for (Challenge challenge : challenges) {
                    completeChallenge(sender, playerInfo, challenge.getId());
                }
            }
        });
        add(new AbstractCommand("show", null, "?page", "show challenges for the chosen player") {
            @Override
            public boolean execute(CommandSender commandSender, String alias, Map<String, Object> data, String... args) {
                PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
                if (playerInfo == null) {
                    commandSender.sendMessage(I18nUtil.tr("\u00a74No player named {0} was found!", data.get("player")));
                    return false;
                }
                if (commandSender instanceof Player player) {
                    int page = args.length > 0 && args[0].matches("[0-9]+") ? Integer.parseInt(args[0], 10) : 1;
                    String playerName = (String) data.get("playerName");
                    player.openInventory(plugin.getMenu().displayChallengeGUI(player, page, playerName));
                    return true;
                }
                return false;
            }
        });
    }

    private void completeChallenge(CommandSender sender, PlayerInfo playerInfo, ChallengeKey challengeId) {
        Challenge challenge = challengeLogic.getChallengeById(challengeId).orElseThrow();
        ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, challengeId);
        Objects.requireNonNull(completion);
        if (completion.getTimesCompleted() > 0) {
            sender.sendMessage(I18nUtil.tr("\u00a74Challenge {0} has already been completed", challengeId.id()));
        } else {
            playerInfo.completeChallenge(challenge, true);
            playerInfo.save();
            sender.sendMessage(I18nUtil.tr("\u00a7eChallenge {0} has been completed for {1}", challengeId.id(), playerInfo.getPlayerName()));
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
                        Challenge challenge = result.getChallenge();
                        ChallengeCompletion completion = challengeLogic.getChallengeCompletion(playerInfo, challenge.getId());
                        if (completion != null) {
                            doExecute(sender, playerInfo, completion);
                            return true;
                        }
                    }
                    case AMBIGUOUS -> {
                        String hint = result.getSuggestions().isEmpty() ? "" : " " + String.join(", ", result.getSuggestions());
                        sender.sendMessage(I18nUtil.tr("\u00a74Ambiguous challenge name: {0}. Did you mean:{1}", result.getNormalizedInput(), hint));
                        return false;
                    }
                    case NOT_FOUND -> {
                        sender.sendMessage(I18nUtil.tr("\u00a74No challenge named {0} was found!", result.getNormalizedInput()));
                        return false;
                    }
                }
            } else {
                sender.sendMessage(I18nUtil.tr("\u00a74No player named {0} was found!", data.get("player")));
            }
            return false;
        }
    }

    private abstract class RankCommand extends AbstractCommand {
        public RankCommand(String name, String permission, String description) {
            super(name, permission, "rank", description);
        }

        protected abstract void doExecute(CommandSender sender, PlayerInfo playerInfo, String rankName, List<Challenge> challenge);

        @Override
        public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
            PlayerInfo playerInfo = (PlayerInfo) data.get("playerInfo");
            if (playerInfo != null && args.length > 0) {
                String rankName = String.join(" ", args);
                List<Challenge> challenges = plugin.getChallengeLogic().getChallengesForRank(rankName);
                if (challenges == null || challenges.isEmpty()) {
                    sender.sendMessage(I18nUtil.tr("\u00a74No rank named {0} was found!", rankName));
                } else {
                    doExecute(sender, playerInfo, rankName, challenges);
                    return true;
                }
            } else {
                sender.sendMessage(I18nUtil.tr("\u00a74No player named {0} was found!", data.get("player")));
            }
            return false;
        }
    }
}
