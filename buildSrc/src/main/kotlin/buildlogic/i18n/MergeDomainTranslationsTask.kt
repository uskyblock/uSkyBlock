package buildlogic.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Merges the per-domain translation catalogs into one `.po` per locale — the tool-free replacement
 * for the `msgcat` merge step. For each locale it concatenates the three domain catalogs (falling
 * back to a domain's untranslated `.pot` template when that locale has no translation for it),
 * deduplicates by msgid, and sorts. Reads only committed sources, so it no longer depends on the
 * `xgettext` extraction and needs no external tools.
 */
abstract class MergeDomainTranslationsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val i18nDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun merge() {
        val src = i18nDir.get().asFile
        val out = outputDir.get().asFile
        out.mkdirs()
        out.listFiles { _, name -> name.lowercase().endsWith(".po") }?.forEach { stale ->
            if (!stale.delete()) {
                throw GradleException("Unable to delete stale merged locale file: ${stale.absolutePath}")
            }
        }

        val domains = TranslationDomain.ordered
        val locales = domains
            .flatMap { domain ->
                src.resolve(domain.id)
                    .listFiles { file -> file.isFile && file.extension.equals("po", ignoreCase = true) }
                    ?.toList()
                    ?: emptyList()
            }
            .map { it.nameWithoutExtension }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)

        if (locales.isEmpty()) {
            logger.lifecycle("No domain translation files found in {}; skipping merge.", src.absolutePath)
            return
        }

        locales.forEach { locale ->
            val catalogs = domains.map { domain ->
                val translated = src.resolve(domain.id).resolve("$locale.po")
                val source = if (translated.exists()) translated else src.resolve("keys.${domain.id}.pot")
                Po.parse(source.readText())
            }
            val merged = Po.merge(catalogs)
            out.resolve("$locale.po").writeText(Po.write(merged.header, merged.entries))
        }
        logger.lifecycle("Merged {} locale catalogs (tool-free)", locales.size)
    }
}
