package us.talabrek.ultimateskyblock.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

/**
 * Talk to your party
 */
@Singleton
public class PartyTalkCommand extends IslandChatCommand {

    @Inject
    public PartyTalkCommand(@NotNull uSkyBlock plugin, @NotNull ChatLogic chatLogic) {
        super(plugin, chatLogic, "partytalk|ptalk|ptk", "usb.party.talk", I18nUtil.tr("talk to your island party"));
    }
}
