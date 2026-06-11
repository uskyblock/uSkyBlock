package us.talabrek.ultimateskyblock.placeholder;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Locale;

import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.talabrek.ultimateskyblock.message.Msg.plainText;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;

public class PlaceholderServiceTest {

    private PlaceholderSource source;
    private PlaceholderService service;
    private Player viewer;

    @BeforeEach
    public void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        source = mock(PlaceholderSource.class);
        service = new PlaceholderService(source);
        viewer = mock(Player.class);
    }

    @Test
    public void resolvesUsbTagFromSource() {
        when(source.resolve(viewer, "island_level")).thenReturn(Component.text("120.6"));

        Component result = parseMini("Level: <usb:island_level>", service.resolvers(viewer));

        assertThat(plainText(result), is("Level: 120.6"));
    }

    @Test
    public void unknownKeyRendersTagLiterally() {
        when(source.resolve(viewer, "bogus")).thenReturn(null);

        Component result = parseMini("Hi <usb:bogus>!", service.resolvers(viewer));

        assertThat(plainText(result), is("Hi <usb:bogus>!"));
    }

    @Test
    public void unknownKeyDoesNotDegradeRestOfMessage() {
        when(source.resolve(viewer, "island_level")).thenReturn(Component.text("42"));
        when(source.resolve(viewer, "bogus")).thenReturn(null);

        Component result = parseMini("Level <usb:island_level> and <usb:bogus>", service.resolvers(viewer));

        assertThat(plainText(result), is("Level 42 and <usb:bogus>"));
    }

    @Test
    public void throwingSourceRendersTagLiterallyWithoutDegradingRestOfMessage() {
        when(source.resolve(viewer, "version")).thenReturn(Component.text("1.0"));
        when(source.resolve(viewer, "island_level")).thenThrow(new IllegalStateException("player data failed to load"));

        Component result = parseMini("v<usb:version> Level <usb:island_level>!", service.resolvers(viewer));

        assertThat(plainText(result), is("v1.0 Level <usb:island_level>!"));
    }

    @Test
    public void usbTagWithoutArgumentRendersLiterally() {
        Component result = parseMini("Hi <usb>!", service.resolvers(viewer));

        assertThat(plainText(result), is("Hi <usb>!"));
    }

    @Test
    public void registeredContributionsResolve() {
        service.register(player -> component("external_test", Component.text("X")));

        Component result = parseMini("<external_test>", service.resolvers(viewer));

        assertThat(plainText(result), is("X"));
    }
}
