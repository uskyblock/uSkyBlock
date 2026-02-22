package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

public class FlushCommand extends AbstractCommand {
    private final uSkyBlock plugin;

    @Inject
    public FlushCommand(@NotNull uSkyBlock plugin) {
        super("flush", "usb.admin.cache", marktr("flushes all caches to files"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
        long flushedIslands = plugin.getIslandLogic().flushCache();
        long flushedPlayers = plugin.getPlayerLogic().flushCache();
        long flushedChallenges = plugin.getChallengeLogic().flushCache();
        send(sender, tr("Flushed <secondary><islands></secondary> islands, <primary><players></primary> players, and <primary><challenges></primary> challenge completions.",
            unparsed("islands", String.valueOf(flushedIslands)),
            unparsed("players", String.valueOf(flushedPlayers)),
            unparsed("challenges", String.valueOf(flushedChallenges))));
        return true;
    }
}
