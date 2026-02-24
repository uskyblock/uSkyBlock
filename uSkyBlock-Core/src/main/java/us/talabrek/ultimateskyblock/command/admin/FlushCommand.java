package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.SECONDARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

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
        sendTr(sender, "Flushed <islands> islands, <players> players, and <challenges> challenge completions.",
            unparsed("islands", String.valueOf(flushedIslands), SECONDARY),
            unparsed("players", String.valueOf(flushedPlayers), PRIMARY),
            unparsed("challenges", String.valueOf(flushedChallenges), PRIMARY));
        return true;
    }
}
