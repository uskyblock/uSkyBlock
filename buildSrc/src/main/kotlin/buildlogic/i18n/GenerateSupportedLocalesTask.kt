package buildlogic.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes the stable, sorted list of supported locale keys (merged locales plus the joke locales). */
abstract class GenerateSupportedLocalesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mergedLocalesDir: DirectoryProperty

    @get:Input
    abstract val extraLocaleKeys: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val poFiles = mergedLocalesDir.get().asFile
            .listFiles { file -> file.isFile && file.extension.equals("po", ignoreCase = true) }
            ?: emptyArray()
        val locales = poFiles.mapTo(mutableSetOf()) { it.nameWithoutExtension }
        locales.addAll(extraLocaleKeys.get())
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(locales.sortedBy { it.lowercase() }.joinToString(separator = "\n", postfix = "\n"))
    }
}
