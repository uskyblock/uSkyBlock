package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ConfigMigrationV115 implements ConfigMigration {
    private static final boolean GUARDIAN_ENABLED = true;
    private static final int GUARDIAN_MAX_PER_ISLAND = 10;
    private static final double GUARDIAN_SPAWN_CHANCE = 0.1d;

    @Override
    public int fromVersion() {
        return 114;
    }

    @Override
    public int toVersion() {
        return 115;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config) {
        if (!config.contains("options.spawning.guardians.enabled")) {
            config.set("options.spawning.guardians.enabled", GUARDIAN_ENABLED);
        }
        if (!config.contains("options.spawning.guardians.max-per-island")) {
            config.set("options.spawning.guardians.max-per-island", GUARDIAN_MAX_PER_ISLAND);
        }
        if (!config.contains("options.spawning.guardians.spawn-chance")) {
            config.set("options.spawning.guardians.spawn-chance", GUARDIAN_SPAWN_CHANCE);
        }

        config.setComments("options.spawning.guardians.enabled", List.of(
            "# [true/false] If true, deep-ocean prismarine habitats can replace water-mob spawns with guardians."));
        config.setComments("options.spawning.guardians.max-per-island", List.of(
            "# [integer] Maximum number of guardians allowed at one island guardian habitat. This safety cap always applies."));
        config.setComments("options.spawning.guardians.spawn-chance", List.of(
            "# [number] Chance from 0.0 to 1.0 that an eligible water-mob spawn is replaced with a guardian."));
    }
}
