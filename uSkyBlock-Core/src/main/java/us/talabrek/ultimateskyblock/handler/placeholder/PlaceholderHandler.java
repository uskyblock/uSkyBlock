package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

@Singleton
public class PlaceholderHandler {

    private final Collection<PlaceholderAPI> apis = new ArrayList<>();

    public void registerPlaceholders(PlaceholderAPI api) {
        apis.add(api);
    }

    @Contract("_, null -> null")
    public String replacePlaceholders(Player player, @Nullable String message) {
        if (message == null) {
            return null;
        }
        String replacedMessage = message;
        for (PlaceholderAPI api : apis) {
            replacedMessage = api.replacePlaceholders(player, replacedMessage);
        }
        return replacedMessage;
    }
}
