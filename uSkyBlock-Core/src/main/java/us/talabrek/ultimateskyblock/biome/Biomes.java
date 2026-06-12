package us.talabrek.ultimateskyblock.biome;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.challenge.IslandBiomeUnlocks;
import us.talabrek.ultimateskyblock.gui.GuiManager;
import us.talabrek.ultimateskyblock.island.IslandInfo;

import java.util.List;

public class Biomes {

    private final GuiManager guiManager;
    private final BiomeConfig biomeConfig;
    private final IslandBiomeUnlocks biomeUnlocks;

    @Inject
    public Biomes(GuiManager guiManager, BiomeConfig biomeConfig, IslandBiomeUnlocks biomeUnlocks) {
        this.guiManager = guiManager;
        this.biomeConfig = biomeConfig;
        this.biomeUnlocks = biomeUnlocks;
    }

    public void openBiomeGui(Player player, IslandInfo islandInfo) {
        List<BiomeEntry> availableBiomes = biomeConfig.getConfiguredBiomeEntries().stream()
            .filter(entry -> biomeUnlocks.canUseBiome(player, islandInfo, entry.biome().getKey().getKey()))
            .toList();
        BiomeGui biomeGui = new BiomeGui(availableBiomes, islandInfo.getIslandBiome());
        guiManager.openGui(biomeGui, player);
    }
}
