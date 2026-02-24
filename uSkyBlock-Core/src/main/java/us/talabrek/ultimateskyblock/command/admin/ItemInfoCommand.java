package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.legacyArg;
import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendPlayerOnly;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Command for querying items reg. NBT stuff
 */
public class ItemInfoCommand extends CompositeCommand {

    @Inject
    public ItemInfoCommand() {
        super("iteminfo", "usb.admin.iteminfo", marktr("advanced info about items"));
        add(new AbstractCommand("info|i", marktr("shows the component format for the currently held item")) {
            @Override
            public boolean execute(CommandSender sender, String alias, Map<String, Object> data, String... args) {
                if (sender instanceof Player player) {
                    ItemStack itemStack = player.getInventory().getItemInMainHand();
                    if (!itemStack.getType().isItem()) {
                        sendErrorTr(player, "No item in hand!");
                        return true;
                    }
                    sendTr(player, "Info for <primary><item></primary>", unparsed("item", ItemStackUtil.asString(itemStack)));
                    sendTr(player, " - name: <primary><name>", legacyArg("name", ItemStackUtil.getItemName(itemStack)));
                    return true;
                }
                sendPlayerOnly(sender);
                return false;
            }
        });
    }
}
