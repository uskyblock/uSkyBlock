package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public class GreetingCommand extends RequireIslandCommand {
    @Inject
    public GreetingCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "greeting", null, "?msg", marktr("Change greeting message of your island"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, us.talabrek.ultimateskyblock.island.IslandInfo island, Map<String, Object> data, String... args) {
        IslandInfo islandInfo = uSkyBlock.getInstance().getIslandInfo(player);
        StringBuilder str= new StringBuilder();
        for (String arg:args){
            str.append(arg).append(" ");
        }
        String cmd = "console:rg flag -w skyworld " + islandInfo.getName() + "island greeting "+str;
        uSkyBlock.getInstance().execCommand(player, cmd,false);

        str = new StringBuilder(str.toString().replaceAll("&(?=[0-9a-fA-FkKlLmMnNoOrR])", "\u00a7"));
        player.sendMessage(tr("\u00a7bYour island greeting message has changed to: \u00a7r")+str);
        return true;
    }
}
