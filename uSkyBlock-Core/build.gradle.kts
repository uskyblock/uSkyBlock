import java.util.regex.Matcher

plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":bukkit-utils"))
    api(project(":po-utils"))
    api(project(":uSkyBlock-API"))
    api(project(":uSkyBlock-APIv2"))
    api(libs.io.papermc.paperlib)
    api(libs.org.bstats.bstats.bukkit)
    api(libs.com.google.inject.guice)
    api(libs.org.jetbrains.annotations)
    testImplementation(testFixtures(project(":bukkit-utils")))
    testImplementation(libs.org.hamcrest.hamcrest)
    testImplementation(libs.org.hamcrest.hamcrest.library.x1)
    testImplementation(libs.org.junit.jupiter.junit.jupiter.api)
    testRuntimeOnly(libs.org.junit.jupiter.junit.jupiter.engine)
    testRuntimeOnly(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.org.mockito.mockito.core)
    testImplementation(libs.net.kyori.adventure.api)
    testImplementation(libs.net.kyori.adventure.text.minimessage)
    testImplementation(libs.net.kyori.adventure.text.serializer.legacy)
    testImplementation(libs.org.apache.commons.commons.lang3)
    implementation(libs.org.xerial.sqlite.jdbc)
    implementation(libs.net.kyori.adventure.text.serializer.plain)
    compileOnly(libs.net.milkbowl.vault.vaultapi)
    compileOnly(libs.org.spigotmc.spigot.api)
    compileOnly(libs.org.mvplugins.multiverse.core.multiverse.core)
    compileOnly(libs.org.mvplugins.multiverse.inventories.multiverse.inventories)
    compileOnly(libs.com.sk89q.worldedit.worldedit.bukkit)
    testImplementation(libs.com.sk89q.worldedit.worldedit.bukkit)
    compileOnly(libs.com.sk89q.worldguard.worldguard.bukkit)
    compileOnly(libs.com.google.guava.guava)
    compileOnly(libs.com.google.code.gson.gson.x1)
    compileOnly(libs.be.maximvdw.mvdwplaceholderapi) {
        exclude(group = "*", module = "*")
    }
    compileOnly(libs.net.kyori.adventure.api)
    compileOnly(libs.net.kyori.adventure.platform.bukkit)
    compileOnly(libs.net.kyori.adventure.text.minimessage)
    compileOnly(libs.net.kyori.adventure.text.serializer.legacy)
    compileOnly(libs.org.apache.commons.commons.lang3)
    compileOnly(libs.org.apache.httpcomponents.httpclient)
    compileOnly(libs.org.apache.maven.maven.artifact)
}

description = "uSkyBlock-Core"

java {
    withJavadocJar()
}

val i18nDir = file("src/main/i18n")
val generatedI18nDir = layout.buildDirectory.dir("generated/i18n")
val supportedLocalesFile = generatedI18nDir.map { it.file("supported-locales.txt") }

enum class TranslationDomain(
    val id: String,
    val precedence: Int
) {
    PLAYER_FACING("player_facing", 3),
    ADMIN_OPS("admin_ops", 2),
    SYSTEM_DEBUG("system_debug", 1)
}

val translationDomains = TranslationDomain.entries
val domainPotFiles = translationDomains.associateWith { domain -> file("$i18nDir/keys.${domain.id}.pot") }
val domainLocaleDirs = translationDomains.associateWith { domain -> file("$i18nDir/${domain.id}") }
val mergedPotFile = generatedI18nDir.map { it.file("keys.pot") }
val combinedExtractionPotFile = generatedI18nDir.map { it.file("keys.all.pot") }
val mergedLocalesDir = generatedI18nDir.map { it.dir("locales") }
val generatedExtraTranslations = mapOf(
    "xx_PIRATE" to generatedI18nDir.map { it.file("xx_PIRATE.po") },
    "xx_lol_US" to generatedI18nDir.map { it.file("xx_lol_US.po") }
)
val generatedExtraLocaleKeys = generatedExtraTranslations.keys

val curatedPotHeader = """
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

""".trimIndent()

fun executeCommand(arguments: List<String>, workingDir: File = rootProject.projectDir) {
    val process = try {
        ProcessBuilder(arguments).directory(workingDir).inheritIO().start()
    } catch (e: Exception) {
        throw GradleException("Unable to execute command: ${arguments.joinToString(" ")}", e)
    }
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw GradleException("Command failed (exit $exitCode): ${arguments.joinToString(" ")}")
    }
}

fun verifyToolAvailable(tool: String) {
    try {
        ProcessBuilder(tool, "--version").start().waitFor()
    } catch (e: Exception) {
        throw GradleException("$tool not found. Please install gettext tools.")
    }
}

fun classifyReferencePath(referencePath: String): TranslationDomain {
    val normalized = referencePath.replace('\\', '/')
    val isSystemDebug = normalized.endsWith("command/admin/DebugCommand.java") ||
        normalized.endsWith("command/admin/GetIslandDataCommand.java") ||
        normalized.endsWith("command/admin/SetIslandDataCommand.java") ||
        normalized.endsWith("command/admin/ItemInfoCommand.java") ||
        normalized.endsWith("command/admin/ImportCommand.java") ||
        normalized.endsWith("command/admin/FlushCommand.java") ||
        normalized.endsWith("command/admin/task/PurgeTask.java") ||
        normalized.endsWith("command/admin/task/PurgeScanTask.java") ||
        normalized.endsWith("command/admin/task/ProtectAllTask.java") ||
        normalized.endsWith("imports/USBImporterExecutor.java") ||
        normalized.endsWith("bukkit-utils/src/main/java/dk/lockfuglsang/minecraft/command/DocumentCommand.java") ||
        normalized.endsWith("bukkit-utils/src/main/java/dk/lockfuglsang/minecraft/command/PlainTextCommandVisitor.java")
    if (isSystemDebug) {
        return TranslationDomain.SYSTEM_DEBUG
    }
    if (normalized.contains("/command/admin/")) {
        return TranslationDomain.ADMIN_OPS
    }
    return TranslationDomain.PLAYER_FACING
}

fun classifyEntry(entry: String): TranslationDomain {
    val referencedDomains = entry.lineSequence()
        .filter { it.startsWith("#: ") }
        .map { classifyReferencePath(it.removePrefix("#: ").trim()) }
        .toList()
    if (referencedDomains.isEmpty()) {
        return TranslationDomain.PLAYER_FACING
    }
    return referencedDomains.maxByOrNull { it.precedence } ?: TranslationDomain.PLAYER_FACING
}

fun parsePotEntries(content: String): List<String> {
    val normalized = content.replace("\r\n", "\n").trim()
    if (normalized.isEmpty()) {
        return emptyList()
    }
    return normalized.split("\n\n")
        .drop(1) // skip header entry
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

fun writePotFile(potFile: File, entries: List<String>) {
    potFile.parentFile.mkdirs()
    val content = buildString {
        append(curatedPotHeader).append("\n\n")
        entries.forEach { entry ->
            append(entry.trimEnd()).append("\n\n")
        }
    }
    potFile.writeText(content)
}

fun sortReferenceCommentBlocks(content: String): String {
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

fun postProcessPotFile(potFile: File) {
    if (!potFile.exists()) {
        return
    }
    var content = potFile.readText().replace("csharp-format", "java-format")
    potFile.writeText(content)
    executeCommand(
        listOf(
            "msgcat",
            "-s",
            "--no-wrap",
            "--add-location=file",
            "-o",
            potFile.absolutePath,
            potFile.absolutePath
        )
    )
    content = potFile.readText().replace(Regex("\"POT-Creation-Date:.*\\n"), "")
    if (content.contains("\n#: ")) {
        content = content.replaceFirst(
            Regex("(?s)^.*?\\n\\n(?=#: )"),
            Matcher.quoteReplacement("$curatedPotHeader\n\n")
        )
        content = sortReferenceCommentBlocks(content)
    } else {
        content = "$curatedPotHeader\n"
    }
    potFile.writeText(content)
}

val mergeDomainTranslations = tasks.register("mergeDomainTranslations") {
    group = "translation"
    description = "Merges domain-specific .po files into locale .po files"
    dependsOn("extractTranslation")
    val includePatterns = translationDomains.map { "${it.id}/*.po" }
    inputs.files(fileTree(i18nDir) { include(*includePatterns.toTypedArray()) })
    inputs.files(domainPotFiles.values)
    outputs.dir(mergedLocalesDir)

    doFirst {
        verifyToolAvailable("msgcat")
    }

    doLast {
        val outputDir = mergedLocalesDir.get().asFile
        outputDir.mkdirs()
        outputDir.listFiles { _, name -> name.lowercase().endsWith(".po") }?.forEach { mergedFile ->
            if (!mergedFile.delete()) {
                throw GradleException("Unable to delete stale merged locale file: ${mergedFile.absolutePath}")
            }
        }

        val domainFiles = fileTree(i18nDir) { include(*includePatterns.toTypedArray()) }.files
        if (domainFiles.isEmpty()) {
            logger.lifecycle("No domain translation files found in ${i18nDir.absolutePath}; skipping mergeDomainTranslations.")
            return@doLast
        }

        val locales = domainFiles
            .map { it.nameWithoutExtension }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)

        locales.forEach { locale ->
            val sources = translationDomains.map { domain ->
                val translatedDomainFile = domainLocaleDirs.getValue(domain).resolve("$locale.po")
                if (translatedDomainFile.exists()) translatedDomainFile else domainPotFiles.getValue(domain)
            }
            val mergedLocaleFile = outputDir.resolve("$locale.po")
            executeCommand(
                listOf(
                    "msgcat",
                    "-s",
                    "--no-wrap",
                    "--add-location=file",
                    "-o",
                    mergedLocaleFile.absolutePath
                ) + sources.map { it.absolutePath }
            )
            logger.lifecycle("Merged domain translations for locale {}", locale)
        }
    }
}

val generateSupportedLocales = tasks.register("generateSupportedLocales") {
    group = "translation"
    description = "Generates a stable list of supported locale keys from .po files"
    dependsOn(mergeDomainTranslations)
    inputs.files(fileTree(mergedLocalesDir) { include("*.po") })
    outputs.file(supportedLocalesFile)
    doLast {
        val locales = fileTree(mergedLocalesDir) { include("*.po") }
            .files
            .map { it.nameWithoutExtension }
            .toMutableSet()
        locales.addAll(generatedExtraLocaleKeys)
        val sortedLocales = locales.sortedBy { it.lowercase() }

        val outputFile = supportedLocalesFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(sortedLocales.joinToString(separator = "\n", postfix = "\n"))
    }
}

val i18nZip = tasks.register<Zip>("i18nZip") {
    group = "build"
    description = "Zips the .po files into i18n.zip"
    dependsOn(generateSupportedLocales, "generateExtraTranslations")
    from(mergedLocalesDir) {
        include("*.po")
    }
    generatedExtraTranslations.values.forEach { generatedPo ->
        from(generatedPo) {
            into("")
        }
    }
    from(supportedLocalesFile) {
        into("")
    }
    archiveFileName.set("i18n.zip")
    // Keep archive output outside processResources destination to avoid self-copy truncation.
    destinationDirectory.set(generatedI18nDir)
}

val i18nZipFile = i18nZip.flatMap { it.archiveFile }

tasks.processResources {
    from(i18nZipFile)
    inputs.file(i18nZipFile)

    val props = mapOf(
        "projectVersion" to project.version,
        "buildNumber" to (System.getenv("GITHUB_RUN_NUMBER") ?: "DEV"),
        "gsonVersion" to libs.versions.com.google.code.gson.gson.x1.get(),
        "guiceVersion" to libs.versions.com.google.inject.guice.get(),
        "guavaVersion" to libs.versions.com.google.guava.guava.get(),
        "adventureApiVersion" to libs.versions.net.kyori.adventure.api.get(),
        "adventureBukkitVersion" to libs.versions.net.kyori.adventure.platform.bukkit.get(),
        "apacheCommonsVersion" to libs.versions.org.apache.commons.commons.lang3.get(),
        "apacheHttpVersion" to libs.versions.org.apache.httpcomponents.httpclient.get(),
        "mavenArtifactVersion" to libs.versions.org.apache.maven.maven.artifact.get(),
        "sqliteVersion" to libs.versions.org.xerial.sqlite.jdbc.get()
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register("extractTranslation") {
    group = "translation"
    description = "Extracts translatable strings into domain-specific .pot files"

    val bukkitUtilsDir = project(":bukkit-utils").projectDir
    val coreDir = projectDir
    val javaFiles = (
        fileTree(bukkitUtilsDir.resolve("src/main/java")) { include("**/*.java") }.files +
            fileTree(coreDir.resolve("src/main/java")) { include("**/*.java") }.files
        )
        .sortedBy { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath }
    val javaFilesRelativePaths = javaFiles.map { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath }

    inputs.files(javaFiles)
    outputs.file(combinedExtractionPotFile)
    outputs.file(mergedPotFile)
    domainPotFiles.values.forEach { outputs.file(it) }

    doFirst {
        verifyToolAvailable("xgettext")
        verifyToolAvailable("msgcat")
    }

    doLast {
        val allPotFile = combinedExtractionPotFile.get().asFile
        allPotFile.parentFile.mkdirs()

        executeCommand(
            listOf(
                "xgettext",
                "--language=C#", // C# parser handles Java lambdas and + concatenation better than Java parser in xgettext
                "--keyword=tr",
                "--keyword=trLegacy",
                "--keyword=marktr",
                "--keyword=sendTr:2",
                "--keyword=sendErrorTr:2",
                "--from-code=UTF-8",
                "--add-comments=I18N:",
                "--add-location=file",
                "--output=${allPotFile.absolutePath}"
            ) + javaFilesRelativePaths,
            rootProject.projectDir
        )
        postProcessPotFile(allPotFile)

        val entries = parsePotEntries(allPotFile.readText())
        val classifiedEntries = entries.map { entry -> entry to classifyEntry(entry) }

        val domainCounts = mutableMapOf<TranslationDomain, Int>()
        translationDomains.forEach { domain ->
            val domainEntries = classifiedEntries
                .filter { (_, classifiedDomain) -> classifiedDomain == domain }
                .map { (entry, _) -> entry }
            domainCounts[domain] = domainEntries.size
            val domainPotFile = domainPotFiles.getValue(domain)
            writePotFile(domainPotFile, domainEntries)
            postProcessPotFile(domainPotFile)
        }

        val mergedEntries = classifiedEntries
            .map { (entry, _) -> entry }
        val mergedPot = mergedPotFile.get().asFile
        writePotFile(mergedPot, mergedEntries)
        postProcessPotFile(mergedPot)

        logger.lifecycle(
            "Extracted translation templates: player_facing={}, admin_ops={}, system_debug={}, merged={}",
            domainCounts[TranslationDomain.PLAYER_FACING] ?: 0,
            domainCounts[TranslationDomain.ADMIN_OPS] ?: 0,
            domainCounts[TranslationDomain.SYSTEM_DEBUG] ?: 0,
            mergedEntries.size
        )
    }
}

val generateExtraTranslations = tasks.register("generateExtraTranslations") {
    group = "translation"
    description = "Generates Pirate and Kitteh translations"
    dependsOn(mergeDomainTranslations)
    inputs.file(mergedPotFile)
    outputs.files(generatedExtraTranslations.values)

    doLast {
        val mergedPot = mergedPotFile.get().asFile
        val pirateScript = file("$i18nDir/en2pirate.pl")
        val kittehScript = file("$i18nDir/en2kitteh.pl")
        val pirateOutput = generatedExtraTranslations.getValue("xx_PIRATE").get().asFile
        val kittehOutput = generatedExtraTranslations.getValue("xx_lol_US").get().asFile
        executeCommand(
            listOf(
                "perl",
                pirateScript.absolutePath,
                mergedPot.absolutePath,
                pirateOutput.absolutePath
            )
        )
        executeCommand(
            listOf(
                "perl",
                kittehScript.absolutePath,
                mergedPot.absolutePath,
                kittehOutput.absolutePath
            )
        )
    }
}

tasks.register("updateTranslation") {
    group = "translation"
    description = "Updates translation sources for Crowdin"
    dependsOn("extractTranslation", "mergeDomainTranslations", "generateExtraTranslations")
}
