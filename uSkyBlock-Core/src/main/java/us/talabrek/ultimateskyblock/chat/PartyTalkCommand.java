package us.talabrek.ultimateskyblock.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;

/**
 * Talk to your party
 */
@Singleton
public class PartyTalkCommand extends IslandChatCommand {

    @Inject
    public PartyTalkCommand(@NotNull uSkyBlock plugin, @NotNull ChatLogic chatLogic) {
        super(plugin, chatLogic, "partytalk|ptalk|ptk", "usb.party.talk", trLegacy("talk to your island party"));
    }
}
