package us.talabrek.ultimateskyblock.command.completion;

import com.google.inject.Inject;
import dk.lockfuglsang.minecraft.command.completion.AbstractTabCompleter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.biome.BiomeConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * TabCompleter for Biomes.
 */
public class BiomeTabCompleter extends AbstractTabCompleter {

    private final BiomeConfig biomeConfig;

    @Inject
    public BiomeTabCompleter(@NotNull BiomeConfig biomeConfig) {
        this.biomeConfig = biomeConfig;
    }

    @Override
    protected List<String> getTabList(CommandSender commandSender, String term) {
        return filter(new ArrayList<>(biomeConfig.getConfiguredBiomeKeys()), term);
    }
}
