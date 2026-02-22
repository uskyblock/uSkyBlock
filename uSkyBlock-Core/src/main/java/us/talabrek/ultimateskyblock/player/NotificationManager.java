package us.talabrek.ultimateskyblock.player;

import com.google.inject.Inject;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NotificationManager {
    private final BukkitAudiences audiences;
    private LegacyComponentSerializer legacySerializer;
    private MiniMessage miniMessage;

    @Inject
    public NotificationManager(Plugin plugin) {
        audiences = BukkitAudiences.create(plugin);
    }

    /**
     * Gets a {@link LegacyComponentSerializer} configured for uSkyBlock's (translatable) messages.
     *
     * @return LegacyComponentSerializer configured for uSkyblock's (translatable) messages.
     */
    public @NotNull LegacyComponentSerializer getLegacySerializer() {
        if (legacySerializer == null) {
            legacySerializer = LegacyComponentSerializer.builder().character('\u00a7').build();
        }
        return legacySerializer;
    }

    public @NotNull MiniMessage getMiniMessage() {
        if (miniMessage == null) {
            miniMessage = MiniMessage.miniMessage();
        }
        return miniMessage;
    }

    /**
     * Sends the given {@link String} as message to the {@link Player}'s ActionBar.
     *
     * @param player  Player to send the given message to
     * @param message Message to send to the given player
     */
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        sendActionBar(player, deserializeMessage(message));
    }

    /**
     * Sends the given {@link Component} as message to the {@link Player}'s ActionBar.
     *
     * @param player    Player to send the given message to
     * @param component Component to send to the given player
     */
    public void sendActionBar(@NotNull Player player, @NotNull Component component) {
        audiences.player(player).sendActionBar(component);
    }

    public void sendMessage(@NotNull CommandSender sender, @Nullable String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        sendMessage(sender, deserializeMessage(message));
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull String... messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull Component component) {
        audiences.sender(sender).sendMessage(component);
    }

    private @NotNull Component deserializeMessage(@NotNull String message) {
        return deserializeMessage(message, getMiniMessage()::deserialize, getLegacySerializer());
    }

    static @NotNull Component deserializeMessage(
        @NotNull String message,
        @NotNull Function<String, Component> miniMessageDeserializer,
        @NotNull LegacyComponentSerializer legacySerializer
    ) {
        try {
            String miniMessageInput = legacyCodesToMiniMessage(message);
            return miniMessageDeserializer.apply(miniMessageInput);
        } catch (RuntimeException e) {
            return legacySerializer.deserialize(message);
        }
    }

    static @NotNull String legacyCodesToMiniMessage(@NotNull String message) {
        if (message.indexOf('\u00a7') < 0) {
            return message;
        }

        StringBuilder out = new StringBuilder(message.length() + 16);
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c != '\u00a7' || i + 1 >= message.length()) {
                out.append(c);
                continue;
            }

            char code = Character.toLowerCase(message.charAt(i + 1));
            if (code == 'x') {
                String hexTag = parseLegacyHexTag(message, i);
                if (hexTag != null) {
                    out.append(hexTag);
                    i += 13; // §x§R§R§G§G§B§B
                    continue;
                }
            }

            String tag = switch (code) {
                case '0' -> "<black>";
                case '1' -> "<dark_blue>";
                case '2' -> "<dark_green>";
                case '3' -> "<dark_aqua>";
                case '4' -> "<dark_red>";
                case '5' -> "<dark_purple>";
                case '6' -> "<gold>";
                case '7' -> "<gray>";
                case '8' -> "<dark_gray>";
                case '9' -> "<blue>";
                case 'a' -> "<green>";
                case 'b' -> "<aqua>";
                case 'c' -> "<red>";
                case 'd' -> "<light_purple>";
                case 'e' -> "<yellow>";
                case 'f' -> "<white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                case 'r' -> "<reset>";
                default -> null;
            };

            if (tag != null) {
                out.append(tag);
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static @Nullable String parseLegacyHexTag(@NotNull String message, int start) {
        if (start + 13 >= message.length()) {
            return null;
        }
        StringBuilder hex = new StringBuilder(6);
        for (int offset = 2; offset <= 12; offset += 2) {
            if (message.charAt(start + offset) != '\u00a7') {
                return null;
            }
            char digit = message.charAt(start + offset + 1);
            if (!isHexDigit(digit)) {
                return null;
            }
            hex.append(Character.toLowerCase(digit));
        }
        return "<#" + hex + ">";
    }

    private static boolean isHexDigit(char ch) {
        char c = Character.toLowerCase(ch);
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f';
    }

    public void shutdown() {
        audiences.close();
    }
}
