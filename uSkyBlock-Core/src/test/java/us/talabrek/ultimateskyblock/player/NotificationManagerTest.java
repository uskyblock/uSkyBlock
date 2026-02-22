package us.talabrek.ultimateskyblock.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class NotificationManagerTest {
    private LegacyComponentSerializer legacySerializer;
    private MiniMessage miniMessage;

    @Before
    public void setUp() {
        legacySerializer = LegacyComponentSerializer.builder().character('\u00a7').build();
        miniMessage = MiniMessage.miniMessage();
    }

    @Test
    public void testDeserializeMessage_legacyOnly() {
        Component component = NotificationManager.deserializeMessage(
            "\u00a7eHello \u00a79World\u00a7e!",
            miniMessage::deserialize,
            legacySerializer
        );

        assertThat(legacySerializer.serialize(component), is("\u00a7eHello \u00a79World\u00a7e!"));
    }

    @Test
    public void testDeserializeMessage_miniMessageOnly() {
        Component component = NotificationManager.deserializeMessage(
            "<yellow>Hello <blue>World</blue>!</yellow>",
            miniMessage::deserialize,
            legacySerializer
        );

        assertThat(legacySerializer.serialize(component), is("\u00a7eHello \u00a79World\u00a7e!"));
    }

    @Test
    public void testDeserializeMessage_mixedLegacyAndMiniMessage() {
        Component component = NotificationManager.deserializeMessage(
            "\u00a7eHello <blue>World</blue>!",
            miniMessage::deserialize,
            legacySerializer
        );

        assertThat(legacySerializer.serialize(component), is("\u00a7eHello \u00a79World\u00a7e!"));
    }

    @Test
    public void testDeserializeMessage_fallsBackToLegacyWhenParserFails() {
        Component component = NotificationManager.deserializeMessage(
            "\u00a7eHello <broken>",
            input -> {
                throw new RuntimeException("boom");
            },
            legacySerializer
        );

        assertThat(legacySerializer.serialize(component), is("\u00a7eHello <broken>"));
    }
}
