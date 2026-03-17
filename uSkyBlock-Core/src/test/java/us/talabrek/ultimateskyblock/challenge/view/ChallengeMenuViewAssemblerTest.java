package us.talabrek.ultimateskyblock.challenge.view;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeProperties;
import us.talabrek.ultimateskyblock.challenge.catalog.DisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeMenuViewAssemblerTest {
    private final ChallengeMenuViewAssembler assembler = new ChallengeMenuViewAssembler();

    @Test
    void lockedRankUsesRankLockedIconForAllSlots() {
        ChallengeCatalog catalog = new ChallengeCatalog(List.of(
            rank("starter", Material.BARRIER, List.of(
                challenge("alpha", Material.STONE, Material.OBSIDIAN),
                challenge("beta", Material.COBBLESTONE, Material.GOLD_BLOCK)
            ))
        ));

        ChallengePageView page = assembler.assemblePage(
            catalog,
            new ChallengePresentationSnapshot(Set.of(), Set.of()),
            1
        );

        ChallengeRankRowView row = page.rows().getFirst();
        assertFalse(row.rankUnlocked());
        assertEquals(2, row.slots().size());
        assertTrue(row.slots().stream().allMatch(slot -> slot.state() == ChallengeSlotState.RANK_LOCKED));
        assertTrue(row.slots().stream().allMatch(slot -> slot.detailMode() == ChallengeSlotDetailMode.RANK_UNLOCK_DETAILS));
        assertTrue(row.slots().stream().allMatch(slot -> slot.icon().create().getType() == Material.BARRIER));
        assertTrue(row.slots().stream().noneMatch(ChallengeSlotView::clickable));
        assertFalse(row.slots().getFirst().lore().isEmpty());
    }

    @Test
    void lockedChallengeUsesResolvedChallengeLockedIcon() {
        ChallengeCatalog catalog = new ChallengeCatalog(List.of(
            rank("starter", Material.BARRIER, List.of(
                challenge("alpha", Material.STONE, Material.OBSIDIAN)
            ))
        ));

        ChallengePageView page = assembler.assemblePage(
            catalog,
            new ChallengePresentationSnapshot(Set.of(RankId.of("starter")), Set.of()),
            1
        );

        ChallengeSlotView slot = page.rows().getFirst().slots().getFirst();
        assertEquals(ChallengeSlotState.CHALLENGE_LOCKED, slot.state());
        assertEquals(ChallengeSlotDetailMode.CHALLENGE_UNLOCK_DETAILS, slot.detailMode());
        assertEquals(Material.OBSIDIAN, slot.icon().create().getType());
        assertFalse(slot.clickable());
        assertFalse(slot.lore().isEmpty());
    }

    @Test
    void unlockedChallengeUsesNormalDisplayItemAndIsClickable() {
        ChallengeCatalog catalog = new ChallengeCatalog(List.of(
            rank("starter", Material.BARRIER, List.of(
                challenge("alpha", Material.STONE, Material.OBSIDIAN)
            ))
        ));

        ChallengePageView page = assembler.assemblePage(
            catalog,
            new ChallengePresentationSnapshot(Set.of(RankId.of("starter")), Set.of(ChallengeId.of("alpha"))),
            1
        );

        ChallengeSlotView slot = page.rows().getFirst().slots().getFirst();
        assertEquals(ChallengeSlotState.CHALLENGE_UNLOCKED, slot.state());
        assertEquals(ChallengeSlotDetailMode.CHALLENGE_DETAILS, slot.detailMode());
        assertEquals(Material.STONE, slot.icon().create().getType());
        assertTrue(slot.clickable());
        assertFalse(slot.lore().isEmpty());
    }

    @Test
    void pageNumberMustBeInRange() {
        ChallengeCatalog catalog = new ChallengeCatalog(List.of(
            rank("starter", Material.BARRIER, List.of(challenge("alpha", Material.STONE, Material.OBSIDIAN)))
        ));

        assertThrows(IllegalArgumentException.class, () -> assembler.assemblePage(catalog, ChallengePresentationState.allUnlocked(), 0));
        assertThrows(IllegalArgumentException.class, () -> assembler.assemblePage(catalog, ChallengePresentationState.allUnlocked(), 2));
    }

    @Test
    void packsUpToFiveRanksIntoOnePage() {
        ChallengeCatalog catalog = new ChallengeCatalog(List.of(
            rank("r1", Material.BARRIER, List.of(challenge("c1", Material.STONE, Material.OBSIDIAN))),
            rank("r2", Material.BARRIER, List.of(challenge("c2", Material.STONE, Material.OBSIDIAN))),
            rank("r3", Material.BARRIER, List.of(challenge("c3", Material.STONE, Material.OBSIDIAN))),
            rank("r4", Material.BARRIER, List.of(challenge("c4", Material.STONE, Material.OBSIDIAN))),
            rank("r5", Material.BARRIER, List.of(challenge("c5", Material.STONE, Material.OBSIDIAN))),
            rank("r6", Material.BARRIER, List.of(challenge("c6", Material.STONE, Material.OBSIDIAN)))
        ));

        ChallengePageView firstPage = assembler.assemblePage(catalog, ChallengePresentationState.allUnlocked(), 1);
        ChallengePageView secondPage = assembler.assemblePage(catalog, ChallengePresentationState.allUnlocked(), 2);

        assertEquals(2, firstPage.totalPages());
        assertEquals(5, firstPage.rows().size());
        assertEquals("r1", firstPage.rows().get(0).rank().id().value());
        assertEquals("r5", firstPage.rows().get(4).rank().id().value());
        assertEquals(1, secondPage.rows().size());
        assertEquals("r6", secondPage.rows().getFirst().rank().id().value());
        assertEquals(0, firstPage.rows().get(0).slots().getFirst().slotIndex());
        assertEquals(36, firstPage.rows().get(4).slots().getFirst().slotIndex());
    }

    private static RankDefinition rank(String id, Material lockedItem, List<ChallengeDefinition> challenges) {
        return new RankDefinition(
            RankId.of(id),
            new RankDisplaySpec(TextSpec.miniMessage(id), TextSpec.empty()),
            item(lockedItem),
            List.of(),
            challenges
        );
    }

    private static ChallengeDefinition challenge(String id, Material displayItem, Material lockedDisplayItem) {
        return new ChallengeDefinition(
            ChallengeId.of(id),
            new DisplaySpec(TextSpec.miniMessage(id), TextSpec.empty(), item(displayItem)),
            item(lockedDisplayItem),
            List.of(),
            List.of(),
            new ChallengeProperties(true),
            new RepeatPolicy(false, Duration.ZERO, 0),
            RewardBundle.empty(),
            RewardBundle.empty()
        );
    }

    private static ItemStackSpec item(Material material) {
        return new ItemStackSpec(new org.bukkit.inventory.ItemStack(material));
    }
}
