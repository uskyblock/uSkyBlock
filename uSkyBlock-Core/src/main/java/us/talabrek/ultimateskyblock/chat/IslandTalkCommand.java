package us.talabrek.ultimateskyblock.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.tr;

/**
 * Island Talk
 */
@Singleton
public class IslandTalkCommand extends IslandChatCommand {

    @Inject
    public IslandTalkCommand(@NotNull uSkyBlock plugin, @NotNull ChatLogic chatLogic) {
        super(plugin, chatLogic, "islandtalk|istalk|it", "usb.island.talk", tr("talk to players on your island"));
    }
}
