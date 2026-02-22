package us.talabrek.ultimateskyblock.i18n;

import dk.lockfuglsang.minecraft.po.POParser;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranslationKeysMiniMessageParseTest {

    private static final TagResolver SEMANTIC_STYLE_TAGS = TagResolver.resolver(
        Placeholder.styling("muted", NamedTextColor.GRAY),
        Placeholder.styling("primary", NamedTextColor.AQUA),
        Placeholder.styling("secondary", NamedTextColor.GREEN),
        Placeholder.styling("cmd", NamedTextColor.AQUA),
        Placeholder.styling("success", NamedTextColor.GREEN),
        Placeholder.styling("error", NamedTextColor.RED)
    );

    @Test
    public void allPotKeysShouldParseAsMiniMessageWithoutPlaceholderReplacement() throws Exception {
        Path potFile = locatePotFile();
        Properties properties;
        try (InputStream in = Files.newInputStream(potFile)) {
            properties = POParser.asProperties(in);
        }
        assertNotNull("Unable to parse keys.pot", properties);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        List<String> failures = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                miniMessage.deserialize(key, SEMANTIC_STYLE_TAGS);
            } catch (RuntimeException e) {
                failures.add(key + " -> " + e.getMessage());
            }
        }

        assertTrue("MiniMessage parse failures in keys.pot:\n" + String.join("\n", failures), failures.isEmpty());
    }

    private Path locatePotFile() throws IOException {
        Path moduleLocal = Path.of("src/main/po/keys.pot");
        if (Files.exists(moduleLocal)) {
            return moduleLocal;
        }
        Path workspaceRelative = Path.of("uSkyBlock-Core/src/main/po/keys.pot");
        if (Files.exists(workspaceRelative)) {
            return workspaceRelative;
        }
        throw new IOException("Could not locate keys.pot");
    }
}
