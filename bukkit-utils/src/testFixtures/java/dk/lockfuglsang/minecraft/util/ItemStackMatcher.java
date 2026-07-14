package dk.lockfuglsang.minecraft.util;

import org.bukkit.inventory.ItemStack;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class ItemStackMatcher extends TypeSafeDiagnosingMatcher<ItemStack> {

    private final String expected;

    public ItemStackMatcher(ItemStack expected) {
        this.expected = ItemStackUtil.asString(expected);
    }

    @Override
    protected boolean matchesSafely(ItemStack itemStack, Description description) {
        String other = ItemStackUtil.asString(itemStack);
        description.appendText(" was ").appendValue(other);
        return expected.equals(other);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expected);
    }

    public static ItemStackMatcher itemStack(ItemStack expected) {
        return new ItemStackMatcher(expected);
    }

    public static Matcher<Iterable<? extends ItemStack>> itemStacks(Collection<ItemStack> items) {
        return itemStacks(items.toArray(new ItemStack[0]));
    }

    public static Matcher<Iterable<? extends ItemStack>> itemStacks(ItemStack... items) {
        List<Matcher<? super ItemStack>> matchers = new ArrayList<>();
        for (ItemStack item : items) {
            matchers.add(itemStack(item));
        }
        return contains(matchers);
    }
}
