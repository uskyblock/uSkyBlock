package buildlogic.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DialectTest {
    @Test
    fun `translates whole words and preserves capitalization`() {
        assertEquals("pirate", Dialect.PIRATE.translate("player"))
        assertEquals("Pirate", Dialect.PIRATE.translate("Player"))
        assertEquals("ye've", Dialect.PIRATE.translate("you've"))
    }

    @Test
    fun `only matches whole words`() {
        // "and" -> "an'", but "island" must not become "isl an'd"
        assertEquals("island", Dialect.PIRATE.translate("island"))
        assertEquals("an'", Dialect.PIRATE.translate("and"))
    }

    @Test
    fun `leaves the contents of angle-bracket tags untranslated`() {
        assertEquals("<player> be captain", Dialect.PIRATE.translate("<player> are leader"))
    }

    @Test
    fun `does not translate a word immediately after a slash`() {
        assertEquals("/player", Dialect.PIRATE.translate("/player"))
        assertEquals("pirate", Dialect.PIRATE.translate("player"))
    }

    @Test
    fun `applies the substring part-dictionary`() {
        assertEquals("flyin'", Dialect.PIRATE.translate("flying"))
    }

    @Test
    fun `escapes apostrophes for MessageFormat when a placeholder is present`() {
        // "you've" -> "ye've" introduces an apostrophe; with a {0} placeholder it must be doubled.
        assertEquals("ye''ve got {0}", Dialect.PIRATE.translate("you've got {0}"))
        // Without a placeholder the apostrophe stays single.
        assertEquals("ye've", Dialect.PIRATE.translate("you've"))
    }

    @Test
    fun `kitteh translates its own dictionary`() {
        assertEquals("cathouz", Dialect.KITTEH.translate("island"))
        assertEquals("iz", Dialect.KITTEH.translate("is"))
        assertEquals("teh kitteh", Dialect.KITTEH.translate("the player"))
    }

    @Test
    fun `generate emits a runtime-parseable po with translated msgids`() {
        val source = listOf(
            PoEntry(listOf("#: Foo.java"), "player", ""),
            PoEntry(emptyList(), "island", ""),
        )
        val parsed = RuntimePoParser.parse(Dialect.KITTEH.generate(source))
        assertEquals("kitteh", parsed["player"])
        assertEquals("cathouz", parsed["island"])
    }

    @Test
    fun `generation is deterministic`() {
        val source = listOf(
            PoEntry(emptyList(), "the player is saving your island", ""),
            PoEntry(emptyList(), "no reward for players", ""),
        )
        assertEquals(Dialect.KITTEH.generate(source), Dialect.KITTEH.generate(source))
        assertEquals(Dialect.PIRATE.generate(source), Dialect.PIRATE.generate(source))
    }
}
