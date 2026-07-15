package buildlogic.i18n

/**
 * A single `.po`/`.pot` entry: its leading comment lines plus the decoded [msgid]/[msgstr] content.
 *
 * "Decoded" means continuation lines are concatenated with no separator (standard PO semantics,
 * matching the runtime `POParser`); escape sequences are **not** interpreted, so a literal `\n` in
 * the source stays a two-character backslash-n here and is written back out verbatim.
 */
data class PoEntry(
    val comments: List<String>,
    val msgid: String,
    val msgstr: String,
) {
    /** `#:` source-location references, without the `#: ` prefix. */
    val references: List<String>
        get() = comments.filter { it.startsWith("#: ") }.map { it.removePrefix("#: ").trim() }

    /** The header entry is the one with an empty msgid. */
    val isHeader: Boolean get() = msgid.isEmpty()
}

/**
 * Minimal `.po` reader/writer/merger — a tool-free replacement for the `msgcat` steps of the i18n
 * build. It only needs to round-trip what the runtime `POParser` accepts: comment lines, blank
 * lines, `msgid`/`msgstr` and quoted continuations. It never emits `msgctxt`/plurals/`\"` escapes,
 * none of which appear in the corpus or are understood by `POParser`.
 */
object Po {
    /** Parses `.po`/`.pot` text into entries (the header entry, if present, is included). */
    fun parse(text: String): List<PoEntry> {
        val entries = mutableListOf<PoEntry>()
        var pendingComments = mutableListOf<String>()
        var comments = emptyList<String>()
        val msgid = StringBuilder()
        val msgstr = StringBuilder()
        var haveMsgid = false
        var inMsgstr = false

        fun flush() {
            if (haveMsgid) {
                entries.add(PoEntry(comments, msgid.toString(), msgstr.toString()))
            }
            comments = emptyList()
            msgid.setLength(0)
            msgstr.setLength(0)
            haveMsgid = false
            inMsgstr = false
        }

        for (raw in text.replace("\r\n", "\n").split("\n")) {
            val line = raw.trim()
            when {
                line.isEmpty() -> flush()
                line.startsWith("#") -> {
                    if (haveMsgid) flush()
                    pendingComments.add(line)
                }
                line.startsWith("msgid \"") && line.endsWith("\"") -> {
                    if (haveMsgid) flush()
                    comments = pendingComments
                    pendingComments = mutableListOf()
                    haveMsgid = true
                    inMsgstr = false
                    msgid.append(unquote(line.removePrefix("msgid ")))
                }
                line.startsWith("msgstr \"") && line.endsWith("\"") -> {
                    inMsgstr = true
                    msgstr.append(unquote(line.removePrefix("msgstr ")))
                }
                line.startsWith("\"") && line.endsWith("\"") -> {
                    if (inMsgstr) msgstr.append(unquote(line)) else if (haveMsgid) msgid.append(unquote(line))
                }
            }
        }
        flush()
        return entries
    }

    /**
     * Merges the given catalogs into one entry list, deduplicating by msgid and sorting by msgid —
     * the tool-free equivalent of `msgcat -s`. The first header entry found is returned separately.
     * Domains are a disjoint partition of the keyset, so in practice each msgid comes from one
     * source; the dedup path (union comments, keep a non-empty msgstr) only guards against overlap.
     */
    fun merge(catalogs: List<List<PoEntry>>): MergeResult {
        val header = catalogs.firstNotNullOfOrNull { entries -> entries.firstOrNull { it.isHeader } }
        val byMsgid = LinkedHashMap<String, PoEntry>()
        for (entries in catalogs) {
            for (entry in entries) {
                if (entry.isHeader) continue
                val existing = byMsgid[entry.msgid]
                byMsgid[entry.msgid] = if (existing == null) {
                    entry
                } else {
                    PoEntry(
                        comments = (existing.comments + entry.comments).distinct(),
                        msgid = entry.msgid,
                        msgstr = existing.msgstr.ifEmpty { entry.msgstr },
                    )
                }
            }
        }
        return MergeResult(header, byMsgid.values.sortedBy { it.msgid })
    }

    /** Renders a catalog back to `.po` text. */
    fun write(header: PoEntry?, entries: List<PoEntry>, includeComments: Boolean = true): String {
        val sb = StringBuilder()
        if (header != null) {
            sb.append(renderHeader(header)).append('\n')
        }
        for (entry in entries) {
            if (includeComments) {
                entry.comments.forEach { sb.append(it).append('\n') }
            }
            sb.append("msgid ").append(quote(entry.msgid)).append('\n')
            sb.append("msgstr ").append(quote(entry.msgstr)).append('\n')
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun renderHeader(header: PoEntry): String {
        val sb = StringBuilder()
        header.comments.forEach { sb.append(it).append('\n') }
        sb.append("msgid \"\"\n")
        sb.append("msgstr \"\"\n")
        // Re-split the joined header value back into one quoted "field\n" line per field.
        header.msgstr.split("\\n").filter { it.isNotEmpty() }.forEach { field ->
            sb.append('"').append(field).append("\\n\"").append('\n')
        }
        return sb.toString()
    }

    private fun unquote(quoted: String): String =
        if (quoted.length >= 2) quoted.substring(1, quoted.length - 1) else ""

    private fun quote(content: String): String = "\"$content\""

    data class MergeResult(val header: PoEntry?, val entries: List<PoEntry>)
}
