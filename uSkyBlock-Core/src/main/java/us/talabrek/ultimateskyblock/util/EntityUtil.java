package us.talabrek.ultimateskyblock.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;

/**
 * Handles various entity operations.
 */
public enum EntityUtil {;

    /**
     * @deprecated Use {@link EntityType#getTranslationKey()} instead.
     */
    @Deprecated
    public static String getEntityDisplayName(EntityType entityType) {
        return fallbackEntityName(entityType);
    }

    @NotNull
    public static Component getEntityName(@NotNull EntityType entityType) {
        String translationKey = entityType.getTranslationKey();
        return Component.translatable(translationKey, fallbackEntityName(entityType));
    }

    private static String fallbackEntityName(EntityType entityType) {
        return Arrays.stream(entityType.name().split("_"))
            .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
            .collect(joining(""));
    }
}
