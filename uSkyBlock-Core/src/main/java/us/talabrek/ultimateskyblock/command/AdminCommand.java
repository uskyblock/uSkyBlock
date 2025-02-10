package us.talabrek.ultimateskyblock.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.animation.AnimationHandler;
import dk.lockfuglsang.minecraft.command.BaseCommandExecutor;
import dk.lockfuglsang.minecraft.command.DocumentCommand;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.command.admin.AdminChallengeCommand;
import us.talabrek.ultimateskyblock.command.admin.AdminIslandCommand;
import us.talabrek.ultimateskyblock.command.admin.ChunkCommand;
import us.talabrek.ultimateskyblock.command.admin.CooldownCommand;
import us.talabrek.ultimateskyblock.command.admin.DebugCommand;
import us.talabrek.ultimateskyblock.command.admin.FlatlandFixCommand;
import us.talabrek.ultimateskyblock.command.admin.FlushCommand;
import us.talabrek.ultimateskyblock.command.admin.GenTopTenCommand;
import us.talabrek.ultimateskyblock.command.admin.GotoIslandCommand;
import us.talabrek.ultimateskyblock.command.admin.ImportCommand;
import us.talabrek.ultimateskyblock.command.admin.ItemInfoCommand;
import us.talabrek.ultimateskyblock.command.admin.JobsCommand;
import us.talabrek.ultimateskyblock.command.admin.LanguageCommand;
import us.talabrek.ultimateskyblock.command.admin.OrphanCommand;
import us.talabrek.ultimateskyblock.command.admin.PerkCommand;
import us.talabrek.ultimateskyblock.command.admin.PlayerInfoCommand;
import us.talabrek.ultimateskyblock.command.admin.ProtectAllCommand;
import us.talabrek.ultimateskyblock.command.admin.PurgeCommand;
import us.talabrek.ultimateskyblock.command.admin.RegionCommand;
import us.talabrek.ultimateskyblock.command.admin.ReloadCommand;
import us.talabrek.ultimateskyblock.command.admin.SetMaintenanceCommand;
import us.talabrek.ultimateskyblock.command.admin.VersionCommand;
import us.talabrek.ultimateskyblock.command.admin.WGCommand;
import us.talabrek.ultimateskyblock.command.completion.AllPlayerTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.BiomeTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.ChallengeTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.OnlinePlayerTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.RankTabCompleter;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;

/**
 * The new admin command, alias /usb
 */
@Singleton
public class AdminCommand extends BaseCommandExecutor {

    @Inject
    public AdminCommand(
        @NotNull uSkyBlock plugin,
        @NotNull ConfirmHandler confirmHandler,
        @NotNull AnimationHandler animationHandler,

        @NotNull OnlinePlayerTabCompleter playerCompleter,
        @NotNull ChallengeTabCompleter challengeCompleter,
        @NotNull AllPlayerTabCompleter allPlayerCompleter,
        @NotNull BiomeTabCompleter biomeCompleter,
        @NotNull RankTabCompleter rankTabCompleter,

        @NotNull ReloadCommand reloadCommand,
        @NotNull ImportCommand importCommand,
        @NotNull GenTopTenCommand genTopTenCommand,
        @NotNull AdminChallengeCommand adminChallengeCommand,
        @NotNull OrphanCommand orphanCommand,
        @NotNull AdminIslandCommand adminIslandCommand,
        @NotNull PurgeCommand purgeCommand,
        @NotNull GotoIslandCommand gotoIslandCommand,
        @NotNull PlayerInfoCommand playerInfoCommand,
        @NotNull FlatlandFixCommand flatlandFixCommand,
        @NotNull DebugCommand debugCommand,
        @NotNull WGCommand wgCommand,
        @NotNull VersionCommand versionCommand,
        @NotNull CooldownCommand cooldownCommand,
        @NotNull PerkCommand perkCommand,
        @NotNull LanguageCommand languageCommand,
        @NotNull FlushCommand flushCommand,
        @NotNull JobsCommand jobsCommand,
        @NotNull DocumentCommand documentCommand,
        @NotNull RegionCommand regionCommand,
        @NotNull SetMaintenanceCommand setMaintenanceCommand,
        @NotNull ItemInfoCommand itemInfoCommand,
        @NotNull ProtectAllCommand protectAllCommand,
        @NotNull ChunkCommand chunkCommand

    ) {
        super("usb", null, marktr("Ultimate SkyBlock Admin"));

        addTab("oplayer", playerCompleter);
        addTab("player", allPlayerCompleter);
        addTab("island", allPlayerCompleter);
        addTab("leader", allPlayerCompleter);
        addTab("challenge", challengeCompleter);
        addTab("biome", biomeCompleter);
        addTab("rank", rankTabCompleter);

        add(reloadCommand);
        add(importCommand);
        add(genTopTenCommand);
        add(adminChallengeCommand);
        add(orphanCommand);
        add(adminIslandCommand);
        add(purgeCommand);
        add(gotoIslandCommand);
        add(playerInfoCommand);
        add(flatlandFixCommand);
        add(debugCommand);
        add(wgCommand);
        add(versionCommand);
        add(cooldownCommand);
        add(perkCommand);
        add(languageCommand);
        add(flushCommand);
        add(jobsCommand);
        add(documentCommand);
        add(regionCommand);
        add(setMaintenanceCommand);
        add(itemInfoCommand);
        add(protectAllCommand);
        add(chunkCommand);
    }
}
