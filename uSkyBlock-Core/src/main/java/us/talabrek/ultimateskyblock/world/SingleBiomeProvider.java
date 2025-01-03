package us.talabrek.ultimateskyblock.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SingleBiomeProvider extends BiomeProvider {
    private final Biome biome;

    public SingleBiomeProvider(Biome biome) {
        this.biome = biome;
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return List.of(biome);
    }
}
