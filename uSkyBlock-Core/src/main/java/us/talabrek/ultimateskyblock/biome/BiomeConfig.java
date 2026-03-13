package us.talabrek.ultimateskyblock.biome;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.file.FileUtil;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

@Singleton
public class BiomeConfig {
    private final Logger logger;
    private final GameObjectFactory gameObjects;
    private final List<BiomeEntry> configuredBiomeEntries;
    private final List<String> configuredBiomeKeys;

    @Inject
    public BiomeConfig(Logger logger, GameObjectFactory gameObjects) {
        this.logger = logger;
        this.gameObjects = gameObjects;
        this.configuredBiomeEntries = loadBiomes();
        this.configuredBiomeKeys = configuredBiomeEntries.stream()
            .map(entry -> entry.biome().getKey().getKey())
            .toList();
    }

    private @NotNull List<BiomeEntry> loadBiomes() {
        FileConfiguration biomeConfig = FileUtil.getYmlConfiguration("biomes.yml");
        ConfigurationSection biomeSection = biomeConfig.getConfigurationSection("biomes");
        if (biomeSection == null) {
            throw new IllegalStateException("You biomes.yml is corrupted, missing 'biomes' section.");
        }
        List<BiomeEntry> result = new ArrayList<>();
        for (String biomeKey : biomeSection.getKeys(false)) {
            ConfigurationSection section = requireNonNull(biomeSection.getConfigurationSection(biomeKey));
            BiomeEntry biomeEntry = readBiomeEntry(biomeKey, section);
            if (biomeEntry != null) {
                result.add(biomeEntry);
            }
        }
        return result;
    }

    public @NotNull List<BiomeEntry> getConfiguredBiomeEntries() {
        return configuredBiomeEntries;
    }

    public @NotNull List<BiomeEntry> getAvailableBiomes(@NotNull Player player) {
        return configuredBiomeEntries.stream()
            .filter(entry -> player.hasPermission("usb.biome." + entry.biome().getKey().getKey()))
            .toList();
    }

    public @NotNull List<String> getConfiguredBiomeKeys() {
        return configuredBiomeKeys;
    }

    private @Nullable BiomeEntry readBiomeEntry(@NotNull String biomeKey, @NotNull ConfigurationSection section) {
        String itemSpecification = section.getString("displayItem");
        String name = section.getString("name");
        String description = section.getString("description");

        Biome biome = Registry.BIOME.match(biomeKey);
        if (biome == null) {
            logger.warning("Unknown biome key: " + biomeKey);
            return null;
        }

        ItemStackSpec displayItem;
        try {
            displayItem = gameObjects.itemStack(itemSpecification);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid item specification for biome " + biomeKey + ": " + itemSpecification);
            displayItem = new ItemStackSpec(new org.bukkit.inventory.ItemStack(Material.GRASS_BLOCK));
        }

        if (name == null) {
            logger.warning("Missing name for biome " + biomeKey);
            name = biomeKey;
        }

        if (description == null) {
            logger.warning("Missing description for biome " + biomeKey);
            description = "";
        }

        return new BiomeEntry(displayItem, biome, name, description);
    }
}
