package buildlogic.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates the Pirate and Kitteh joke locales — the tool-free replacement for the two Perl scripts.
 * English msgids come from the committed domain `.pot` templates (merged in-process), so this no
 * longer depends on `xgettext`/`msgcat` output and cannot hang on a stray `perl` on the PATH.
 */
abstract class GenerateExtraTranslationsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val domainTemplates: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val english = Po.merge(domainTemplates.files.map { Po.parse(it.readText()) }).entries
        val out = outputDir.get().asFile
        out.mkdirs()
        listOf(Dialect.PIRATE, Dialect.KITTEH).forEach { dialect ->
            out.resolve("${dialect.localeKey}.po").writeText(dialect.generate(english))
        }
    }
}
