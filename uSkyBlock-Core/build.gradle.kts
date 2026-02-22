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
    testImplementation(libs.junit.junit)
    testImplementation(libs.org.junit.vintage.junit.vintage.engine)
    testImplementation(libs.org.mockito.mockito.core)
    testImplementation(libs.net.kyori.adventure.api)
    testImplementation(libs.net.kyori.adventure.text.minimessage)
    testImplementation(libs.net.kyori.adventure.text.serializer.legacy)
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

val poDir = file("src/main/po")
val generatedI18nDir = layout.buildDirectory.dir("generated/i18n")
val supportedLocalesFile = generatedI18nDir.map { it.file("supported-locales.txt") }

val generateSupportedLocales = tasks.register("generateSupportedLocales") {
    group = "translation"
    description = "Generates a stable list of supported locale keys from .po files"
    inputs.files(fileTree(poDir) { include("*.po") })
    outputs.file(supportedLocalesFile)
    doLast {
        val locales = fileTree(poDir) { include("*.po") }
            .files
            .map { it.nameWithoutExtension }
            .sortedBy { it.lowercase() }

        val outputFile = supportedLocalesFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(locales.joinToString(separator = "\n", postfix = "\n"))
    }
}

val i18nZip = tasks.register<Zip>("i18nZip") {
    group = "build"
    description = "Zips the .po files into i18n.zip"
    dependsOn(generateSupportedLocales)
    from(poDir) {
        include("*.po")
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
        "mavenArtifactVersion" to libs.versions.org.apache.maven.maven.artifact.get()
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register<Exec>("extractTranslation") {
    group = "translation"
    description = "Extracts translatable strings from source files"

    val potFile = file("$poDir/keys.pot")
    val bukkitUtilsDir = project(":bukkit-utils").projectDir
    val coreDir = projectDir

    val javaFiles = fileTree(bukkitUtilsDir.resolve("src/main/java")) { include("**/*.java") }.files +
        fileTree(coreDir.resolve("src/main/java")) { include("**/*.java") }.files

    inputs.files(javaFiles)
    outputs.file(potFile)

    workingDir = rootProject.projectDir
    executable = "xgettext"
    args(
        "--language=C#", // C# parser handles Java lambdas and + concatenation better than Java parser in xgettext
        "--keyword=tr",
        "--keyword=trLegacy",
        "--keyword=marktr",
        "--from-code=UTF-8",
        "--add-comments=I18N:",
        "--add-location=file"
    )
    args("--output=${potFile.absolutePath}")
    args(javaFiles.map { it.relativeTo(rootProject.projectDir).path })

    doFirst {
        try {
            ProcessBuilder("xgettext", "--version").start().waitFor()
        } catch (e: Exception) {
            throw GradleException("xgettext not found. Please install gettext tools.")
        }
    }

    doLast {
        if (potFile.exists()) {
            // Correct the format flag from csharp-format to java-format
            var content = potFile.readText().replace("csharp-format", "java-format")
            potFile.writeText(content)
            ProcessBuilder(
                "msgcat", "-s", "--no-wrap", "--add-location=file",
                "-o", potFile.absolutePath, potFile.absolutePath
            ).inheritIO().start().waitFor()

            // Keep keys.pot stable by removing xgettext's generated timestamp.
            content = potFile.readText().replace(Regex("\"POT-Creation-Date:.*\\n"), "")

            // Keep the curated project header stable so contributor guidance remains intact.
            val curatedHeader = """
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
            content = content.replaceFirst(
                Regex("(?s)^.*?\\n\\n(?=#: )"),
                Matcher.quoteReplacement(curatedHeader)
            )
            potFile.writeText(content)
        }
    }
}

val mergeTranslation = tasks.register("mergeTranslation") {
    group = "translation"
    description = "Manual fallback: merge keys.pot into .po files (Crowdin-managed repos usually skip this)"
    dependsOn("extractTranslation")

    doLast {
        val potFile = file("$poDir/keys.pot")
        fileTree(poDir) { include("*.po") }.forEach { poFile ->
            if (poFile.name != "xx_PIRATE.po" && poFile.name != "xx_lol_US.po") {
                ProcessBuilder(
                    "msgmerge",
                    "--update",
                    "--no-fuzzy-matching",
                    "--backup=none",
                    "--no-wrap",
                    "--add-location=file",
                    poFile.absolutePath,
                    potFile.absolutePath
                ).inheritIO().start().waitFor()

                ProcessBuilder(
                    "msgattrib",
                    "--clear-fuzzy",
                    "--empty",
                    "--no-obsolete",
                    "--no-wrap",
                    "-o",
                    poFile.absolutePath,
                    poFile.absolutePath
                ).inheritIO().start().waitFor()

                ProcessBuilder(
                    "msgcat",
                    "-s",
                    "--no-wrap",
                    "--add-location=file",
                    "-o",
                    poFile.absolutePath,
                    poFile.absolutePath
                ).inheritIO().start().waitFor()
            }
        }
    }
}

val generateExtraTranslations = tasks.register("generateExtraTranslations") {
    group = "translation"
    description = "Generates Pirate and Kitteh translations"
    dependsOn("extractTranslation")

    doLast {
        ProcessBuilder("perl", "en2pirate.pl").directory(poDir).inheritIO().start().waitFor()
        ProcessBuilder("perl", "en2kitteh.pl").directory(poDir).inheritIO().start().waitFor()
    }
}

tasks.register("updateTranslation") {
    group = "translation"
    description = "Updates translation sources for Crowdin"
    dependsOn("extractTranslation", "generateExtraTranslations")
}
