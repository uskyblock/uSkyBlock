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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TranslationKeysMiniMessageParseTest {
    private static final List<String> DOMAIN_POT_FILENAMES = List.of(
        "keys.player_facing.pot",
        "keys.admin_ops.pot",
        "keys.system_debug.pot"
    );

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
        MiniMessage miniMessage = MiniMessage.miniMessage();
        List<String> failures = new ArrayList<>();
        for (Path potFile : locatePotFiles()) {
            Properties properties;
            try (InputStream in = Files.newInputStream(potFile)) {
                properties = POParser.asProperties(in);
            }
            assertNotNull("Unable to parse " + potFile.getFileName(), properties);

            for (String key : properties.stringPropertyNames()) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                try {
                    miniMessage.deserialize(key, SEMANTIC_STYLE_TAGS);
                } catch (RuntimeException e) {
                    failures.add(potFile.getFileName() + ": " + key + " -> " + e.getMessage());
                }
            }
        }

        assertTrue("MiniMessage parse failures in domain .pot files:\n" + String.join("\n", failures), failures.isEmpty());
    }

    private List<Path> locatePotFiles() throws IOException {
        List<Path> potFiles = new ArrayList<>();
        for (String fileName : DOMAIN_POT_FILENAMES) {
            Path moduleLocal = Path.of("src/main/i18n", fileName);
            if (Files.exists(moduleLocal)) {
                potFiles.add(moduleLocal);
                continue;
            }
            Path workspaceRelative = Path.of("uSkyBlock-Core/src/main/i18n", fileName);
            if (Files.exists(workspaceRelative)) {
                potFiles.add(workspaceRelative);
                continue;
            }
            throw new IOException("Could not locate " + fileName);
        }
        return Collections.unmodifiableList(potFiles);
    }
}
