package us.talabrek.ultimateskyblock.config.migration;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigMigrations {
    private final Map<Integer, ConfigMigration> migrationsByVersion;

    private ConfigMigrations(@NotNull List<ConfigMigration> migrations) {
        this.migrationsByVersion = new LinkedHashMap<>();
        for (ConfigMigration migration : migrations) {
            ConfigMigration previous = migrationsByVersion.put(migration.fromVersion(), migration);
            if (previous != null) {
                throw new IllegalStateException("Duplicate config migration registered for version " + migration.fromVersion() + ".");
            }
        }
    }

    @NotNull
    public static ConfigMigrations defaults() {
        return new ConfigMigrations(List.of(
            new ConfigMigrationV112(),
            new ConfigMigrationV113(),
            new ConfigMigrationV114(),
            new ConfigMigrationV115(),
            new ConfigMigrationV116(),
            new ConfigMigrationV117(),
            new ConfigMigrationV118(),
            new ConfigMigrationV119()
        ));
    }

    @NotNull
    public ConfigMigration find(int fromVersion) {
        ConfigMigration migration = migrationsByVersion.get(fromVersion);
        if (migration == null) {
            throw new IllegalStateException(
                "config.yml version " + fromVersion + " requires explicit migrations, but no step is registered.");
        }
        return migration;
    }
}
