package us.talabrek.ultimateskyblock.command.completion;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SchematicTabCompleter extends AbstractTabCompleter {
    private final uSkyBlock plugin;

    @Inject
    public SchematicTabCompleter(uSkyBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    protected List<String> getTabList(CommandSender commandSender, String term) {
        if (commandSender instanceof Player) {
            return new ArrayList<>(plugin.getPerkLogic().getSchemes((Player) commandSender));
        }
        return plugin.getIslandGenerator().getSchemeNames();
    }
}
