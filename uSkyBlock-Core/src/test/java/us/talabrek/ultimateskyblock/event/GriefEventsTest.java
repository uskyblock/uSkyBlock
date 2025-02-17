package us.talabrek.ultimateskyblock.event;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Locale;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GriefEventsTest {

    @Test
    public void testWitherNaming() throws ParseException {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        String name = I18nUtil.tr("{0}''s Wither", "R4zorax");
        assertThat(name, is("R4zorax's Wither"));

        Object[] parse = new MessageFormat(I18nUtil.marktr("{0}''s Wither")).parse(name);
        assertThat(parse, CoreMatchers.notNullValue());
        assertThat(parse.length, is(1));
        assertThat((String)parse[0], is("R4zorax"));
    }
}
