package dk.lockfuglsang.minecraft.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dk.lockfuglsang.minecraft.util.FormatUtil.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FormatUtilTest {

    @Test
    public void testNormalize() throws Exception {
        String input = "pre&00&11&22&33&44&55&66&77&88&99&aa&bb&cc&dd&ee&ff&ggpost";
        String expected = "pre§00§11§22§33§44§55§66§77§88§99§aa§bb§cc§dd§ee§ff&ggpost";
        assertThat(normalize(input), is(expected));
    }

    @Test
    public void testWordWrap() throws Exception {
        assertThat(wordWrap("asdadfasd asfasdfasd", 9, 12),
                is(Arrays.asList(
                "asdadfasd", "asfasdfasd")));
        assertThat(wordWrap("§1Hello §2World §3How are you", 4, 10),
                is(Arrays.asList("§1Hello", "§2World §3How", "§3are you")));

        assertThat(wordWrap("§1§2§3§4§5Hello §aWorld §1§2§3§4what happens", 10, 10), is(Arrays.asList(
            "§1§2§3§4§5Hello §aWorld", "§1§2§3§4what happens"
        )));

        assertThat(wordWrap("this is a short story of a long sentence withaverylongword in the end", 14, 14),
                is(Arrays.asList(
                        "this is a short",
                        "story of a long",
                        "sentence withaverylongword",
                        "in the end")));
    }

    @Test
    public void wordWrapStartsEachLineWithTheFormatInEffectAtTheEndOfTheLineBefore() {
        assertThat(wordWrap("a bucket of §clava, some ice and one slime block", 20, 30),
                is(Arrays.asList("a bucket of §clava, some", "§cice and one slime block")));
        assertThat(wordWrap("§c§lred bold words here", 10, 10),
                is(Arrays.asList("§c§lred bold words", "§c§lhere")));
        assertThat(wordWrap("§cred words §lbold words", 10, 10),
                is(Arrays.asList("§cred words", "§c§lbold words")));
        assertThat(wordWrap("§cred words and §rplain words", 10, 10),
                is(Arrays.asList("§cred words", "§cand §rplain", "§rwords")));
    }

    @Test
    public void wordWrapStrictCarriesColourAndFormatTogether() {
        assertThat(wordWrapStrict("§c§lred bold words here", 10),
                is(Arrays.asList("§c§lred bold", "§c§lwords here")));
    }

    @Test
    public void testWordWrapStrict() {
        assertThat(wordWrapStrict("this is a short story of a long sentence withaverylongword in the end", 14),
                is(Arrays.asList(
                        "this is a",
                        "short story of",
                        "a long",
                        "sentence witha",
                        "verylongword",
                        "in the end")));

        assertThat(wordWrapStrict("§1Hello §2World §3How are you", 10),
                is(Arrays.asList("§1Hello", "§2World §3How", "§3are you")));
    }

    @Test
    public void testCapitalize() throws Exception {
        assertThat(camelcase("SAND_CASTLE"), is("SandCastle"));
        assertThat(camelcase("SHEEP"), is("Sheep"));
        assertThat(camelcase("ender chest"), is("EnderChest"));
        assertThat(capitalize("SAND_CASTLE"), is("Sand Castle"));
        assertThat(capitalize("LIGHT_BLUE"), is("Light Blue"));
        assertThat(capitalize("RED"), is("Red"));
    }

    @Test
    public void testEscape() throws Exception {
        String text = "\u00a7eHello World\r\n\u00a7aThis is a \u00a7kmagic\r\n\u00a7lhej";
        assertThat(escape(text), is("&eHello World\r\n&aThis is a &kmagic\r\n&lhej"));
    }
}
