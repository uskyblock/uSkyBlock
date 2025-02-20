package us.talabrek.ultimateskyblock.handler.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by R4zorax on 26/04/2016.
 */
public class TextPlaceholderTest {
    @Test
    public void replacePlaceholders() throws Exception {
        TextPlaceholder placeholder = new TextPlaceholder(new PlaceholderAPI.PlaceholderReplacer() {
            @Override
            public @NotNull Collection<String> getPlaceholders() {
                return Set.of("usb_replaceme");
            }

            @Override
            public String replace(OfflinePlayer offlinePlayer, Player player, String placeholder) {
                return "replaced string";
            }
        });
        assertThat(placeholder.replacePlaceholders(null, null), is(nullValue()));
        assertThat(placeholder.replacePlaceholders(null, "Hi {uskyblock_island_level}"), is("Hi {uskyblock_island_level}"));
        assertThat(placeholder.replacePlaceholders(null, "Hi {usb_island_level}"), is("Hi {usb_island_level}"));
        assertThat(placeholder.replacePlaceholders(null, "Hi {usb_replaceme} please"), is("Hi replaced string please"));
    }
}
