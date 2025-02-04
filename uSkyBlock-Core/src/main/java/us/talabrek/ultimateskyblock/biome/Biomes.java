package us.talabrek.ultimateskyblock.biome;

import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.gui.GuiManager;
import us.talabrek.ultimateskyblock.island.IslandInfo;

public class Biomes {

    private final GuiManager guiManager;
    private final BiomeConfig biomeConfig;

    public Biomes(GuiManager guiManager, BiomeConfig biomeConfig) {
        this.guiManager = guiManager;
        this.biomeConfig = biomeConfig;
    }

    public void openBiomeGui(Player player, IslandInfo islandInfo) {
        BiomeGui biomeGui = new BiomeGui(biomeConfig.getAvailableBiomes(player), islandInfo.getIslandBiome());
        guiManager.openGui(biomeGui, player);
    }
}
