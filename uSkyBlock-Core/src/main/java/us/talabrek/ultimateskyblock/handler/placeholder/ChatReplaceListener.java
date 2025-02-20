package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ChatReplaceListener implements Listener {

    private final PlaceholderHandler placeholderHandler;

    @Inject
    public ChatReplaceListener(@NotNull PlaceholderHandler placeholderHandler) {
        this.placeholderHandler = placeholderHandler;
    }

    @EventHandler
    public void onChatEvent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        event.setFormat(placeholderHandler.replacePlaceholders(player, event.getFormat()));
        event.setMessage(placeholderHandler.replacePlaceholders(player, event.getMessage()));
    }
}
