package us.talabrek.ultimateskyblock.util;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ComponentLineSplitterTest {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @Test
    public void splitLines_nullComponent_returnsEmptyList() {
        List<Component> lines = ComponentLineSplitter.splitLines(null);

        assertThat(lines, is(empty()));
    }

    @Test
    public void splitLines_preservesFormattingAcrossLineBreaks() {
        Component input = Component.text("first\nsecond", NamedTextColor.GREEN);

        List<Component> lines = ComponentLineSplitter.splitLines(input);

        assertThat(lines, hasSize(2));
        assertThat(LEGACY.serialize(lines.get(0)), is("\u00a7afirst"));
        assertThat(LEGACY.serialize(lines.get(1)), is("\u00a7asecond"));
    }

    @Test
    public void splitLines_preservesInheritedParentStyleOnChildLines() {
        Component input = Component.text("first\n", NamedTextColor.RED)
            .append(Component.text("second"));

        List<Component> lines = ComponentLineSplitter.splitLines(input);

        assertThat(lines, hasSize(2));
        assertThat(LEGACY.serialize(lines.get(0)), is("\u00a7cfirst"));
        assertThat(LEGACY.serialize(lines.get(1)), is("\u00a7csecond"));
    }

    @Test
    public void splitLines_preservesNonTextComponentsAndEvents() {
        ClickEvent clickEvent = ClickEvent.openUrl("https://example.com");
        HoverEvent<?> hoverEvent = HoverEvent.showText(Component.text("Hover text"));
        Component translatable = Component.translatable("item.minecraft.apple")
            .color(NamedTextColor.AQUA)
            .clickEvent(clickEvent)
            .hoverEvent(hoverEvent);
        Component input = Component.text("Need ")
            .append(translatable)
            .append(Component.text("\nDone"));

        List<Component> lines = ComponentLineSplitter.splitLines(input);

        assertThat(lines, hasSize(2));
        assertThat(LEGACY.serialize(lines.get(0)), is("Need \u00a7bitem.minecraft.apple"));
        assertThat(LEGACY.serialize(lines.get(1)), is("Done"));

        Component retained = lines.get(0).children().stream()
            .filter(component -> component instanceof TranslatableComponent)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing translatable component in first line"));

        TranslatableComponent retainedTranslatable = (TranslatableComponent) retained;
        assertThat(retainedTranslatable.key(), is("item.minecraft.apple"));
        assertThat(retainedTranslatable.clickEvent(), is(clickEvent));
        assertThat(retainedTranslatable.hoverEvent(), is(hoverEvent));
    }

    @Test
    public void splitLines_supportsCrlfCrLfSeparators_andKeepsTrailingEmptyLine() {
        Component input = Component.text("a\r\nb\rc\n");

        List<Component> lines = ComponentLineSplitter.splitLines(input);

        assertThat(lines, hasSize(4));
        assertThat(LEGACY.serialize(lines.get(0)), is("a"));
        assertThat(LEGACY.serialize(lines.get(1)), is("b"));
        assertThat(LEGACY.serialize(lines.get(2)), is("c"));
        assertThat(LEGACY.serialize(lines.get(3)), is(""));
    }

    @Test
    public void splitLines_preservesNestedFormatting() {
        Component input = I18nUtil.parseMini("Test <red>with<newline>nested <green>formatting</green>.</red>");

        List<Component> lines = ComponentLineSplitter.splitLines(input);

        assertThat(lines, hasSize(2));
        assertThat(LEGACY.serialize(lines.get(0)), is("Test \u00a7cwith"));
        assertThat(LEGACY.serialize(lines.get(1)), is("\u00a7cnested \u00a7aformatting\u00a7c."));
    }
}
