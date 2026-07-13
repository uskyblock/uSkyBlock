package buildlogic.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PoTest {
    @Test
    fun `parses comments, msgid and msgstr`() {
        val entries = Po.parse(
            """
            #. translator note
            #: Foo.java
            msgid "Hello"
            msgstr "Hallo"
            """.trimIndent(),
        )
        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals("Hello", entry.msgid)
        assertEquals("Hallo", entry.msgstr)
        assertEquals(listOf("Foo.java"), entry.references)
        assertEquals(listOf("#. translator note", "#: Foo.java"), entry.comments)
    }

    @Test
    fun `concatenates multi-line msgid and msgstr with no separator`() {
        val entries = Po.parse(
            """
            msgid ""
            "Hello "
            "World"
            msgstr ""
            "Hallo "
            "Welt"
            """.trimIndent(),
        )
        // An empty leading msgid would be the header; here continuations make it a real entry.
        val entry = entries.single()
        assertEquals("Hello World", entry.msgid)
        assertEquals("Hallo Welt", entry.msgstr)
    }

    @Test
    fun `identifies the header entry by empty msgid`() {
        val entries = Po.parse(
            """
            msgid ""
            msgstr ""
            "Language: de\n"

            msgid "x"
            msgstr "y"
            """.trimIndent(),
        )
        assertTrue(entries.first().isHeader)
        assertEquals("Language: de\\n", entries.first().msgstr)
        assertEquals("x", entries[1].msgid)
    }

    @Test
    fun `merge deduplicates by msgid, keeps a non-empty translation, and sorts`() {
        val a = Po.parse(
            """
            msgid ""
            msgstr ""
            "Language: de\n"

            msgid "banana"
            msgstr "Banane"
            """.trimIndent(),
        )
        val b = Po.parse(
            """
            msgid "apple"
            msgstr "Apfel"

            msgid "banana"
            msgstr ""
            """.trimIndent(),
        )
        val merged = Po.merge(listOf(a, b))
        assertEquals(listOf("apple", "banana"), merged.entries.map { it.msgid })
        assertEquals("Banane", merged.entries.first { it.msgid == "banana" }.msgstr)
        assertTrue(merged.header!!.isHeader)
    }

    @Test
    fun `written output parses back through the runtime parser`() {
        val entries = listOf(
            PoEntry(listOf("#: Foo.java"), "Hello", "Hallo"),
            PoEntry(emptyList(), "Line1\\nLine2", "Zeile1\\nZeile2"),
        )
        val header = PoEntry(emptyList(), "", "Language: de\\nContent-Type: text/plain; charset=UTF-8\\n")
        val text = Po.write(header, entries)

        val parsed = RuntimePoParser.parse(text)
        assertEquals("Hallo", parsed["Hello"])
        // A literal \n in the source becomes a real newline at runtime, on both key and value.
        assertEquals("Zeile1\nZeile2", parsed["Line1\nLine2"])
    }
}
