package us.talabrek.ultimateskyblock.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.AsyncWorldEditHandler;
import us.talabrek.ultimateskyblock.handler.SchematicHandler;
import us.talabrek.ultimateskyblock.player.Perk;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The factory for creating islands (actual blocks).
 */
@Singleton
public class IslandGenerator {
    private final Logger logger;
    private final SchematicHandler schematicHandler;
    private final RuntimeConfigs runtimeConfigs;

    @Inject
    public IslandGenerator(
        @NotNull Logger logger,
        @NotNull SchematicHandler schematicHandler,
        @NotNull RuntimeConfigs runtimeConfigs
    ) {
        this.logger = logger;
        this.schematicHandler = schematicHandler;
        this.runtimeConfigs = runtimeConfigs;
    }

    /**
     * Generate an island at the given {@link Location}.
     *
     * @param next   Location to generate an island.
     * @param cSchem New island schematic.
     * @return True if the island was generated, false otherwise.
     */
    public boolean createIsland(@NotNull Location next, @Nullable String cSchem) {
        // Hacky, but clear the Orphan info
        next.setYaw(0);
        next.setPitch(0);
        next.setY(runtimeConfigs.current().island().height());
        SchematicHandler.SchematicPair pair = schematicHandler.getScheme(cSchem);
        if (pair == null) {
            logger.warning("No schematic configured for '" + (cSchem != null ? cSchem : "default") + "'");
            return false;
        }

        Path overworldPath = pair.overworld();
        if (!Files.isRegularFile(overworldPath)) {
            logger.warning("Missing overworld schematic for '" + (cSchem != null ? cSchem : "default") + "' at " + overworldPath);
            return false;
        }

        Optional<Path> netherPath = pair.nether();

        AsyncWorldEditHandler.loadIslandSchematic(overworldPath.toFile(), next);
        World skyBlockNetherWorld = uSkyBlock.getInstance().getWorldManager().getNetherWorld();
        if (skyBlockNetherWorld != null && netherPath.isPresent() && Files.isRegularFile(netherPath.get())) {
            Location netherHome = new Location(skyBlockNetherWorld, next.getBlockX(), runtimeConfigs.current().nether().height(), next.getBlockZ());
            AsyncWorldEditHandler.loadIslandSchematic(netherPath.get().toFile(), netherHome);
        }
        return true;
    }

    /**
     * Find the nearest chest at the given {@link Location} and fill the chest with the starter and {@link Perk}
     * based items.
     *
     * @param location Location to search for a chest.
     * @param perk     Perk containing extra perk-based items to add.
     * @return True if the chest is found and filled, false otherwise.
     */
    public boolean findAndSetChest(@NotNull Location location, @NotNull Perk perk) {
        Location chestLocation = LocationUtil.findChestLocation(location);
        return setChest(chestLocation, perk);
    }

    /**
     * Fill the {@link Inventory} of the given chest {@link Location} with the starter and {@link Perk} based items.
     *
     * @param chestLocation Location of the chest block.
     * @param perk          Perk containing extra perk-based items to add.
     * @return True if the chest is found and filled, false otherwise.
     */
    public boolean setChest(@Nullable Location chestLocation, @NotNull Perk perk) {
        if (chestLocation == null || chestLocation.getWorld() == null) {
            return false;
        }

        final Block block = chestLocation.getWorld().getBlockAt(chestLocation);
        if (block.getType() == Material.CHEST) {
            final Chest chest = (Chest) block.getState();
            final Inventory inventory = chest.getInventory();
            inventory.addItem(ItemStackUtil.createItemArray(ItemStackUtil.createItemList(runtimeConfigs.current().island().chestItemSpecs())));
            if (runtimeConfigs.current().island().addExtraItems()) {
                inventory.addItem(ItemStackUtil.createItemArray(perk.getExtraItems()));
            }
            return true;
        }
        return false;
    }

}
