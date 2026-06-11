package us.talabrek.ultimateskyblock.papi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.placeholder.PlaceholderSource;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UskyblockExpansionTest {

    private PlaceholderSource source;
    private UskyblockExpansion expansion;
    private OfflinePlayer player;

    @BeforeEach
    public void setUp() {
        source = mock(PlaceholderSource.class);
        uSkyBlock plugin = mock(uSkyBlock.class);
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        when(description.getVersion()).thenReturn("3.5.0-test");
        when(plugin.getDescription()).thenReturn(description);
        expansion = new UskyblockExpansion(plugin, source);
        player = mock(OfflinePlayer.class);
    }

    @Test
    public void identifierIsUskyblockAndPersists() {
        assertThat(expansion.getIdentifier(), is("uskyblock"));
        assertTrue(expansion.persist());
        assertThat(expansion.getVersion(), is("3.5.0-test"));
        assertThat(expansion.getAuthor(), is("uSkyBlock"));
        assertThat(expansion.getRequiredPlugin(), is("uSkyBlock"));
    }

    @Test
    public void delegatesToSourceAndSerializesLegacy() {
        when(source.resolve(player, "island_level"))
            .thenReturn(Component.text("120.6").color(NamedTextColor.RED));

        assertThat(expansion.onRequest(player, "island_level"), is("§c120.6"));
    }

    @Test
    public void unknownKeyReturnsNull() {
        when(source.resolve(player, "nope")).thenReturn(null);
        assertNull(expansion.onRequest(player, "nope"));
    }

    @Test
    public void nullPlayerReturnsEmpty() {
        assertThat(expansion.onRequest(null, "island_level"), is(""));
    }
}
