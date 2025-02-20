package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * MVdWPlaceholder proxy
 */
@Singleton
public class MVdWPlaceholderAPI implements PlaceholderAPI {

    private final PlaceholderReplacer replacer;

    @Inject
    MVdWPlaceholderAPI(@NotNull PlaceholderReplacer replacer) {
        this.replacer = replacer;
    }

    public void setup(uSkyBlock plugin) {
        be.maximvdw.placeholderapi.PlaceholderReplacer proxy = e -> {
            if (replacer.getPlaceholders().contains(e.getPlaceholder())) {
                return replacer.replace(e.getOfflinePlayer(), e.getPlayer(), e.getPlaceholder());
            }
            return null;
        };
        for (String placeholder : replacer.getPlaceholders()) {
            be.maximvdw.placeholderapi.PlaceholderAPI.registerPlaceholder(plugin, placeholder, proxy);
        }
    }

    @Override
    public String replacePlaceholders(Player player, String message) {
        return be.maximvdw.placeholderapi.PlaceholderAPI.replacePlaceholders(player, message);
    }
}
