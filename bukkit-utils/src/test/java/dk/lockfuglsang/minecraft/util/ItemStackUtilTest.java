package dk.lockfuglsang.minecraft.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static dk.lockfuglsang.minecraft.util.ItemStackMatcher.itemStack;
import static dk.lockfuglsang.minecraft.util.ItemStackMatcher.itemStacks;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ItemStackUtilTest extends BukkitServerMock {

    @BeforeClass
    public static void setUpClass() throws Exception {
        setupServerMock();
    }

    @Before
    public void setUp() {
        useMetaData = false;
        itemMetaMap.clear();
    }

    @Test
    public void createItemsWithProbability1() {
        List<ItemStackUtil.ItemProbability> actual = ItemStackUtil.createItemsWithProbability(List.of("{p=0.9}LAVA_BUCKET:1"));
        List<ItemStackUtil.ItemProbability> expected = List.of(new ItemStackUtil.ItemProbability(0.9, new ItemStack(Material.LAVA_BUCKET, 1)));
        assertThat(actual, notNullValue());
        assertThat(actual, is(expected));
    }

    @Test
    public void createItemsWithProbabilityN() {
        List<ItemStackUtil.ItemProbability> actual = ItemStackUtil.createItemsWithProbability(List.of("{p=0.9}LAVA_BUCKET:1", "{p=0.2}STONE:3", "{p=0.3}NETHER_BRICK_FENCE:2"));
        List<ItemStackUtil.ItemProbability> expected = List.of(new ItemStackUtil.ItemProbability(0.9, new ItemStack(Material.LAVA_BUCKET, 1)), new ItemStackUtil.ItemProbability(0.2, new ItemStack(Material.STONE, 3)), new ItemStackUtil.ItemProbability(0.3, new ItemStack(Material.NETHER_BRICK_FENCE, 2)));
        assertThat(actual, notNullValue());
        assertThat(actual, is(expected));
    }

    @Test
    public void createItemsWithProbabilityWithComponents() {
        useMetaData = true;
        List<ItemStackUtil.ItemProbability> actual = ItemStackUtil.createItemsWithProbability(List.of("{p=0.9}lava_bucket[some: value]:1", "{p=0.2}minecraft:stone[quoted: \"value\"]:3", "{p=0.3}NETHER_BRICK_FENCE[meta:{nested:{data:[{},{}]}}]:2"));

        assertThat(actual.get(0).item().getType(), is(Material.LAVA_BUCKET));
        assertThat(actual.get(0).item().getItemMeta().toString(), is("[some: value]"));
        assertThat(actual.get(0).probability(), is(0.9));

        assertThat(actual.get(1).item().getType(), is(Material.STONE));
        assertThat(actual.get(1).item().getItemMeta().toString(), is("[quoted: \"value\"]"));
        assertThat(actual.get(1).probability(), is(0.2));

        assertThat(actual.get(2).item().getType(), is(Material.NETHER_BRICK_FENCE));
        assertThat(actual.get(2).item().getItemMeta().toString(), is("[meta:{nested:{data:[{},{}]}}]"));
        assertThat(actual.get(2).probability(), is(0.3));
    }

    @Test
    public void createItemListInvalid() {
        try {
            ItemStackUtil.createItemList(List.of("DART"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Unknown item: 'DART'"));
        }
    }

    @Test
    public void createItemList() {
        List<ItemStack> actual = ItemStackUtil.createItemList(List.of("LAVA_BUCKET:1", "STONE:3", "NETHER_BRICK_FENCE:2"));
        List<ItemStack> expected = List.of(new ItemStack(Material.LAVA_BUCKET, 1), new ItemStack(Material.STONE, 3), new ItemStack(Material.NETHER_BRICK_FENCE, 2));
        assertThat(actual, itemStacks(expected));
    }

    @Test
    public void createItemListStringAndListWithComponents() {
        List<ItemStack> actual = ItemStackUtil.createItemList(List.of("diamond_sword[damage:200]:2", "white_banner[minecraft:banner_patterns=[{color: \"green\", pattern: \"minecraft:creeper\"}]]:256"));

        assertThat(actual.get(0).getType(), is(Material.DIAMOND_SWORD));
        assertThat(actual.get(1).getType(), is(Material.WHITE_BANNER));

        assertThat(actual.get(0).getItemMeta().toString(), is("[damage:200]"));
        assertThat(actual.get(1).getItemMeta().toString(), is("[minecraft:banner_patterns=[{color: \"green\", pattern: \"minecraft:creeper\"}]]"));

        assertThat(actual.get(0).getAmount(), is(2));
        assertThat(actual.get(1).getAmount(), is(256));
    }

    @Test
    public void createItemArrayNull() {
        ItemStack[] actual = ItemStackUtil.createItemArray(null);
        assertThat(actual, is(new ItemStack[0]));
    }

    @Test
    public void createItemArrayEmpty() {
        ItemStack[] actual = ItemStackUtil.createItemArray(List.of());
        assertThat(actual, is(new ItemStack[0]));
    }

    @Test
    public void createItemArray() {
        List<ItemStack> expected = List.of(new ItemStack(Material.LAVA_BUCKET, 1), new ItemStack(Material.STONE, 3), new ItemStack(Material.NETHER_BRICK_FENCE, 2), new ItemStack(Material.JUNGLE_WOOD, 256) // Jungle Wood Planks
        );
        ItemStack[] actual = ItemStackUtil.createItemArray(expected);
        assertThat(actual, is(expected.toArray()));
    }

    @Test
    public void createItemStackName() {
        ItemStack actual = ItemStackUtil.createItemStack("DIRT");
        ItemStack expected = new ItemStack(Material.DIRT, 1);
        assertThat(actual, itemStack(expected));

        actual = ItemStackUtil.createItemStack("DIORITE");
        expected = new ItemStack(Material.DIORITE, 1);
        assertThat(actual, itemStack(expected));
    }

    @Test
    public void testClone() {
        List<ItemStack> orig = new ArrayList<>(List.of(new ItemStack(Material.LAVA, 1), new ItemStack(Material.DIRT, 2), new ItemStack(Material.STONE, 3)));
        List<ItemStack> clone = ItemStackUtil.clone(orig);
        assertThat(clone, itemStacks(orig));
        orig.get(0).setAmount(10);
        orig.get(1).setAmount(20);
        orig.remove(2);
        assertThat(clone, not(orig));
        assertThat(clone.size(), is(3));
        assertThat(clone.get(1).getAmount(), is(2));
    }
}
