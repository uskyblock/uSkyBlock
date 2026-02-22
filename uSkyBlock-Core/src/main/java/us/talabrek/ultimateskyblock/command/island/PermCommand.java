package us.talabrek.ultimateskyblock.command.island;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.menu.PartyPermissionMenuItem;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import static us.talabrek.ultimateskyblock.util.Msg.send;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendLegacy;

public class PermCommand extends RequireIslandCommand {

    @Inject
    public PermCommand(@NotNull uSkyBlock plugin) {
        super(plugin, "perm", "usb.island.perm", "member ?perm", marktr("changes a member's island permissions"));
    }

    @Override
    protected boolean doExecute(String alias, Player player, PlayerInfo pi, IslandInfo island, Map<String, Object> data, String... args) {
        String playerName = args.length > 0 ? args[0] : null;
        String perm = args.length > 1 ? args[1] : null;
        if (playerName != null && island.getMembers().contains(playerName) && perm == null) {
            String msg = trLegacy("<muted>Permissions for <primary><player></primary>:<newline>",
                unparsed("player", playerName));
            for (String validPerm : getValidPermissions()) {
                boolean permValue = island.hasPerm(playerName, validPerm);
                msg += miniToLegacy("<muted> - <primary><permission></primary>: <state><newline>",
                    unparsed("permission", validPerm),
                    legacyArg("state", permValue ? trLegacy("<secondary>ON") : trLegacy("<error>OFF")));
            }
            sendLegacy(player, msg.trim().split("\n"));
            return true;
        }
        if (playerName == null || perm == null || perm.isEmpty() || playerName.isEmpty()) {
            return false;
        }
        if (!isValidPermission(perm)) {
            send(player, tr("<error>Invalid permission <permission>. Must be one of <permissions>",
                unparsed("permission", perm),
                unparsed("permissions", String.join(", ", getValidPermissions()))));
            return true;
        }
        if (island.togglePerm(playerName, perm)) {
            boolean permValue = island.hasPerm(playerName, perm);
            send(player, tr("Toggled permission <primary><permission></primary> for <primary><player></primary> to <state>.",
                unparsed("permission", perm),
                unparsed("player", playerName),
                component("state", permValue ? tr("<secondary>ON") : tr("<error>OFF"))));
        } else {
            send(player, tr("<error>Unable to toggle permission <primary><permission></primary> for <primary><player></primary>.",
                unparsed("permission", perm),
                unparsed("player", playerName)));
        }
        return true;
    }

    private boolean isValidPermission(String perm) {
        return getValidPermissions().contains(perm);
    }

    private List<String> getValidPermissions() {
        List<String> list = new ArrayList<>();
        for (PartyPermissionMenuItem item : plugin.getMenu().getPermissionMenuItems()) {
            list.add(item.getPerm());
        }
        return list;
    }
}
