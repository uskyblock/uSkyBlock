package us.talabrek.ultimateskyblock.command.completion;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.island.IslandLogic;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.player.PlayerLogic;

import java.util.ArrayList;
import java.util.List;

/**
 * Only list members of your current party.
 */
public class MemberTabCompleter implements TabCompleter {
    private final PlayerLogic playerLogic;
    private final IslandLogic islandLogic;

    @Inject
    public MemberTabCompleter(
        @NotNull PlayerLogic playerLogic,
        @NotNull IslandLogic islandLogic
    ) {
        this.playerLogic = playerLogic;
        this.islandLogic = islandLogic;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (sender instanceof Player) {
            PlayerInfo playerInfo = playerLogic.getPlayerInfo((Player) sender);
            if (playerInfo != null && playerInfo.getHasIsland()) {
                IslandInfo islandInfo = islandLogic.getIslandInfo(playerInfo);
                if (islandInfo != null) {
                    String member = args.length > 0 ? args[args.length - 1] : "";
                    return AbstractTabCompleter.filter(new ArrayList<>(islandInfo.getMembers()), member);
                }
            }
        }
        return null;
    }
}
