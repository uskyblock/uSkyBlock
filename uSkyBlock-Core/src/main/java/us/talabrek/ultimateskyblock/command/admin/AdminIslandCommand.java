package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.handler.ConfirmHandler;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.send;

/**
 * The island moderator command.
 */
public class AdminIslandCommand extends CompositeCommand {
    private final uSkyBlock plugin;

    @Inject
    public AdminIslandCommand(@NotNull uSkyBlock plugin, @NotNull ConfirmHandler confirmHandler) {
        super("island|is", "usb.admin.island", marktr("manage islands"));
        this.plugin = plugin;
        add(new AbstractIslandInfoCommand("protect", "usb.admin.protect", marktr("protects the island")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, String... args) {
                protectIsland(sender, islandInfo);
            }
        });
        add(new AbstractCommand("delete", "usb.admin.delete", "?leader", marktr("delete the island (removes the blocks)")) {
            @Override
            public boolean execute(final CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length == 1) {
                    PlayerInfo playerInfo = plugin.getPlayerInfo(args[0]);
                    if (playerInfo == null) {
                        send(sender, tr("<error>Could not locate an island for player <player>!", unparsed("player", args[0])));
                        return false;
                    }
                    deleteIsland(sender, playerInfo);
                    return true;
                } else if (args.length == 0 && sender instanceof Player) {
                    String islandName = WorldGuardHandler.getIslandNameAt(((Player) sender).getLocation());
                    if (islandName != null) {
                        if (plugin.deleteEmptyIsland(islandName, new Runnable() {
                            @Override
                            public void run() {
                                send(sender, tr("<primary>Deleted abandoned island at your current location."));
                            }
                        })) {
                            return true;
                        } else {
                            send(sender, tr("<error>Island at this location has members.</error><newline><muted>Use <cmd>/usb island delete [name]</cmd> to delete it."));
                        }
                    }
                }
                return false;
            }
        });
        add(new AbstractIslandInfoCommand("remove", "usb.admin.remove", marktr("removes the player from the island")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, String... args) {
                removePlayerFromIsland(sender, playerInfo, islandInfo);
            }
        });
        add(new AbstractCommand("addmember|add", "usb.admin.addmember", "player ?island", marktr("adds the player to the island")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                IslandInfo islandInfo = null;
                if (args.length == 2) {
                    islandInfo = plugin.getIslandInfo(Bukkit.getPlayer(args[1]));
                } else if (args.length == 1 && sender instanceof Player) {
                    String islandName = WorldGuardHandler.getIslandNameAt(((Player) sender).getLocation());
                    islandInfo = plugin.getIslandInfo(islandName);
                }
                if (islandInfo != null && args.length > 0) {
                    PlayerInfo playerInfo = plugin.getPlayerInfo(args[0]);
                    if (playerInfo != null) {
                        islandInfo.addMember(playerInfo);
                        playerInfo.save();
                        islandInfo.sendMessageToIslandGroup(tr("<primary><player></primary> has joined your island group.",
                            legacyArg("player", playerInfo.getDisplayName())));
                        return true;
                    } else {
                        send(sender, tr("<error>No player named <player> was found!", unparsed("player", args[0])));
                    }
                } else {
                    send(sender, tr("<error>No valid island provided, either stand within one, or provide an island name"));
                }
                return false;
            }
        });
        add(new AbstractIslandInfoCommand("info", "usb.admin.info", marktr("print out info about the island")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, String... args) {
                send(sender, islandInfo.asComponentLines());
            }
        });
        add(new AbstractCommand("setbiome", "usb.admin.setbiome", "?leader biome", marktr("sets the biome of the island")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (args.length == 2) {
                    PlayerInfo playerInfo = plugin.getPlayerInfo(args[0]);
                    if (playerInfo == null || !playerInfo.getHasIsland()) {
                        send(sender, tr("<error>That player does not have an island!"));
                        return false;
                    }
                    Biome biome = plugin.getBiome(args[0]);
                    if (biome == null) {
                        return false;
                    }
                    setBiome(sender, playerInfo, plugin.getIslandInfo(playerInfo), biome);
                    return true;
                } else if (args.length == 1 && sender instanceof Player) {
                    Biome biome = plugin.getBiome(args[0]);
                    String islandName = WorldGuardHandler.getIslandNameAt(((Player) sender).getLocation());
                    if (biome == null || islandName == null) {
                        return false;
                    }
                    IslandInfo islandInfo = plugin.getIslandInfo(islandName);
                    if (islandInfo == null) {
                        send(sender, tr("<error>No valid island at your location"));
                        return false;
                    }
                    setBiome(sender, islandInfo, biome);
                    return true;
                }
                return false;
            }
        });
        add(new AbstractCommand("purge", "usb.admin.purge", "?leader", marktr("purges the island")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                String cmd = "/usb island purge";
                IslandInfo islandInfo = null;
                if (args.length == 0 && sender instanceof Player player) {
                    String islandName = WorldGuardHandler.getIslandNameAt(player.getLocation());
                    islandInfo = plugin.getIslandInfo(islandName);
                } else if (args.length == 1) {
                    cmd += " " + args[0];
                    PlayerInfo playerInfo = plugin.getPlayerInfo(args[0]);
                    islandInfo = plugin.getIslandInfo(playerInfo);
                }
                if (islandInfo == null) {
                    send(sender, tr("<error>Error! <primary>No valid island found for purging."));
                    return false;
                } else {
                    String islandName = islandInfo.getName();
                    if (sender instanceof Player && confirmHandler.checkCommand((Player) sender, cmd)) {
                        plugin.getIslandLogic().purge(islandName);
                        send(sender, tr("<error>PURGE: <primary>Purged island at <island>", unparsed("island", islandName)));
                    } else if (!(sender instanceof Player)) {
                        plugin.getIslandLogic().purge(islandName);
                        send(sender, tr("<error>PURGE: <primary>Purged island at <island>", unparsed("island", islandName)));
                    }
                }
                return true;
            }
        });
        add(new MakeLeaderCommand(plugin));
        add(new RegisterIslandToPlayerCommand());
        add(new AbstractIslandInfoCommand("ignore", "usb.admin.ignore", marktr("toggles the island's ignore status")) {
            @Override
            protected void doExecute(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, String... args) {
                if (islandInfo != null) {
                    islandInfo.setIgnore(!islandInfo.ignore());
                    if (islandInfo.ignore()) {
                        send(sender, tr("<error>Set <leader>'s island to be ignored on top-ten and purge.",
                            unparsed("leader", islandInfo.getLeader())));
                    } else {
                        send(sender, tr("<error>Removed ignore-flag of <leader>'s island, it will now show up on top-ten and purge.",
                            unparsed("leader", islandInfo.getLeader())));
                    }
                }
            }
        });
        add(new SetIslandDataCommand(plugin));
        add(new GetIslandDataCommand());
    }

    private void removePlayerFromIsland(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo) {
        if (playerInfo == null) {
            send(sender, tr("<error>No valid player name supplied."));
            return;
        }
        send(sender, tr("Removing <player> from the island.", unparsed("player", playerInfo.getPlayerName())));
        islandInfo.removeMember(playerInfo);
        playerInfo.save();
    }

    private void setBiome(CommandSender sender, IslandInfo islandInfo, Biome biome) {
        uSkyBlock.getInstance().setBiome(islandInfo.getIslandLocation(), biome);
        islandInfo.setBiome(biome);
        send(sender, tr("Changed biome of <primary><leader></primary>'s island to <primary><biome></primary>.",
            unparsed("leader", islandInfo.getLeader()),
            unparsed("biome", biome.name())));
        send(sender, tr("<secondary>You may need to go to spawn, or relog, to see the changes."));
    }

    private void setBiome(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, Biome biome) {
        if (playerInfo == null || !playerInfo.getHasIsland()) {
            send(sender, tr("<error>That player does not have an island!"));
            return;
        }
        uSkyBlock.getInstance().setBiome(playerInfo.getIslandLocation(), biome);
        islandInfo.setBiome(biome);
        send(sender, tr("Changed <primary><player></primary>'s biome to <primary><biome></primary>.",
            unparsed("player", playerInfo.getPlayerName()),
            unparsed("biome", biome.name())));
        send(sender, tr("<secondary>You may need to go to spawn, or relog, to see the changes."));
    }

    private void deleteIsland(CommandSender sender, PlayerInfo playerInfo) {
        if (playerInfo != null && playerInfo.getIslandLocation() != null) {
            send(sender, tr("Removing <primary><player></primary>'s island.", unparsed("player", playerInfo.getPlayerName())));
            uSkyBlock.getInstance().deletePlayerIsland(playerInfo.getPlayerName(), null);
        } else {
            send(sender, tr("<error>That player does not have an island!"));
        }
    }

    private void protectIsland(CommandSender sender, IslandInfo islandInfo) {
        if (WorldGuardHandler.protectIsland(plugin, sender, islandInfo)) {
            send(sender, tr("<primary><leader></primary>'s island at <primary><island></primary> has been protected.",
                unparsed("leader", islandInfo.getLeader()),
                unparsed("island", islandInfo.getName())));
        } else {
            send(sender, tr("<error><leader>'s island at <island> was already protected",
                unparsed("leader", islandInfo.getLeader()),
                unparsed("island", islandInfo.getName())));
        }
    }
}
