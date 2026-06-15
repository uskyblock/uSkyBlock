package us.talabrek.ultimateskyblock.biome;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import us.talabrek.ultimateskyblock.challenge.IslandBiomeUnlocks;
import us.talabrek.ultimateskyblock.gui.GuiManager;
import us.talabrek.ultimateskyblock.island.IslandInfo;

import java.util.List;
import java.util.Set;

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
        // One unlock-set computation for the whole GUI; a per-biome check would re-read
        // island progress for every configured biome.
        Set<String> unlockedBiomes = biomeUnlocks.unlockedBiomes(islandInfo);
        List<BiomeEntry> availableBiomes = biomeConfig.getConfiguredBiomeEntries().stream()
            .filter(entry -> {
                String biomeKey = entry.biome().getKey().getKey();
                return player.hasPermission("usb.biome." + biomeKey) || unlockedBiomes.contains(biomeKey);
            })
            .toList();
        BiomeGui biomeGui = new BiomeGui(availableBiomes, islandInfo.getIslandBiome());
        guiManager.openGui(biomeGui, player);
    }
}
