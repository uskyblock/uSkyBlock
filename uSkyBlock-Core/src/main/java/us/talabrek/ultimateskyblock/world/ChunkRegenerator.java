package us.talabrek.ultimateskyblock.world;

import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Class responsible for regenerating chunks.
 */
public class ChunkRegenerator {
    private final uSkyBlock plugin;
    private final ChunkGenerator chunkGen;
    private final World world;
    private BukkitTask task;

    ChunkRegenerator(@NotNull World world) {
        Validate.notNull(world, "World cannot be null");

        this.plugin = uSkyBlock.getInstance();
        this.world = world;
        this.chunkGen = plugin.getDefaultWorldGenerator(world.getName(), "");
    }

    /**
     * Regenerates the given list of {@link Chunk}s at the configured chunks/tick speed (default: 4).
     * @param chunkList List of chunks to regenerate.
     * @param onCompletion Runnable to schedule on completion, or null to call no runnable.
     */
    public void regenerateChunks(@NotNull List<Chunk> chunkList, @Nullable Runnable onCompletion) {
        Validate.notNull(chunkList, "ChunkList cannot be empty");

        final int CHUNKS_PER_TICK = plugin.getConfig().getInt("options.advanced.chunkRegenSpeed", 4);
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        task = scheduler.runTaskTimer(plugin, () -> {
            for (int i = 0; i <= CHUNKS_PER_TICK; i++) {
                if (!chunkList.isEmpty()) {
                    Chunk chunk = chunkList.removeFirst();
                    regenerateChunk(chunk);
                } else {
                    if (onCompletion != null) {
                        scheduler.runTaskLater(plugin, onCompletion, 1L);
                    }
                    task.cancel();
                    break;
                }
            }
        }, 0L, 1L);
    }

    /**
     * Regenerates the given {@link Chunk}, removing all it's entities except players and setting the default biome.
     * @param chunk Chunk to regenerate.
     */
    public void regenerateChunk(@NotNull Chunk chunk) {
        Validate.notNull(chunk, "Chunk cannot be null");

        spawnTeleportPlayers(chunk);

        Random random = new Random(world.getSeed() + (long) chunk.getX() * (long) chunk.getZ());

        World world = chunk.getWorld();

        ChunkData chunkData = Bukkit.createChunkData(world);
        chunkGen.generateNoise(world, random, chunk.getX(), chunk.getZ(), chunkData);
        chunkGen.generateSurface(world, random, chunk.getX(), chunk.getZ(), chunkData);
        chunkGen.generateBedrock(world, random, chunk.getX(), chunk.getZ(), chunkData);
        chunkGen.generateCaves(world, random, chunk.getX(), chunk.getZ(), chunkData);

        BiomeProvider biomeProvider = chunkGen.getDefaultBiomeProvider(world);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    if (biomeProvider != null) {
                        chunk.getBlock(x, y, z).setBiome(biomeProvider.getBiome(world, x, y, z));
                    }
                    chunk.getBlock(x, y, z).setBlockData(chunkData.getBlockData(x, y, z));
                }
            }
        }

        removeEntities(chunk);
    }

    /**
     * Removes all the entities within the given {@link Chunk}, except for {@link Player}s.
     * @param chunk Chunk to remove entities in.
     */
    private void removeEntities(@NotNull Chunk chunk) {
        Arrays.stream(chunk.getEntities())
                .filter(entity -> !(entity instanceof Player))
                .forEach(Entity::remove);
    }

    /**
     * Teleport all the {@link Player}s within the given {@link Chunk} to spawn.
     * @param chunk Chunk to spawnteleport players in.
     */
    private void spawnTeleportPlayers(@NotNull Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) {
                uSkyBlock.getInstance().getTeleportLogic().spawnTeleport((Player) entity, true);
            }
        }
    }
}
