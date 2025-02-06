package us.talabrek.ultimateskyblock.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.command.BaseCommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;
import us.talabrek.ultimateskyblock.command.completion.AllPlayerTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.BiomeTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.MemberTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.OnlinePlayerTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.PermissionTabCompleter;
import us.talabrek.ultimateskyblock.command.completion.SchematicTabCompleter;
import us.talabrek.ultimateskyblock.command.island.AcceptRejectCommand;
import us.talabrek.ultimateskyblock.command.island.AutoCommand;
import us.talabrek.ultimateskyblock.command.island.BanCommand;
import us.talabrek.ultimateskyblock.command.island.BiomeCommand;
import us.talabrek.ultimateskyblock.command.island.CreateCommand;
import us.talabrek.ultimateskyblock.command.island.HomeCommand;
import us.talabrek.ultimateskyblock.command.island.InfoCommand;
import us.talabrek.ultimateskyblock.command.island.InviteCommand;
import us.talabrek.ultimateskyblock.command.island.KickCommand;
import us.talabrek.ultimateskyblock.command.island.LeaveCommand;
import us.talabrek.ultimateskyblock.command.island.LevelCommand;
import us.talabrek.ultimateskyblock.command.island.LockUnlockCommand;
import us.talabrek.ultimateskyblock.command.island.LogCommand;
import us.talabrek.ultimateskyblock.command.island.MakeLeaderCommand;
import us.talabrek.ultimateskyblock.command.island.MobLimitCommand;
import us.talabrek.ultimateskyblock.command.island.PartyCommand;
import us.talabrek.ultimateskyblock.command.island.PermCommand;
import us.talabrek.ultimateskyblock.command.island.RestartCommand;
import us.talabrek.ultimateskyblock.command.island.SetHomeCommand;
import us.talabrek.ultimateskyblock.command.island.SetWarpCommand;
import us.talabrek.ultimateskyblock.command.island.SpawnCommand;
import us.talabrek.ultimateskyblock.command.island.ToggleWarp;
import us.talabrek.ultimateskyblock.command.island.TopCommand;
import us.talabrek.ultimateskyblock.command.island.TrustCommand;
import us.talabrek.ultimateskyblock.command.island.WarpCommand;
import us.talabrek.ultimateskyblock.menu.SkyBlockMenu;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * The main /island command
 */
@Singleton
public class IslandCommand extends BaseCommandExecutor {
    private final uSkyBlock plugin;
    private final SkyBlockMenu menu;

    @Inject
    public IslandCommand(
        @NotNull uSkyBlock plugin,
        @NotNull SkyBlockMenu menu,

        @NotNull OnlinePlayerTabCompleter onlinePlayerTabCompleter,
        @NotNull AllPlayerTabCompleter allPlayerTabCompleter,
        @NotNull BiomeTabCompleter biomeTabCompleter,
        @NotNull MemberTabCompleter memberTabCompleter,
        @NotNull SchematicTabCompleter schematicTabCompleter,
        @NotNull PermissionTabCompleter permissionTabCompleter,

        @NotNull HomeCommand homeCommand,
        @NotNull LevelCommand levelCommand,
        @NotNull InfoCommand infoCommand,
        @NotNull InviteCommand inviteCommand,
        @NotNull AcceptRejectCommand acceptRejectCommand,
        @NotNull LeaveCommand leaveCommand,
        @NotNull KickCommand kickCommand,
        @NotNull PartyCommand partyCommand,
        @NotNull MakeLeaderCommand makeLeaderCommand,
        @NotNull SpawnCommand spawnCommand,
        @NotNull TrustCommand trustCommand,
        @NotNull MobLimitCommand mobLimitCommand,
        @NotNull AutoCommand autoCommand,
        @NotNull PermCommand permCommand,
        @NotNull RestartCommand restartCommand,
        @NotNull LogCommand logCommand,
        @NotNull CreateCommand createCommand,
        @NotNull SetHomeCommand setHomeCommand,
        @NotNull SetWarpCommand setWarpCommand,
        @NotNull WarpCommand warpCommand,
        @NotNull ToggleWarp toggleWarpCommand,
        @NotNull BanCommand banCommand,
        @NotNull LockUnlockCommand lockUnlockCommand,
        @NotNull TopCommand topCommand,
        @NotNull BiomeCommand biomeCommand
    ) {
        super("island|is", "usb.island.create", marktr("general island command"));
        this.plugin = plugin;
        this.menu = menu;

        addFeaturePermission("usb.mod.bypasscooldowns", tr("allows user to bypass cooldowns"));
        addFeaturePermission("usb.mod.bypassprotection", tr("allows user to bypass visitor-protections"));
        addFeaturePermission("usb.mod.bypassteleport", tr("allows user to bypass teleport-delay"));
        addFeaturePermission("usb.island.signs.use", tr("allows user to use [usb] signs"));
        addFeaturePermission("usb.island.signs.place", tr("allows user to place [usb] signs"));

        addTab("island", allPlayerTabCompleter);
        addTab("player", allPlayerTabCompleter);
        addTab("oplayer", onlinePlayerTabCompleter);
        addTab("biome", biomeTabCompleter);
        addTab("member", memberTabCompleter);
        addTab("schematic", schematicTabCompleter);
        addTab("perm", permissionTabCompleter);

        add(restartCommand);
        add(logCommand);
        add(createCommand);
        add(setHomeCommand);
        add(homeCommand);
        add(setWarpCommand);
        add(warpCommand);
        add(toggleWarpCommand);
        add(banCommand);
        add(lockUnlockCommand);
        if (Settings.island_useTopTen) {
            add(topCommand);
        }
        add(biomeCommand);
        add(levelCommand);
        add(infoCommand);
        add(inviteCommand);
        add(acceptRejectCommand);
        add(leaveCommand);
        add(kickCommand);
        add(partyCommand);
        add(makeLeaderCommand);
        add(spawnCommand);
        add(trustCommand);
        add(mobLimitCommand);
        add(autoCommand);
        add(permCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!plugin.isRequirementsMet(sender, this, args)) {
            return true;
        }
        if (sender instanceof Player player) {
            if (args.length == 0) {
                player.openInventory(menu.displayIslandGUI(player));
                return true;
            }
        }
        return super.onCommand(sender, command, alias, args);
    }
}
