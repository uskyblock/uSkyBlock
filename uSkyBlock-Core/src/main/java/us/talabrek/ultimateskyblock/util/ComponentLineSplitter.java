package us.talabrek.ultimateskyblock.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits Adventure components into line components while preserving resolved styling.
 */
public final class ComponentLineSplitter {
    private ComponentLineSplitter() {
    }

    /**
     * Splits a component into line components on '\n', '\r', and '\r\n'.
     * <p>
     * The resulting line components preserve inherited style attributes by applying effective styles on each part.
     *
     * @param component Component to split.
     * @return One component per line.
     */
    public static @NotNull List<Component> splitLines(@Nullable Component component) {
        if (component == null) {
            return List.of();
        }
        LineCollector collector = new LineCollector();
        appendComponent(component, Style.empty(), collector);
        return collector.finish();
    }

    private static void appendComponent(
        @NotNull Component component,
        @NotNull Style inheritedStyle,
        @NotNull LineCollector collector
    ) {
        Style effectiveStyle = inheritedStyle.merge(component.style(), Style.Merge.Strategy.ALWAYS);
        if (component instanceof TextComponent textComponent) {
            appendTextContent(textComponent.content(), effectiveStyle, collector);
        } else {
            collector.append(component.children(List.of()).style(effectiveStyle));
        }

        for (Component child : component.children()) {
            appendComponent(child, effectiveStyle, collector);
        }
    }

    private static void appendTextContent(
        @NotNull String content,
        @NotNull Style style,
        @NotNull LineCollector collector
    ) {
        int length = content.length();
        int start = 0;
        for (int i = 0; i < length; i++) {
            char current = content.charAt(i);
            if (current != '\n' && current != '\r') {
                continue;
            }

            if (i > start) {
                collector.append(Component.text(content.substring(start, i), style));
            }
            collector.newLine();

            if (current == '\r' && i + 1 < length && content.charAt(i + 1) == '\n') {
                i++;
            }
            start = i + 1;
        }

        if (start < length) {
            collector.append(Component.text(content.substring(start), style));
        }
    }

    private static final class LineCollector {
        private final List<Component> lines = new ArrayList<>();
        private Component currentLine = Component.empty();
        private boolean currentLineHasContent = false;
        private boolean sawLineBreak = false;

        void append(@NotNull Component component) {
            if (Component.empty().equals(component)) {
                return;
            }
            currentLine = currentLine.append(component);
            currentLineHasContent = true;
        }

        void newLine() {
            lines.add(currentLine);
            currentLine = Component.empty();
            currentLineHasContent = false;
            sawLineBreak = true;
        }

        @NotNull List<Component> finish() {
            if (currentLineHasContent || sawLineBreak) {
                lines.add(currentLine);
            }
            return lines;
        }
    }
}
