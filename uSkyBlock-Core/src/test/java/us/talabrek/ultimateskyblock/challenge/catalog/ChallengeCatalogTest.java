package us.talabrek.ultimateskyblock.challenge.catalog;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.CompletedChallengesRequirement;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChallengeCatalogTest {
    @Test
    void preservesRankAndChallengeOrderAndBuildsIndexes() {
        RankDefinition early = new RankDefinition(
            RankId.of("Tier1"),
            rankDisplay("Novice"),
            item(Material.BARRIER),
            List.of(),
            List.of(challenge("alpha"), challenge("beta"))
        );
        RankDefinition late = new RankDefinition(
            RankId.of("Tier2"),
            rankDisplay("Adept"),
            item(Material.OBSIDIAN),
            List.of(new CompletedChallengesRequirement(List.of(ChallengeId.of("alpha")))),
            List.of(challenge("gamma"))
        );

        ChallengeCatalog catalog = new ChallengeCatalog(List.of(early, late));

        assertEquals(List.of(early, late), catalog.ranks());
        assertEquals(early, catalog.rank(RankId.of("Tier1")).orElseThrow());
        assertEquals(late, catalog.rank(RankId.of("Tier2")).orElseThrow());
        assertEquals("alpha", catalog.ranks().get(0).challenges().get(0).id().value());
        assertEquals("beta", catalog.ranks().get(0).challenges().get(1).id().value());
        assertEquals("Tier2", catalog.index().challengeOwners().get(ChallengeId.of("gamma")).value());
    }

    @Test
    void rejectsDuplicateChallengeIds() {
        RankDefinition rank = new RankDefinition(
            RankId.of("Tier1"),
            rankDisplay("Novice"),
            item(Material.BARRIER),
            List.of(),
            List.of(challenge("duplicate"), challenge("duplicate"))
        );

        assertThrows(IllegalArgumentException.class, () -> new ChallengeCatalog(List.of(rank)));
    }

    private static ChallengeDefinition challenge(String id) {
        return new ChallengeDefinition(
            ChallengeId.of(id),
            challengeDisplay(id),
            List.of(),
            List.of(),
            new ChallengeProperties(true),
            new RepeatPolicy(false, Duration.ZERO, 0),
            RewardBundle.empty(),
            RewardBundle.empty()
        );
    }

    private static RankDisplaySpec rankDisplay(String name) {
        return new RankDisplaySpec(
            TextSpec.miniMessage(name),
            TextSpec.empty()
        );
    }

    private static DisplaySpec challengeDisplay(String name) {
        return new DisplaySpec(
            TextSpec.miniMessage(name),
            TextSpec.empty(),
            item(Material.STONE)
        );
    }

    private static ItemStackSpec item(Material material) {
        return new ItemStackSpec(new org.bukkit.inventory.ItemStack(material));
    }
}
