package us.talabrek.ultimateskyblock.world;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Maintains the {@code worlds.<name>.generator} mapping in the server's bukkit.yml.
 * The plugin generator association is not stored in the world data, and the bukkit.yml
 * mapping is the only mechanism the server consults on every load path - including the
 * default (level-name) world, which loads before any plugin can attach a generator.
 * Only missing entries are added; existing values are never overwritten. Changes take
 * effect on the next server start.
 */
public class BukkitYmlGeneratorMapping {

    private static final String GENERATOR_NAME = "uSkyBlock";

    private final Path bukkitYml;
    private final Logger logger;

    public BukkitYmlGeneratorMapping(@NotNull Path bukkitYml, @NotNull Logger logger) {
        this.bukkitYml = bukkitYml;
        this.logger = logger;
    }

    /**
     * Ensures bukkit.yml maps the given world to the uSkyBlock generator. Adds the entry
     * when missing, leaves any existing generator untouched, and only logs on failure.
     *
     * @param worldName Name of the world to register the generator for.
     */
    public void ensureMapping(@NotNull String worldName) {
        if (!Files.exists(bukkitYml)) {
            logger.warning("No bukkit.yml found at " + bukkitYml.toAbsolutePath()
                + ", cannot register the world generator for '" + worldName + "'.");
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(bukkitYml.toFile());
        } catch (IOException | InvalidConfigurationException e) {
            logger.warning("Could not read bukkit.yml to register the generator for '"
                + worldName + "': " + e.getMessage());
            return;
        }
        String path = "worlds." + worldName + ".generator";
        String existing = config.getString(path);
        if (existing == null || existing.isEmpty()) {
            config.set(path, GENERATOR_NAME);
            try {
                config.save(bukkitYml.toFile());
                logger.info("Registered generator '" + GENERATOR_NAME + "' for world '" + worldName
                    + "' in bukkit.yml, so the world keeps its void generator on every load path.");
            } catch (IOException e) {
                logger.warning("Could not write bukkit.yml to register the generator for '"
                    + worldName + "': " + e.getMessage());
            }
        } else if (!existing.equals(GENERATOR_NAME)) {
            logger.warning("bukkit.yml maps world '" + worldName + "' to generator '" + existing
                + "' instead of '" + GENERATOR_NAME + "'. Leaving it unchanged - make sure it is a void generator.");
        }
    }
}
