package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public final class ConfigMigrationV113 implements ConfigMigration {
    @Override
    public int fromVersion() {
        return 112;
    }

    @Override
    public int toVersion() {
        return 113;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config) {
        ConfigMigrationSupport.migrateSecondsToDuration(config, "options.general.cooldownRestart");
        ConfigMigrationSupport.migrateSecondsToDuration(config, "options.general.biomeChange");
        ConfigMigrationSupport.migrateSecondsToDuration(config, "options.island.islandTeleportDelay");
        ConfigMigrationSupport.migrateMinutesToDuration(config, "options.island.topTenTimeout");
        ConfigMigrationSupport.migrateInviteTimeoutToDuration(config, "options.party.invite-timeout");
        ConfigMigrationSupport.migrateSecondsToDuration(config, "options.advanced.confirmTimeout");
        ConfigMigrationSupport.migrateMillisToDuration(config, "options.restart.teleportDelay");

        ConfigMigrationSupport.setComment(config, "options.general.cooldownRestart",
            "# [duration] The time before a player can use the /island restart command again. Use ms, s, m, h, or d.");
        ConfigMigrationSupport.setComment(config, "options.general.biomeChange",
            "# [duration] The time before a player can use the /island biome command again. Use ms, s, m, h, or d.");
        ConfigMigrationSupport.setComment(config, "options.island.islandTeleportDelay",
            "# [duration] The delay before teleporting a player to their island. Use ms, s, m, h, or d.");
        ConfigMigrationSupport.setComment(config, "options.island.topTenTimeout",
            "# [duration] How long to cache top-ten data before recalculating it. Use ms, s, m, h, or d.");
        ConfigMigrationSupport.setComment(config, "options.party.invite-timeout",
            "# [duration] How long an island invite stays valid. Use ms, s, m, h, or d.");
        ConfigMigrationSupport.setComment(config, "options.advanced.confirmTimeout",
            "# [duration] The time to wait for repeating a risky command. Use ms, s, m, h, or d.");
        ConfigMigrationSupport.setComment(config, "options.restart.teleportDelay",
            "# [duration] The time to wait before porting the player back on /is restart or /is create. Use ms, s, m, h, or d.");
    }
}
