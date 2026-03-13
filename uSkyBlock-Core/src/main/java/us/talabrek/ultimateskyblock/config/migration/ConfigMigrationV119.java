package us.talabrek.ultimateskyblock.config.migration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class ConfigMigrationV119 implements ConfigMigration {
    private static final String ISLAND_CHAT_FORMAT = "options.island.chat-format";
    private static final String PARTY_CHAT_FORMAT = "options.party.chat-format";
    private static final String DISPLAY_NAME_TOKEN = "USB_DISPLAY_NAME_TOKEN";
    private static final String MESSAGE_TOKEN = "USB_MESSAGE_TOKEN";

    @Override
    public int fromVersion() {
        return 118;
    }

    @Override
    public int toVersion() {
        return 119;
    }

    @Override
    public void apply(@NotNull YamlConfiguration config, @NotNull Path pluginDataDir) {
        migrateChatFormat(config, ISLAND_CHAT_FORMAT);
        migrateChatFormat(config, PARTY_CHAT_FORMAT);
    }

    private static void migrateChatFormat(@NotNull YamlConfiguration config, @NotNull String path) {
        String value = config.getString(path);
        if (value == null || value.isBlank() || isAlreadyMigrated(value)) {
            return;
        }
        String withTokens = value
            .replace("{DISPLAYNAME}", DISPLAY_NAME_TOKEN)
            .replace("{MESSAGE}", MESSAGE_TOKEN);
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(withTokens);
        String migrated = MiniMessage.miniMessage().serialize(component)
            .replace(DISPLAY_NAME_TOKEN, "<display-name>")
            .replace(MESSAGE_TOKEN, "<message>");
        config.set(path, migrated);
    }

    private static boolean isAlreadyMigrated(@NotNull String value) {
        return value.contains("<display-name>") || value.contains("<message>");
    }
}
