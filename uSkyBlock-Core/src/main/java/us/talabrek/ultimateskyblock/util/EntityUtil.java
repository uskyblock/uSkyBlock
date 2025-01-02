package us.talabrek.ultimateskyblock.util;

import dk.lockfuglsang.minecraft.util.FormatUtil;
import org.bukkit.entity.EntityType;

/**
 * Handles various entity operations.
 */
public enum EntityUtil {;

    public static String getEntityDisplayName(EntityType entityType) {
        return FormatUtil.camelcase(entityType.name());
    }
}
