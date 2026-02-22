package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

public class GriefEventsTest {

    @Test
    public void testWitherNaming() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        String name = I18nUtil.trLegacy("<player>'s Wither", unparsed("player", "R4zorax"));
        assertThat(name, is("R4zorax's Wither"));
    }
}
