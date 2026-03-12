package us.talabrek.ultimateskyblock.event;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuardianHabitatPolicyTest {
    private final GuardianHabitatPolicy policy = new GuardianHabitatPolicy(10, 0.10d);

    @Test
    public void isEnabled_requiresPositiveCapAndChance() {
        assertFalse(new GuardianHabitatPolicy(0, 0.10d).isEnabled());
        assertFalse(new GuardianHabitatPolicy(10, 0.0d).isEnabled());
        assertTrue(policy.isEnabled());
    }

    @Test
    public void shouldSpawnGuardian_requiresAvailableGuardianCap() {
        assertFalse(policy.shouldSpawnGuardian(10, true, 0.0d));
    }

    @Test
    public void shouldSpawnGuardian_requiresGeneralMonsterCapacityWhenEnabled() {
        assertFalse(policy.shouldSpawnGuardian(0, false, 0.0d));
    }

    @Test
    public void shouldSpawnGuardian_requiresSuccessfulChanceRoll() {
        assertFalse(policy.shouldSpawnGuardian(0, true, 0.10d));
    }

    @Test
    public void shouldSpawnGuardian_allowsSpawnWhenBelowCapAndChanceSucceeds() {
        assertTrue(policy.shouldSpawnGuardian(3, true, 0.09d));
    }
}
