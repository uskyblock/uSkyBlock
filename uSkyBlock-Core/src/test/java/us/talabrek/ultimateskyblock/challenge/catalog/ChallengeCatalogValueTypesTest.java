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
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    void blockRequirementSpecDefensivelyClonesBlockData() {
        BlockData original = mock(BlockData.class);
        BlockData storedClone = mock(BlockData.class);
        BlockData returnedClone = mock(BlockData.class);
        when(original.clone()).thenReturn(storedClone);
        when(storedClone.clone()).thenReturn(returnedClone);
        when(returnedClone.clone()).thenReturn(mock(BlockData.class));
        BlockRequirementSpec spec = new BlockRequirementSpec(original, 12);

        var first = spec.prototype();
        var second = spec.prototype();

        assertNotSame(original, first);
        assertNotSame(original, second);
        assertEquals(12, spec.amount());
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
