package us.talabrek.ultimateskyblock.message;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Centralized message send helper for uSkyBlock-Core.
 *
 * <p>Keep translation markers (tr(...)) at call-sites and pass translated text
 * through this helper so delivery internals can evolve without another
 * codebase-wide refactor.</p>
 */
public class Msg {
    private Msg() {
    }

    private static final Delivery DEFAULT_DELIVERY = (sender, message) -> sender.sendMessage(I18nUtil.legacy(message));
    private static volatile Delivery delivery = DEFAULT_DELIVERY;

    public static final Style DEFAULT = Style.empty();
    public static final Style ERROR = Style.style(NamedTextColor.RED);
    public static final Style SUCCESS = Style.style(NamedTextColor.GREEN);
    public static final Style CMD = Style.style(NamedTextColor.AQUA);
    public static final Style MUTED = Style.style(NamedTextColor.GRAY);
    public static final Style PRIMARY = Style.style(NamedTextColor.AQUA);
    public static final Style SECONDARY = Style.style(NamedTextColor.GREEN);
    public static final Style MENU = Style.style(NamedTextColor.BLUE);

    @FunctionalInterface
    public interface Delivery {
        void send(@NotNull CommandSender sender, @NotNull Component message);
    }

    public static void configure(@Nullable Delivery configuredDelivery) {
        delivery = configuredDelivery != null ? configuredDelivery : DEFAULT_DELIVERY;
    }

    public static void send(@NotNull CommandSender sender, @Nullable Component message) {
        if (message != null) {
            delivery.send(sender, message);
        }
    }

    public static void send(@NotNull CommandSender sender, @NotNull Component... messages) {
        for (Component message : messages) {
            send(sender, message);
        }
    }

    public static void sendTr(@NotNull CommandSender sender, @Nullable String message, @NotNull TagResolver... resolvers) {
        send(sender, tr(message, resolvers));
    }

    public static void sendTr(@NotNull CommandSender sender, @Nullable String message, @Nullable Style style, @NotNull TagResolver... resolvers) {
        send(sender, tr(message, style, resolvers));
    }

    public static void sendError(@NotNull CommandSender sender, @NotNull Component message) {
        sendWithStyle(sender, message, ERROR);
    }

    public static void sendErrorTr(@NotNull CommandSender sender, @Nullable String message, @NotNull TagResolver... resolvers) {
        sendWithStyle(sender, tr(message, resolvers), ERROR);
    }

    public static void sendLegacy(@NotNull CommandSender sender, @Nullable String message) {
        if (message != null) {
            send(sender, I18nUtil.fromLegacy(message));
        }
    }

    public static void sendLegacy(@NotNull CommandSender sender, @NotNull String... messages) {
        for (String message : messages) {
            sendLegacy(sender, message);
        }
    }

    public static void sendPlayerOnly(@NotNull CommandSender sender) {
        sendErrorTr(sender, "This command is only available to players.");
    }

    public static void sendNoCommandAccess(@NotNull CommandSender sender) {
        sendErrorTr(sender, "You do not have access to that command.");
    }

    private static void sendWithStyle(@NotNull CommandSender sender, @Nullable Component message, @NotNull Style style) {
        if (message != null) {
            send(sender, message.applyFallbackStyle(style));
        }
    }

    @NotNull
    public static String plainText(@Nullable Component component) {
        if (component == null) {
            return "";
        } else {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
    }
}
