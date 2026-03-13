package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendNoCommandAccess;

public class LockUnlockCommand extends RequireIslandCommand {
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public LockUnlockCommand(@NotNull uSkyBlock plugin, @NotNull RuntimeConfigs runtimeConfigs) {
        super(plugin, "lock|unlock", "usb.island.lock", marktr("lock your island to non-party members."));
        this.runtimeConfigs = runtimeConfigs;
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        if (alias.equalsIgnoreCase("lock") && pi.getHasIsland()) {
            if (runtimeConfigs.current().island().allowIslandLock()) {
                if (island.hasPerm(player, "canToggleLock")) {
                    island.lock(player);
                } else {
                    sendErrorTr(player, "You do not have permission to lock your island!");
                }
            } else {
                sendNoCommandAccess(player);
            }
            return true;
        }
        if (alias.equalsIgnoreCase("unlock") && pi.getHasIsland()) {
            if (runtimeConfigs.current().island().allowIslandLock()) {
                if (island.hasPerm(player, "canToggleLock")) {
                    island.unlock(player);
                } else {
                    sendErrorTr(player, "You do not have permission to unlock your island!");
                }
            } else {
                sendNoCommandAccess(player);
            }
            return true;
        }
        return false;
    }
}
