package us.talabrek.ultimateskyblock.event;

final class GuardianHabitatPolicy {
    private final int maxPerIsland;
    private final double spawnChance;

    GuardianHabitatPolicy(int maxPerIsland, double spawnChance) {
        this.maxPerIsland = Math.max(0, maxPerIsland);
        this.spawnChance = Math.max(0.0d, Math.min(1.0d, spawnChance));
    }

    boolean isEnabled() {
        return maxPerIsland > 0 && spawnChance > 0.0d;
    }

    boolean shouldSpawnGuardian(int currentGuardians, boolean generalMonsterLimitAllowsSpawn, double randomRoll) {
        if (currentGuardians >= maxPerIsland) {
            return false;
        }
        if (!generalMonsterLimitAllowsSpawn) {
            return false;
        }
        return randomRoll < spawnChance;
    }
}
