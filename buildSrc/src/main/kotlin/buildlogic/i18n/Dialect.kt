package buildlogic.i18n

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A tool-free port of the `en2pirate.pl` / `en2kitteh.pl` joke-locale generators. Both Perl scripts
 * share one algorithm and differ only in their dictionaries and header, so they collapse into this
 * one class with two instances ([PIRATE], [KITTEH]).
 *
 * Per message: split off `<...>` tags (left untranslated), then over each remaining segment apply
 * each dictionary entry as a word-boundary substitution that optionally carries a leading `§`-colour
 * code and never fires right after a `/` (so command names survive), plus its capitalised form;
 * then apply the literal substring [partDict]; finally, if the message carries a `{0}`-style
 * placeholder, normalise apostrophes for MessageFormat (`''`→`'`→`''`).
 *
 * Order is deterministic (insertion order) — unlike Perl's randomised hash iteration, which made the
 * generated joke locales vary run-to-run.
 */
class Dialect(
    /** Filename base and supported-locales key, e.g. `xx_PIRATE` (file `xx_PIRATE.po`). */
    val localeKey: String,
    /** `Language:` header value, e.g. `xx-PIRATE`. */
    val languageTag: String,
    private val lastTranslator: String,
    dict: LinkedHashMap<String, String>,
    private val partDict: LinkedHashMap<String, String>,
) {
    private val tagPattern = Pattern.compile("<[^>]+>")

    private class Rule(val pattern: Pattern, val replacement: String)

    private val rules: List<Rule> = buildList {
        for ((key, value) in dict) {
            add(rule(key, value))
            add(rule(ucfirst(key), ucfirst(value)))
        }
    }

    private fun rule(key: String, value: String): Rule {
        // (?<!/)  \b  (optional §colour)  literal-key  \b   ->   colour + value
        val pattern = Pattern.compile("(?<!/)\\b(§[0-9a-fklmor])?" + Pattern.quote(key) + "\\b")
        return Rule(pattern, "$1" + Matcher.quoteReplacement(value))
    }

    /** Translates a single message string into the dialect. */
    fun translate(text: String): String {
        val out = StringBuilder()
        val matcher = tagPattern.matcher(text)
        var last = 0
        while (matcher.find()) {
            out.append(translateSegment(text.substring(last, matcher.start())))
            out.append(matcher.group()) // keep the <tag> verbatim
            last = matcher.end()
        }
        out.append(translateSegment(text.substring(last)))

        var result = out.toString()
        if (result.contains("{0")) {
            result = result.replace("''", "'").replace("'", "''")
        }
        return result
    }

    private fun translateSegment(segment: String): String {
        var s = segment
        for (rule in rules) {
            s = rule.pattern.matcher(s).replaceAll(rule.replacement)
        }
        for ((key, value) in partDict) {
            s = s.replace(key, value)
        }
        return s
    }

    /** Builds a full dialect `.po` from the English source entries (their msgids are translated). */
    fun generate(source: List<PoEntry>): String {
        val header = PoEntry(
            comments = emptyList(),
            msgid = "",
            msgstr = "Language: $languageTag\\nLast-Translator: $lastTranslator\\n" +
                "Content-Type: text/plain; charset=UTF-8\\n",
        )
        val entries = source
            .filterNot { it.isHeader }
            .map { PoEntry(emptyList(), it.msgid, translate(it.msgid)) }
        return Po.write(header, entries, includeComments = false)
    }

    private fun ucfirst(text: String): String = text.replaceFirstChar { it.uppercaseChar() }

    companion object {
        // NOTE: en2pirate.pl also declared '\sis', '\sisland', '\sislands', but Perl's \Q…\E quoted
        // the leading \s literally, so those keys matched a backslash and never fired. They are
        // omitted here rather than faithfully reproducing a dead regex; the observable output is
        // unchanged. The 'hideout' part-dict entries are kept as in the source (they, too, never
        // match now that pirate no longer produces "hideout").
        val PIRATE = Dialect(
            localeKey = "xx_PIRATE",
            languageTag = "xx-PIRATE",
            lastTranslator = "R4zorax",
            dict = linkedMapOf(
                "you've" to "ye've",
                "your" to "yer",
                "you" to "ye",
                "am" to "be",
                "are" to "be",
                "my" to "me",
                "them" to "'em",
                "no" to "nay",
                "yes" to "aye",
                "and" to "an'",
                "player" to "pirate",
                "players" to "pirates",
                "member" to "mates",
                "warning" to "avast",
                "error" to "blimey",
                "wrong" to "awry",
                "leader" to "captain",
                "saving" to "storin'",
                "reward" to "bounty",
            ),
            partDict = linkedMapOf(
                "ing" to "in'",
                "on yer hideout" to "in yer hideout",
                "an hideout" to "a hideout",
            ),
        )

        val KITTEH = Dialect(
            localeKey = "xx_lol_US",
            languageTag = "xx_lol_US",
            lastTranslator = "Woolwind",
            dict = linkedMapOf(
                "you've" to "kitteh haz",
                "your" to "yoor",
                "am" to "iz",
                "is" to "iz",
                "are" to "be",
                "my" to "mah",
                "them" to "'dem",
                "no" to "noe",
                "and" to "an'",
                "has" to "haz",
                "was" to "wuz",
                "player" to "kitteh",
                "players" to "kittehs",
                "member" to "cats",
                "warning" to "oh noes",
                "error" to "yikes",
                "wrong" to "awry",
                "leader" to "top cat",
                "saving" to "storin'",
                "reward" to "kitteh treet",
                "island" to "cathouz",
                "islands" to "cathouzez",
                "requirements" to "stuffs needed",
                "currency" to "moniez",
                "Item" to "stuffs",
                "the" to "teh",
                "block" to "blok",
                "blocks" to "bloks",
                "biomes" to "biomz",
                "biome" to "baium",
                "grass" to "graz",
                "more" to "moar",
                "requires" to "rekwirz",
                "days" to "dais",
                "within" to "wiffin",
                "protects" to "proteks",
                "purge" to "purrrge",
                "orphans" to "orfanz",
                "invitation" to "invitashun",
            ),
            partDict = linkedMapOf(
                "an cathouz" to "a cathouz",
                "iz not available" to "kitteh no can haz",
                "iz not repeatable" to "can haz onlie wunce",
                "teh following bloks short" to "no haz dese bloks",
            ),
        )
    }
}
