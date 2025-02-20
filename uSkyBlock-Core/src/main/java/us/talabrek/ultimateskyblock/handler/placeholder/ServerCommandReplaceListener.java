package us.talabrek.ultimateskyblock.handler.placeholder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ServerCommandReplaceListener implements Listener {

    private final PlaceholderHandler placeholderHandler;

    @Inject
    public ServerCommandReplaceListener(@NotNull PlaceholderHandler placeholderHandler) {
        this.placeholderHandler = placeholderHandler;
    }

    @EventHandler
    public void onCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        String replacement = placeholderHandler.replacePlaceholders(event.getSender() instanceof Player ? (Player) event.getSender() : null, command);
        if (replacement != null && !command.equals(replacement)) {
            event.setCommand(command);
        }
    }
}
