package us.talabrek.ultimateskyblock.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;

import java.util.List;
import java.util.Random;

/**
 * A chunk generator that generates an empty world with a single biome.
 */
public class SkyBlockChunkGenerator extends ChunkGenerator {

    @NotNull
    public List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of();
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new SingleBiomeProvider(Settings.general_defaultBiome);
    }

    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5d, Settings.island_height, 0.5d);
    }
}
