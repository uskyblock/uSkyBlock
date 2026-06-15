package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.BiomeReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.CommandReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.CommandSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.EconomyReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.ExperienceReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.ItemReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.PermissionReward;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards.RewardAction;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.gameobject.ItemStackAmountProbabilitySpec;
import us.talabrek.ultimateskyblock.hook.HookManager;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PerkLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.send;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.number;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

/**
 * Hands out catalog reward bundles. Invoked by {@link ChallengeExecutor} only after the
 * completion has been persisted.
 */
public final class RewardApplier {
    private static final Random RND = new Random();

    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final HookManager hookManager;
    private final PerkLogic perkLogic;

    public RewardApplier(
        @NotNull uSkyBlock plugin,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull HookManager hookManager,
        @NotNull PerkLogic perkLogic
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.runtimeConfigs = Objects.requireNonNull(runtimeConfigs, "runtimeConfigs");
        this.hookManager = Objects.requireNonNull(hookManager, "hookManager");
        this.perkLogic = Objects.requireNonNull(perkLogic, "perkLogic");
    }

    public void apply(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull ChallengeDefinition challenge,
        boolean firstCompletion
    ) {
        RewardBundle bundle = firstCompletion || challenge.repeatReward().isEmpty()
            ? challenge.firstCompletionReward()
            : challenge.repeatReward();
        Component challengeName = ChallengeText.displayName(challenge);
        sendTr(player, "You completed the <challenge> challenge!", component("challenge", challengeName, PRIMARY));

        for (RewardAction action : bundle.actions()) {
            switch (action) {
                case ItemReward itemReward -> grantItems(player, itemReward);
                case ExperienceReward experienceReward -> grantExperience(player, experienceReward.amount());
                case EconomyReward economyReward -> grantCurrency(player, economyReward.amount());
                case PermissionReward permissionReward -> grantPermissions(playerInfo, permissionReward.permissions());
                // Biome unlocks are derived from completion state; only announce them.
                case BiomeReward biomeReward -> sendTr(player, "Unlocked island biome: <biomes>",
                    unparsed("biomes", String.join(", ", biomeReward.biomes()), PRIMARY));
                case CommandReward commandReward -> runCommands(player, challenge, commandReward.commands());
            }
        }

        announceCompletion(player, playerInfo, challengeName, firstCompletion);
    }

    private void grantItems(@NotNull Player player, @NotNull ItemReward itemReward) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStackAmountProbabilitySpec spec : itemReward.itemSpecs()) {
            if (RND.nextDouble() < spec.probability()) {
                items.addAll(spec.item().stacks());
            }
        }
        if (items.isEmpty()) {
            return;
        }
        List<Component> names = items.stream()
            .map(item -> tr("<amount> x <item>",
                number("amount", item.getAmount()),
                component("item", ItemStackUtil.getItemName(item))))
            .toList();
        sendTr(player, "Item rewards: <items>",
            component("items", Component.join(JoinConfiguration.commas(true), names), PRIMARY));
        HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        for (ItemStack item : leftOvers.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
        if (!leftOvers.isEmpty()) {
            sendErrorTr(player, "Your inventory is full. <muted>Items were dropped on the ground.");
        }
    }

    private void grantExperience(@NotNull Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        player.giveExp(amount);
        sendTr(player, "XP reward: <experience:'0'>", number("experience", amount, PRIMARY));
    }

    private void grantCurrency(@NotNull Player player, int amount) {
        if (amount <= 0 || !runtimeConfigs.current().challenges().enableEconomyRewards()) {
            return;
        }
        double rewBonus = 1 + perkLogic.getPerk(player).getRewBonus();
        double currencyReward = amount * rewBonus;
        double percentage = rewBonus - 1.0;
        hookManager.getEconomyHook().ifPresent(hook -> {
            hook.depositPlayer(player, currencyReward);
            sendTr(player, "Currency reward: <primary><amount:'#,##0'><currency></primary> <secondary>(<bonus:'0%'>)</secondary>",
                number("amount", currencyReward),
                unparsed("currency", hook.getCurrenyName()),
                number("bonus", percentage));
        });
    }

    private void grantPermissions(@NotNull PlayerInfo playerInfo, @NotNull List<String> permissions) {
        if (permissions.isEmpty()) {
            return;
        }
        IslandInfo islandInfo = playerInfo.getIslandInfo();
        if (islandInfo == null) {
            return;
        }
        for (UUID memberUUID : islandInfo.getMemberUUIDs()) {
            if (memberUUID == null) {
                continue;
            }
            PlayerInfo member = plugin.getPlayerInfo(memberUUID);
            if (member != null) {
                member.addPermissions(permissions);
            }
        }
    }

    private void runCommands(@NotNull Player player, @NotNull ChallengeDefinition challenge, @NotNull List<CommandSpec> commands) {
        for (CommandSpec command : commands) {
            String rendered = command.command()
                .replaceAll("\\{challenge\\}", Matcher.quoteReplacement(challenge.id().value()))
                .replaceAll("\\{challengeName\\}", Matcher.quoteReplacement(ChallengeText.plainName(challenge)));
            String prefixed = switch (command.execution()) {
                case PLAYER -> rendered;
                case OP -> "op:" + rendered;
                case CONSOLE -> "console:" + rendered;
            };
            plugin.execCommand(player, prefixed, true);
        }
    }

    private void announceCompletion(
        @NotNull Player player,
        @NotNull PlayerInfo playerInfo,
        @NotNull Component challengeName,
        boolean firstCompletion
    ) {
        Component message = tr("<player> has completed the <challenge> challenge!",
            unparsed("player", player.getName(), PRIMARY),
            component("challenge", challengeName, PRIMARY));
        if (firstCompletion && runtimeConfigs.current().challenges().broadcast().enabled()) {
            String prefix = runtimeConfigs.current().challenges().broadcast().prefix();
            Component broadcast = I18nUtil.fromLegacy(prefix).append(message);
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                send(onlinePlayer, broadcast);
            }
            send(plugin.getServer().getConsoleSender(), broadcast);
            return;
        }
        IslandInfo island = playerInfo.getIslandInfo();
        if (island != null) {
            island.sendMessageToOnlineMembers(message);
        }
    }
}
