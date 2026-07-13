package buildlogic.i18n

/**
 * A faithful test-only mirror of the runtime parser
 * (`po-utils/.../POParser.readPOAsProperties`), so tests can assert that generated `.po` output
 * actually loads at runtime — the real acceptance criterion. It accepts only comment lines, blank
 * lines, `msgid`/`msgstr` and quoted continuations; anything else throws; only `\n` is unescaped.
 */
object RuntimePoParser {
    fun parse(text: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var key: String? = null
        var value: String? = null
        var lineNo = 0

        fun flush() {
            if (key != null && value != null) result[normalize(key!!)] = normalize(value!!)
        }

        for (raw in text.split("\n")) {
            lineNo++
            val line = raw.trim()
            when {
                line.startsWith("#") -> {}
                line.startsWith("msgid \"") && line.endsWith("\"") -> {
                    if (key != null && value == null) error("msgid without msgstr at line $lineNo")
                    flush()
                    value = null
                    key = line.substring(7, line.length - 1)
                }
                line.startsWith("msgstr \"") && line.endsWith("\"") -> {
                    if (key == null) error("msgstr without msgid at line $lineNo")
                    if (value != null) error("msgstr before msgid at line $lineNo")
                    value = line.substring(8, line.length - 1)
                }
                value != null && line.startsWith("\"") && line.endsWith("\"") ->
                    value += line.substring(1, line.length - 1)
                key != null && line.startsWith("\"") && line.endsWith("\"") ->
                    key += line.substring(1, line.length - 1)
                line.isNotEmpty() -> error("unexpected '$line' at line $lineNo")
            }
        }
        flush()
        return result
    }

    private fun normalize(text: String): String = text.replace("\\n", "\n")
}
