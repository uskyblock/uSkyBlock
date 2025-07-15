package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

public class FarewellCommand extends RequireIslandCommand {
    @Inject
    public FarewellCommand(uSkyBlock plugin) {
        super(plugin, "farewell", null, "?msg", marktr("Change farewell message of your island"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        us.talabrek.ultimateskyblock.api.IslandInfo islandInfo = uSkyBlock.getInstance().getIslandInfo(player);
        StringBuilder str= new StringBuilder();
        for (String arg:args){
            str.append(arg).append(" ");
        }
        String cmd = "console:rg flag -w skyworld " + islandInfo.getName() + "island farewell "+str;
        uSkyBlock.getInstance().execCommand(player, cmd,false);

        str = new StringBuilder(str.toString().replaceAll("&(?=[0-9a-fA-FkKlLmMnNoOrR])", "\u00a7"));
        player.sendMessage(tr("\u00a7bYour island farewell message has changed to: \u00a7r")+str);
        return true;
    }
}
