package dk.lockfuglsang.minecraft.po;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;

public class I18nUtilTest {
    @BeforeEach
    public void setUp() {
        URL dataFolderUrl = getClass().getClassLoader().getResource("");
        I18nUtil.initialize(new File(dataFolderUrl.getFile()), Locale.ENGLISH);
    }

    @Test
    public void testTr_nullKey() {
        assertThat(I18nUtil.legacy(I18nUtil.tr(null)), is(""));
    }

    @Test
    public void testTr_emptyKey() {
        assertThat(I18nUtil.legacy(I18nUtil.tr("")), is(""));
    }

    @Test
    public void testTr_existingKey() {
        String TEST_STRING = "\u00a7eYou do not have access to that island-schematic!";
        String TEST_RESULT = "\u00a7eYou have no azzess to the schemz";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING)), is(TEST_RESULT));
    }

    @Test
    public void testTr_nonExistingKey() {
        String TEST_STRING = "\u00a7eYou have no access to that island-schematic!";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING)), is(TEST_STRING));
    }

    @Test
    public void testTr_nonExistingKeyWithUnknownMiniMessageTag() {
        String TEST_STRING = "Hello <player>!";
        String TEST_RESULT = "Hello <player>!";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING)), is(TEST_RESULT));
    }

    @Test
    public void testTr_existingKeyWithLocaleFallbackToLanguageFile() {
        I18nUtil.setLocale(Locale.US);
        String TEST_STRING = "\u00a7eYou do not have access to that island-schematic!";
        String TEST_RESULT = "\u00a7eYou have no azzess to the schemz";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING)), is(TEST_RESULT));
    }

    @Test
    public void testTr_nonExistingKeyWithNamedUnparsedPlaceholder() {
        String TEST_STRING = "Hello <name>!";
        String TEST_RESULT = "Hello <red>World</red>!";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING, Placeholder.unparsed("name", "<red>World</red>"))), is(TEST_RESULT));
    }

    @Test
    public void testTr_nonExistingKeyWithNamedComponentPlaceholder() {
        String TEST_STRING = "Hello <name>!";
        String TEST_RESULT = "Hello World!";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING, Placeholder.component("name", Component.text("World")))), is(TEST_RESULT));
    }

    @Test
    public void testTrLegacy_nonExistingKeyWithNumberFormatter() {
        String TEST_STRING = "Value: <value>";
        String TEST_RESULT = "Value: 250.25";

        assertThat(I18nUtil.trLegacy(TEST_STRING, Formatter.number("value", 250.25d)), is(TEST_RESULT));
    }

    @Test
    public void testTrLegacy_withLegacyArgColoredValue() {
        String TEST_STRING = "<yellow>Hello <name>!</yellow>";
        String TEST_RESULT = "\u00a7eHello \u00a7cWorld\u00a7e!";

        assertThat(I18nUtil.trLegacy(TEST_STRING, I18nUtil.legacyArg("name", "\u00a7cWorld")), is(TEST_RESULT));
    }

    @Test
    public void testTrLegacy_withLegacyArgNullValue() {
        String TEST_STRING = "Hello <name>!";
        String TEST_RESULT = "Hello !";

        assertThat(I18nUtil.trLegacy(TEST_STRING, I18nUtil.legacyArg("name", null)), is(TEST_RESULT));
    }

    @Test
    public void testTr_nonExistingKeyWithEscapedMiniMessageTag() {
        String TEST_STRING = "Hello \\<blue>World";
        String TEST_RESULT = "Hello <blue>World";

        assertThat(I18nUtil.legacy(I18nUtil.tr(TEST_STRING)), is(TEST_RESULT));
    }

    @Test
    public void testMarktr_nullKey() {
        assertNull(I18nUtil.marktr(null));
    }

    @Test
    public void testMarktr_emptyKey() {
        assertThat(I18nUtil.marktr(""), is(""));
    }

    @Test
    public void testMarktr_withString() {
        String TEST_STRING = "This is a test message for {0}.";

        assertThat(I18nUtil.marktr(TEST_STRING), is(TEST_STRING));
    }

    @Test
    public void testMiniToLegacy_nullString() {
        assertThat(I18nUtil.miniToLegacy(null), is(""));
    }

    @Test
    public void testMiniToLegacy_emptyString() {
        assertThat(I18nUtil.miniToLegacy(""), is(""));
    }

    @Test
    public void testMiniToLegacy_withNonFormattedString() {
        String TEST_STRING = "\u00a7eThis is a test string";

        assertThat(I18nUtil.miniToLegacy(TEST_STRING), is(TEST_STRING));
    }

    @Test
    public void testMiniToLegacy_withFormattedString() {
        String TEST_STRING = "<aqua>This is a test for <user> regarding <plugin>.";
        String TEST_RESULT = "\u00a7bThis is a test for Jinxert regarding Ultimate Skyblock.";

        assertThat(I18nUtil.miniToLegacy(TEST_STRING,
            Placeholder.unparsed("user", "Jinxert"),
            Placeholder.unparsed("plugin", "Ultimate Skyblock")), is(TEST_RESULT));
    }

    @Test
    public void testMiniToLegacy_withSemanticAliasTags() {
        String TEST_STRING = "Use <cmd>/is home</cmd> <muted>to return.";
        String TEST_RESULT = "Use \u00a7b/is home\u00a7r \u00a77to return.";

        assertThat(I18nUtil.miniToLegacy(TEST_STRING), is(TEST_RESULT));
    }

    @Test
    public void testGetLocale_unset() {
        assertThat(I18nUtil.getLocale(), is(Locale.ENGLISH));
    }

    @Test
    public void testGetLocale_setToChina() {
        I18nUtil.setLocale(Locale.CHINA);

        assertThat(I18nUtil.getLocale(), is(Locale.CHINA));
    }

    @Test
    public void testSetLocale_setToNull() {
        I18nUtil.setLocale(null);

        assertThat(I18nUtil.getLocale(), is(Locale.ENGLISH));
    }
}
