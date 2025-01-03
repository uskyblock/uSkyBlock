package us.talabrek.ultimateskyblock.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.Settings;

import java.util.List;
import java.util.Random;

public class SkyBlockNetherChunkGenerator extends ChunkGenerator {

    // The nether height limits intentionally differ from World.getMinHeight() and World.getMaxHeight() to reflect
    // vanilla nether limits.
    private static final int MIN_HEIGHT = 0;
    private static final int MAX_HEIGHT = 128;

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Generate lava sea
        chunkData.setRegion(0, MIN_HEIGHT, 0, 16, Settings.nether_lava_level + 1, 16, Material.LAVA);

        // Generate netherrack ceiling

        // First layer with holes
        int y = MAX_HEIGHT - 8;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (random.nextDouble() >= 0.20) { // 20% air
                    chunkData.setBlock(x, y, z, Material.NETHERRACK);
                }
            }
        }

        // Solid block above
        chunkData.setRegion(0, MAX_HEIGHT - 7, 0, 16, MAX_HEIGHT, 16, Material.NETHERRACK);
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Solid bedrock floor
        chunkData.setRegion(0, MIN_HEIGHT, 0, 16, MIN_HEIGHT + 1, 16, Material.BEDROCK);

        // Bedrock floor with holes in it
        for (int yOffset = 1; yOffset < 6; yOffset++) {
            double yThreshold = 0.10 * yOffset; // 90% - 50% bedrock
            int y = MIN_HEIGHT + yOffset;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (random.nextDouble() >= yThreshold) {
                        chunkData.setBlock(x, y, z, Material.BEDROCK);
                    }
                }
            }
        }

        // Bedrock ceiling with holes in it
        for (int yOffset = 1; yOffset < 6; yOffset++) {
            double yThreshold = 0.20 * (yOffset); // 20% - 100% bedrock
            int y = MAX_HEIGHT - yOffset - 1;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (random.nextDouble() >= yThreshold) {
                        chunkData.setBlock(x, y, z, Material.BEDROCK);
                    }
                }
            }
        }

        // Solid bedrock ceiling
        chunkData.setRegion(0, MAX_HEIGHT - 1, 0, 16, MAX_HEIGHT, 16, Material.BEDROCK);
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of();
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new SingleBiomeProvider(Settings.general_defaultNetherBiome);
    }

    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0, Settings.nether_height, 0);
    }
}
