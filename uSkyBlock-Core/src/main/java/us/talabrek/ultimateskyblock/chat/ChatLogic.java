package us.talabrek.ultimateskyblock.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import us.talabrek.ultimateskyblock.message.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.talabrek.ultimateskyblock.api.IslandInfo;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfigs;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.handler.placeholder.PlaceholderHandler;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static dk.lockfuglsang.minecraft.po.I18nUtil.marktr;
import static dk.lockfuglsang.minecraft.po.I18nUtil.miniToLegacy;
import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static us.talabrek.ultimateskyblock.api.event.IslandChatEvent.Type;
import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendLegacy;

/**
 * The primary logic of uSkyBlocks chat-handling
 */
@Singleton
public class ChatLogic {
    private static final List<String> ALONE_MESSAGE_KEYS = Arrays.asList(
        marktr("But you are ALLLLLLL ALOOOOONE!"),
        marktr("But you are yelling in the wind!"),
        marktr("But your fantasy friends are gone!"),
        marktr("But you are talking to yourself!")
    );
    private final uSkyBlock plugin;
    private final RuntimeConfigs runtimeConfigs;
    private final WorldManager worldManager;
    private final PlaceholderHandler placeholderHandler;
    private final Map<UUID, Type> toggled = new HashMap<>();

    @Inject
    public ChatLogic(
        @NotNull uSkyBlock plugin,
        @NotNull RuntimeConfigs runtimeConfigs,
        @NotNull WorldManager worldManager,
        @NotNull PlaceholderHandler placeholderHandler
    ) {
        this.plugin = plugin;
        this.runtimeConfigs = runtimeConfigs;
        this.worldManager = worldManager;
        this.placeholderHandler = placeholderHandler;
    }

    /**
     * Gets a {@link List} containing {@link Player}'s with all the recipients that should receive the given message
     * {@link Type} from the sending {@link Player}. Returns an empty list when there are no recipients.
     *
     * @param sender   Player sending the message.
     * @param chatType Message type that the player is sending.
     * @return List of all recipients, or an empty list if there are none.
     */
    public @NotNull List<Player> getRecipients(Player sender, Type chatType) {
        if (chatType == Type.PARTY) {
            IslandInfo islandInfo = plugin.getIslandInfo(sender);
            return islandInfo != null ? islandInfo.getOnlineMembers() : Collections.singletonList(sender);
        } else if (chatType == Type.ISLAND) {
            if (worldManager.isSkyWorld(sender.getWorld())) {
                return WorldGuardHandler.getPlayersInRegion(worldManager.getWorld(),
                    WorldGuardHandler.getIslandRegionAt(sender.getLocation()));
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * Sends the given message to all online partymembers or island visitors on the given {@link Player}'s island,
     * depending on the given {@link Type}.
     *
     * @param sender  Player sending the message.
     * @param type    Message type to send.
     * @param message Message to send.
     */
    public void sendMessage(Player sender, Type type, String message) {
        String format = getFormat(type);
        String msg = miniToLegacy(format,
            Placeholder.legacy("display-name", sender.getDisplayName()),
            Placeholder.unparsed("message", message));
        msg = placeholderHandler.replacePlaceholders(sender, msg);
        List<Player> onlineMembers = getRecipients(sender, type);
        if (onlineMembers.size() <= 1) {
            int randomIndex = ThreadLocalRandom.current().nextInt(ALONE_MESSAGE_KEYS.size());
            sendErrorTr(sender, "Sorry! <reason>",
                component("reason", tr(ALONE_MESSAGE_KEYS.get(randomIndex), PRIMARY)));
        } else {
            for (Player member : onlineMembers) {
                sendLegacy(member, msg);
            }
        }
    }

    /**
     * Gets the message format for the given {@link Type}.
     *
     * @param type Island chat type to lookup.
     * @return Message format.
     */
    public @NotNull String getFormat(Type type) {
        return switch (type) {
            case PARTY -> runtimeConfigs.current().party().chatFormat();
            case ISLAND -> runtimeConfigs.current().island().chatFormat();
        };
    }

    /**
     * Toggle the {@link Type} on or off for the given {@link Player}, returns true if it is toggled on.
     *
     * @param player Player to toggle the chat type for.
     * @param type   Chat type to toggle.
     * @return True if it is toggled on, false otherwise.
     */
    public synchronized boolean toggle(Player player, Type type) {
        Type oldType = toggled.get(player.getUniqueId());
        if (oldType == type) {
            toggled.remove(player.getUniqueId());
            return false;
        } else {
            toggled.put(player.getUniqueId(), type);
        }
        return true;
    }

    /**
     * Gets the current {@link Type} toggle for the given {@link Player}, or null if none exists.
     *
     * @param player Player to lookup.
     * @return The current Type toggle, or null if none exists.
     */
    public synchronized @Nullable Type getToggle(Player player) {
        return toggled.get(player.getUniqueId());
    }
}
