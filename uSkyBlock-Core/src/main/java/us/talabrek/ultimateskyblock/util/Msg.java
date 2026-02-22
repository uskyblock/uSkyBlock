package us.talabrek.ultimateskyblock.util;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import net.kyori.adventure.text.Component;
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
public enum Msg {
    ;

    private static final Delivery DEFAULT_DELIVERY = (sender, message) -> sender.sendMessage(I18nUtil.legacy(message));
    private static volatile Delivery delivery = DEFAULT_DELIVERY;

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
        send(sender, tr("<error>This command is only available to players."));
    }

    public static void sendNoCommandAccess(@NotNull CommandSender sender) {
        send(sender, tr("<error>You do not have access to that command."));
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
