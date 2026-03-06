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

    @Test
    public void allPoValuesShouldParseAsMiniMessageWithoutPlaceholderReplacement() throws Exception {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        List<String> failures = new ArrayList<>();
        for (Path poFile : locatePoFiles()) {
            Properties properties;
            try (InputStream in = Files.newInputStream(poFile)) {
                properties = POParser.asProperties(in);
            }
            assertNotNull("Unable to parse " + poFile, properties);

            for (String key : properties.stringPropertyNames()) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                String value = properties.getProperty(key);
                if (value == null || value.isBlank()) {
                    continue;
                }
                try {
                    miniMessage.deserialize(value, SEMANTIC_STYLE_TAGS);
                } catch (RuntimeException e) {
                    failures.add(poFile + ": " + key + " -> " + e.getMessage());
                }
            }
        }

        assertTrue("MiniMessage parse failures in translated .po files:\n" + String.join("\n", failures), failures.isEmpty());
    }

    private List<Path> locatePotFiles() throws IOException {
        Path i18nRoot = locateI18nRoot();
        List<Path> potFiles = new ArrayList<>();
        for (String fileName : DOMAIN_POT_FILENAMES) {
            Path potFile = i18nRoot.resolve(fileName);
            if (!Files.exists(potFile)) {
                throw new IOException("Could not locate " + fileName + " under " + i18nRoot);
            }
            potFiles.add(potFile);
        }
        return Collections.unmodifiableList(potFiles);
    }

    private List<Path> locatePoFiles() throws IOException {
        Path i18nRoot = locateI18nRoot();
        List<Path> poFiles;
        try (var files = Files.walk(i18nRoot)) {
            poFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".po"))
                .sorted()
                .toList();
        }
        if (poFiles.isEmpty()) {
            throw new IOException("Could not locate any .po files under " + i18nRoot);
        }
        return Collections.unmodifiableList(poFiles);
    }

    private Path locateI18nRoot() throws IOException {
        Path moduleLocal = Path.of("src/main/i18n");
        if (Files.exists(moduleLocal)) {
            return moduleLocal;
        }
        Path workspaceRelative = Path.of("uSkyBlock-Core/src/main/i18n");
        if (Files.exists(workspaceRelative)) {
            return workspaceRelative;
        }
        throw new IOException("Could not locate src/main/i18n");
    }
}
