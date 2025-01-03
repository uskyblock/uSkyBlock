package us.talabrek.ultimateskyblock.command.completion;

import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import us.talabrek.ultimateskyblock.command.island.BiomeCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * TabCompleter for Biomes.
 */
public class BiomeTabCompleter extends AbstractTabCompleter {

    @Override
    protected List<String> getTabList(CommandSender commandSender, String term) {
        return filter(new ArrayList<>(BiomeCommand.AVAILABLE_BIOMES), term);
    }
}
