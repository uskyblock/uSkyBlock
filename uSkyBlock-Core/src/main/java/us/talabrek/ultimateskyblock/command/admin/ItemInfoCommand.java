package us.talabrek.ultimateskyblock.command.admin;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.AbstractCommand;
import dk.lockfuglsang.minecraft.command.CompositeCommand;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Msg.*;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;

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
                    sendTr(player, "Info for <item>", unparsed("item", ItemStackUtil.asString(itemStack), PRIMARY));
                    sendTr(player, " - name: <item-name>", component("item-name", ItemStackUtil.getItemName(itemStack), PRIMARY));
                    return true;
                }
                sendPlayerOnly(sender);
                return false;
            }
        });
    }
}
