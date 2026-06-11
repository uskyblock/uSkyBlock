package us.talabrek.ultimateskyblock.config.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v124: placeholder modernization. Rewrites {usb_*} tokens in chat formats to
 * MiniMessage &lt;usb:*&gt; tags, and drops the removed placeholder config section
 * and the placeholder cache tuning key.
 */
public final class ConfigMigrationV124 implements ConfigMigration {

    private static final Pattern USB_TOKEN = Pattern.compile("\\{usb_([a-z_]+)}");
    private static final String[] CHAT_FORMAT_KEYS = {
        "options.island.chat-format",
        "options.party.chat-format",
    };

    @Override
    public int fromVersion() {
        return 123;
    }

    @Override
    public int toVersion() {
        return 124;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        for (String key : CHAT_FORMAT_KEYS) {
            String format = config.getString(key);
            if (format != null) {
                Matcher matcher = USB_TOKEN.matcher(format);
                config.set(key, matcher.replaceAll("<usb:$1>"));
            }
        }
        config.set("placeholder", null);
        config.set("options.advanced.placeholderCache", null);
    }
}
