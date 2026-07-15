package buildlogic.i18n

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.regex.Matcher
import javax.inject.Inject

/**
 * Extracts translatable strings from the Java sources into the committed domain `.pot` templates.
 *
 * This is the one i18n task that still shells out to the gettext toolchain (`xgettext` + `msgcat`):
 * faithfully re-implementing `xgettext` is not worth it, and it only needs to run when strings
 * change. It is therefore kept off the default build (see the `translation` group and the CI drift
 * check) — a plain `./gradlew build` no longer reaches it. The committed `.pot` output is
 * `msgcat`-canonicalised, so the exact pre-`msgcat` shape does not affect the result.
 */
abstract class ExtractTranslationTask : DefaultTask() {
    /**
     * Gradle's process service — the configuration-cache-safe way to run an external process from a
     * task action (the successor to the removed `Project.exec`). It drains the child's stdout/stderr
     * on its own threads, so we neither hand-roll stream pumping nor risk a pipe-buffer deadlock.
     */
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val javaSources: ConfigurableFileCollection

    /** Repo root — `xgettext` runs here so `#:` references are repository-relative. */
    @get:Internal
    abstract val projectRootDir: DirectoryProperty

    @get:OutputFile
    abstract val combinedPotFile: RegularFileProperty

    @get:OutputFile
    abstract val mergedPotFile: RegularFileProperty

    @get:OutputFile
    abstract val playerFacingPotFile: RegularFileProperty

    @get:OutputFile
    abstract val adminOpsPotFile: RegularFileProperty

    @get:OutputFile
    abstract val systemDebugPotFile: RegularFileProperty

    @TaskAction
    fun extract() {
        verifyToolAvailable("xgettext")
        verifyToolAvailable("msgcat")
        val root = projectRootDir.get().asFile

        val combined = combinedPotFile.get().asFile
        combined.parentFile.mkdirs()
        val relativePaths = javaSources.files
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .map { it.relativeTo(root).invariantSeparatorsPath }
        runCommand(
            listOf(
                "xgettext",
                "--language=C#", // C# parser handles Java lambdas and + concatenation better than the Java parser
                "--keyword=tr",
                "--keyword=trLegacy",
                "--keyword=marktr",
                "--keyword=sendTr:2",
                "--keyword=sendErrorTr:2",
                "--from-code=UTF-8",
                "--add-comments=I18N:",
                "--add-location=file",
                "--output=${combined.absolutePath}",
            ) + relativePaths,
            root,
        )
        postProcessPotFile(combined, root)

        val entries = Po.parse(combined.readText()).filterNot { it.isHeader }
        val byDomain = entries.groupBy { TranslationDomain.classifyEntry(it) }
        val domainFiles = mapOf(
            TranslationDomain.PLAYER_FACING to playerFacingPotFile.get().asFile,
            TranslationDomain.ADMIN_OPS to adminOpsPotFile.get().asFile,
            TranslationDomain.SYSTEM_DEBUG to systemDebugPotFile.get().asFile,
        )
        domainFiles.forEach { (domain, file) ->
            writePot(file, byDomain[domain] ?: emptyList())
            postProcessPotFile(file, root)
        }

        val merged = mergedPotFile.get().asFile
        writePot(merged, entries)
        postProcessPotFile(merged, root)

        logger.lifecycle(
            "Extracted translation templates: player_facing={}, admin_ops={}, system_debug={}, merged={}",
            byDomain[TranslationDomain.PLAYER_FACING]?.size ?: 0,
            byDomain[TranslationDomain.ADMIN_OPS]?.size ?: 0,
            byDomain[TranslationDomain.SYSTEM_DEBUG]?.size ?: 0,
            entries.size,
        )
    }

    private fun writePot(file: File, entries: List<PoEntry>) {
        file.parentFile.mkdirs()
        file.writeText(CURATED_POT_HEADER + "\n\n" + Po.write(header = null, entries = entries))
    }

    private fun postProcessPotFile(potFile: File, workingDir: File) {
        if (!potFile.exists()) return
        potFile.writeText(potFile.readText().replace("csharp-format", "java-format"))
        runCommand(
            listOf("msgcat", "-s", "--no-wrap", "--add-location=file", "-o", potFile.absolutePath, potFile.absolutePath),
            workingDir,
        )
        var content = potFile.readText().replace(Regex("\"POT-Creation-Date:.*\\n"), "")
        content = if (content.contains("\n#: ")) {
            sortReferenceCommentBlocks(
                content.replaceFirst(Regex("(?s)^.*?\\n\\n(?=#: )"), Matcher.quoteReplacement("$CURATED_POT_HEADER\n\n")),
            )
        } else {
            "$CURATED_POT_HEADER\n"
        }
        potFile.writeText(content)
    }

    private fun sortReferenceCommentBlocks(content: String): String {
        val lines = content.split('\n')
        val output = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].startsWith("#: ")) {
                val refs = mutableListOf<String>()
                while (i < lines.size && lines[i].startsWith("#: ")) {
                    refs.add(lines[i])
                    i++
                }
                refs.sortBy { it.lowercase() }
                output.addAll(refs)
            } else {
                output.add(lines[i])
                i++
            }
        }
        return output.joinToString("\n")
    }

    /**
     * Runs an external command via Gradle's process service. Output is captured and stays quiet on
     * success (only surfaced at DEBUG); on a non-zero exit it is logged at ERROR and the build fails
     * via [org.gradle.process.ExecResult.assertNormalExitValue]. We avoid `inheritIO()` on purpose:
     * it wires the child to the JVM's real file descriptors and bypasses Gradle's console capture
     * (gradle/gradle#16716, gradle/gradle#16719); routing through ExecOperations keeps that capture.
     */
    private fun runCommand(command: List<String>, workDir: File) {
        logger.info("Executing command: {}", command.joinToString(" "))
        val captured = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine(command)
            workingDir = workDir
            standardOutput = captured
            errorOutput = captured
            isIgnoreExitValue = true
        }
        val output = captured.toString(Charsets.UTF_8).trim()
        if (result.exitValue != 0) {
            if (output.isNotEmpty()) logger.error(output)
            result.assertNormalExitValue()
        } else if (output.isNotEmpty()) {
            logger.debug(output)
        }
    }

    /** Fails with an install hint unless the tool runs and reports its version with a zero exit. */
    private fun verifyToolAvailable(tool: String) {
        val result = try {
            execOperations.exec {
                commandLine(tool, "--version")
                standardOutput = OutputStream.nullOutputStream()
                errorOutput = OutputStream.nullOutputStream()
                isIgnoreExitValue = true
            }
        } catch (e: GradleException) {
            // With isIgnoreExitValue = true, exec() only fails when the process cannot be started.
            throw GradleException("$tool not found. Please install the gettext tools.", e)
        }
        if (result.exitValue != 0) {
            throw GradleException("$tool exited ${result.exitValue} on --version; check your gettext installation.")
        }
    }
}

/**
 * The curated header prepended to every generated `.pot` template. Kept byte-for-byte identical to
 * the previous inline build-script value, because CI drift-checks the committed `.pot` with `git diff`.
 *
 * The trailing `"\n"` is significant: it produces the blank line between the header and the first
 * `#:` entry when this constant is spliced back in during post-processing (`"$CURATED_POT_HEADER\n\n"`
 * yields two newlines after the last header field → one blank line). Do not drop it, or the committed
 * `.pot` loses a blank line and the drift check fails.
 */
private val CURATED_POT_HEADER = """
    # uSkyBlock translation template
    # Copyright (C) 2026 uSkyBlock contributors
    # This file is distributed under GPL-3.0 license.
    # Translators should preserve MiniMessage tags/placeholders exactly.
    #
    msgid ""
    msgstr ""
    "Project-Id-Version: uSkyBlock\n"
    "Report-Msgid-Bugs-To: https://github.com/uskyblock/uSkyBlock/issues\n"
    "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\n"
    "Last-Translator: minoneer <minoneer@gmail.com>\n"
    "Language-Team: LANGUAGE <LL@li.org>\n"
    "Language: \n"
    "MIME-Version: 1.0\n"
    "Content-Type: text/plain; charset=UTF-8\n"
    "Content-Transfer-Encoding: 8bit\n"
    "Plural-Forms: nplurals=2; plural=(n != 1);\n"
""".trimIndent() + "\n"
