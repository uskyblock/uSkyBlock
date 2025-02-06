package us.talabrek.ultimateskyblock.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.command.BaseCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.command.challenge.ChallengeCompleteCommand;
import us.talabrek.ultimateskyblock.command.challenge.ChallengeInfoCommand;
import us.talabrek.ultimateskyblock.command.completion.AvailableChallengeTabCompleter;
import us.talabrek.ultimateskyblock.menu.SkyBlockMenu;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Primary challenges command
 */
@Singleton
public class ChallengeCommand extends BaseCommandExecutor {
    private final uSkyBlock plugin;
    private final ChallengeLogic challengeLogic;
    private final WorldManager worldManager;
    private final PlayerLogic playerLogic;
    private final SkyBlockMenu mainMenu;

    @Inject
    public ChallengeCommand(
        @NotNull uSkyBlock plugin,
        @NotNull ChallengeCompleteCommand challengeCompleteCommand,
        @NotNull AvailableChallengeTabCompleter availableChallengeTabCompleter,
        @NotNull ChallengeInfoCommand challengeInfoCommand,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull WorldManager worldManager,
        @NotNull PlayerLogic playerLogic,
        @NotNull SkyBlockMenu mainMenu
    ) {
        super("challenges|c", "usb.island.challenges", marktr("complete and list challenges"));
        this.plugin = plugin;
        this.challengeLogic = challengeLogic;
        this.worldManager = worldManager;
        this.playerLogic = playerLogic;
        this.mainMenu = mainMenu;
        addTab("challenge", availableChallengeTabCompleter);
        add(challengeCompleteCommand);
        add(challengeInfoCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!plugin.isRequirementsMet(sender, this, args)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(tr("\u00a7cCommand only available for players."));
            return false;
        }
        if (!challengeLogic.isEnabled()) {
            sender.sendMessage(tr("\u00a7eChallenges has been disabled. Contact an administrator."));
            return false;
        }
        if (!worldManager.isSkyAssociatedWorld(player.getWorld())) {
            player.sendMessage(tr("\u00a74You can only submit challenges in the skyblock world!"));
            return true;
        }
        PlayerInfo playerInfo = playerLogic.getPlayerInfo(player);
        if (!playerInfo.getHasIsland()) {
            player.sendMessage(tr("\u00a74You can only submit challenges when you have an island!"));
            return true;
        }
        if (args.length == 0) {
            player.openInventory(mainMenu.displayChallengeGUI(player, 1, null));
            return true;
        } else {
            return super.onCommand(sender, command, alias, args);
        }
    }
}
