package us.talabrek.ultimateskyblock.command.admin;

import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static us.talabrek.ultimateskyblock.util.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.util.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.util.Msg.sendTr;

/**
 * Sets data directly on the IslandInfo object
 */
public class GetIslandDataCommand extends AbstractIslandInfoCommand {
    private final TabCompleter tabCompleter;
    private final List<String> getterNames;

    public GetIslandDataCommand() {
        super("get", "usb.admin.get", marktr("advanced command for getting island-data"));
        getterNames = new ArrayList<>();
        for (Method m : IslandInfo.class.getDeclaredMethods()) {
            if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
                String fieldName = m.getName().substring(3);
                fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
                getterNames.add(fieldName);
            }
        }
        tabCompleter = new ReflectionTabCompleter(getterNames);
    }

    @Override
    protected void doExecute(CommandSender sender, PlayerInfo playerInfo, IslandInfo islandInfo, String... args) {
        if (args.length == 1 && args[0].length() > 1) {
            String getName = "get" + args[0].substring(0,1).toUpperCase() + args[0].substring(1);
            try {
                Object value = IslandInfo.class.getMethod(getName).invoke(islandInfo);
                sendTr(sender, "Current value for <field> is '<value>'.",
                    unparsed("field", args[0], PRIMARY),
                    unparsed("value", String.valueOf(value), PRIMARY));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                sendErrorTr(sender, "Unable to get state for <field>", unparsed("field", args[0]));
            }
        } else {
            sendTr(sender, "Valid fields: <fields>", unparsed("fields", String.join(", ", getterNames), PRIMARY));
        }
    }

    @Override
    public TabCompleter getTabCompleter() {
        return tabCompleter;
    }

    private static class ReflectionTabCompleter extends AbstractTabCompleter {
        private final List<String> getterNames;

        public ReflectionTabCompleter(List<String> getterNames) {
            this.getterNames = getterNames;
        }

        @Override
        protected List<String> getTabList(CommandSender commandSender, String term) {
            return getterNames;
        }
    }
}
