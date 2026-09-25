package us.talabrek.ultimateskyblock.island.level;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginDataDir;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Loads the block types the variety island level ignores. The bundled {@code levelBlacklist.yml}
 * is used unless {@code plugins/uSkyBlock/config/levelBlacklist.yml} exists, which then replaces the
 * bundled list. An override that cannot be read or has no {@code ignore} list is reported and the
 * bundled list is used instead, so a typo never silently makes water and lava count. Names unknown
 * to the running server version are skipped with a warning.
 * <p>
 * The {@code config/} subdirectory is where plugin configuration files live from 4.0 on; files still
 * loaded from the plugin folder root are legacy locations.
 */
@Singleton
public class LevelBlacklistLoader {
    static final String CONFIG_DIRECTORY = "config";
    static final String LEVEL_BLACKLIST_NAME = "levelBlacklist.yml";
    private static final String IGNORE_KEY = "ignore";

    private final Path pluginDataDir;
    private final Logger logger;

    @Inject
    public LevelBlacklistLoader(@NotNull @PluginDataDir Path pluginDataDir, @NotNull @PluginLog Logger logger) {
        this.pluginDataDir = pluginDataDir;
        this.logger = logger;
    }

    public @NotNull Set<Material> load() {
        List<String> names = loadNames();
        EnumSet<Material> ignored = EnumSet.noneOf(Material.class);
        List<String> unknown = new ArrayList<>();
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                unknown.add(name);
            } else {
                ignored.add(material);
            }
        }
        if (!unknown.isEmpty()) {
            logger.warning(LEVEL_BLACKLIST_NAME + ": skipping block names unknown to this server version: "
                + String.join(", ", unknown));
        }
        return Collections.unmodifiableSet(ignored);
    }

    private @NotNull List<String> loadNames() {
        Path blacklistPath = pluginDataDir.resolve(CONFIG_DIRECTORY).resolve(LEVEL_BLACKLIST_NAME);
        if (Files.exists(blacklistPath)) {
            YamlConfiguration override = new YamlConfiguration();
            try {
                override.load(blacklistPath.toFile());
                if (override.isList(IGNORE_KEY)) {
                    logger.info("Loading " + LEVEL_BLACKLIST_NAME + " from " + blacklistPath + ".");
                    return override.getStringList(IGNORE_KEY);
                }
                logger.warning(blacklistPath + " has no '" + IGNORE_KEY + "' list; using the bundled "
                    + LEVEL_BLACKLIST_NAME + " instead.");
            } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
                logger.warning("Could not read " + blacklistPath + " (" + e.getMessage() + "); using the bundled "
                    + LEVEL_BLACKLIST_NAME + " instead.");
            }
        }
        return loadBundled().getStringList(IGNORE_KEY);
    }

    private @NotNull YamlConfiguration loadBundled() {
        try (var stream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(LEVEL_BLACKLIST_NAME),
            "Missing bundled resource " + LEVEL_BLACKLIST_NAME);
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + LEVEL_BLACKLIST_NAME, e);
        }
    }
}
