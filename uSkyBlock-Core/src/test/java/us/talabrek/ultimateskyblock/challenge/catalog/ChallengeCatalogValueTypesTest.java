package us.talabrek.ultimateskyblock.challenge.catalog;

import dk.lockfuglsang.minecraft.util.ItemRequirement;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.BlockRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.EntityRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemAmountProgression;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemRequirementSpec;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChallengeCatalogValueTypesTest {
    @Test
    void itemRequirementSpecComputesRepeatedAmounts() {
        ItemRequirementSpec requirement = new ItemRequirementSpec(
            new ItemStackSpec(new org.bukkit.inventory.ItemStack(Material.COBBLESTONE)),
            64,
            new ItemAmountProgression(ItemRequirement.Operator.ADD, 16)
        );

        assertEquals(64, requirement.amountForRepetitions(0));
        assertEquals(80, requirement.amountForRepetitions(1));
        assertEquals(96, requirement.amountForRepetitions(2));
    }

    @Test
    void blockRequirementSpecMatchesByMaterial() {
        BlockData prototype = mock(BlockData.class);
        when(prototype.getMaterial()).thenReturn(Material.OAK_LOG);
        BlockRequirementSpec spec = new BlockRequirementSpec(prototype, 12);

        assertEquals(12, spec.amount());
        assertEquals(Material.OAK_LOG, ((ChallengeRequirements.ExactBlock) spec.matcher()).material());
        assertTrue(spec.matches(Material.OAK_LOG));
        assertFalse(spec.matches(Material.STONE));
    }

    @Test
    void anyOfMatchersAcceptAnyMember() {
        ChallengeRequirements.AnyOfBlocks blocks = new ChallengeRequirements.AnyOfBlocks(List.of(
            new ChallengeRequirements.ExactBlock(Material.RED_BED),
            new ChallengeRequirements.ExactBlock(Material.BLUE_BED)
        ));
        assertTrue(blocks.matches(Material.RED_BED));
        assertTrue(blocks.matches(Material.BLUE_BED));
        assertFalse(blocks.matches(Material.GREEN_BED));

        ItemRequirementSpec items = new ItemRequirementSpec(
            new ChallengeRequirements.AnyOfItems(List.of(
                new ChallengeRequirements.ExactItem(new ItemStackSpec(new org.bukkit.inventory.ItemStack(Material.OAK_DOOR))),
                new ChallengeRequirements.ExactItem(new ItemStackSpec(new org.bukkit.inventory.ItemStack(Material.SPRUCE_DOOR)))
            )), 1, ItemAmountProgression.none());
        assertTrue(items.matches(new org.bukkit.inventory.ItemStack(Material.OAK_DOOR)));
        assertTrue(items.matches(new org.bukkit.inventory.ItemStack(Material.SPRUCE_DOOR)));
        assertFalse(items.matches(new org.bukkit.inventory.ItemStack(Material.IRON_DOOR)));

        assertThrows(IllegalArgumentException.class, () -> new ChallengeRequirements.AnyOfBlocks(List.of()));
    }

    @Test
    void entityRequirementSpecDefensivelyCopiesMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("Color", "RED");

        EntityRequirementSpec spec = new EntityRequirementSpec(org.bukkit.entity.EntityType.SHEEP, metadata, 3);
        metadata.put("Color", "BLUE");

        assertEquals(Map.of("Color", "RED"), spec.metadata());
        assertThrows(UnsupportedOperationException.class, () -> spec.metadata().put("Color", "GREEN"));
    }

    @Test
    void collectionsAreStoredImmutably() {
        InventoryItemsRequirement requirement = new InventoryItemsRequirement(List.of(
            new ItemRequirementSpec(
                new ItemStackSpec(new org.bukkit.inventory.ItemStack(Material.STONE)),
                4,
                ItemAmountProgression.none()
            )
        ));

        assertThrows(UnsupportedOperationException.class, () -> requirement.items().add(null));
    }
}
