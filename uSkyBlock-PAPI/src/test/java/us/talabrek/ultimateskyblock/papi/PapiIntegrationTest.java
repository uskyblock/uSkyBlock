package us.talabrek.ultimateskyblock.papi;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.Locale;

import static dk.lockfuglsang.minecraft.po.I18nUtil.parseMini;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static us.talabrek.ultimateskyblock.message.Msg.plainText;

public class PapiIntegrationTest {

    private Player viewer;

    @BeforeEach
    public void setUp() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        viewer = mock(Player.class);
    }

    @Test
    public void papiTagResolvesThroughPlaceholderApi() {
        try (MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(viewer, "%luckperms_prefix%"))
                .thenReturn("§c[Admin]");

            TagResolver resolver = PapiIntegration.papiResolver(viewer);
            Component result = parseMini("<papi:luckperms_prefix> hi", resolver);

            assertThat(plainText(result), is("[Admin] hi"));
        }
    }

    @Test
    public void parameterizedPlaceholderJoinsAllArguments() {
        try (MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(viewer, "%math_5:2+3%"))
                .thenReturn("5");

            Component result = parseMini("<papi:math_5:2+3>", PapiIntegration.papiResolver(viewer));

            assertThat(plainText(result), is("5"));
        }
    }

    @Test
    public void unknownPlaceholderRendersPapiEcho() {
        try (MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(viewer, "%nope%"))
                .thenReturn("%nope%");

            Component result = parseMini("<papi:nope>", PapiIntegration.papiResolver(viewer));

            assertThat(plainText(result), is("%nope%"));
        }
    }

    @Test
    public void throwingExpansionFallsBackToLiteralToken() {
        try (MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(viewer, "%nope%"))
                .thenThrow(new RuntimeException("boom"));

            Component result = parseMini("<papi:nope>", PapiIntegration.papiResolver(viewer));

            assertThat(plainText(result), is("%nope%"));
        }
    }

    @Test
    public void noArgPapiTagRendersLiterally() {
        Component result = parseMini("Hi <papi>!", PapiIntegration.papiResolver(viewer));

        assertThat(plainText(result), is("Hi <papi>!"));
    }
}
