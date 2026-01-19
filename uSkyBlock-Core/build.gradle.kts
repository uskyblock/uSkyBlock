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
    compileOnly(libs.org.apache.commons.commons.lang3)
    compileOnly(libs.org.apache.httpcomponents.httpclient)
    compileOnly(libs.org.apache.maven.maven.artifact)
}

description = "uSkyBlock-Core"

java {
    withJavadocJar()
}

val poDir = file("src/main/po")

val i18nZip = tasks.register<Zip>("i18nZip") {
    group = "build"
    description = "Zips the .po files into i18n.zip"
    from(poDir) {
        include("*.po")
    }
    archiveFileName.set("i18n.zip")
    destinationDirectory.set(layout.buildDirectory.dir("resources/main"))
}

tasks.processResources {
    from(i18nZip)
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

    workingDir = rootProject.projectDir // Execute from root to get relative paths in POT
    executable = "xgettext"
    args(
        "--language=Java",
        "--keyword=tr",
        "--keyword=marktr",
        "--from-code=UTF-8",
        "--add-comments=I18N:" // Extracts dev hints for AI context
    )
    args("--output=${potFile.absolutePath}")
    // Pass relative paths to ensure relative location comments
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
            // Sort the POT file alphabetically by msgid
            ProcessBuilder("msgcat", "-s", "-o", potFile.absolutePath, potFile.absolutePath)
                .inheritIO().start().waitFor()
        }
    }
}

val mergeTranslation = tasks.register("mergeTranslation") {
    group = "translation"
    description = "Merges extracted strings into .po files"
    dependsOn("extractTranslation")

    doLast {
        val potFile = file("$poDir/keys.pot")
        fileTree(poDir) { include("*.po") }.forEach { poFile ->
            if (poFile.name != "xx_PIRATE.po" && poFile.name != "xx_lol_US.po") {
                ProcessBuilder("msgmerge", "--update", "--no-fuzzy-matching", "--backup=none", "--no-location", poFile.absolutePath, potFile.absolutePath)
                    .inheritIO().start().waitFor()
                ProcessBuilder("msgattrib", "--clear-fuzzy", "--empty", "--no-obsolete", "--no-location", "-o", poFile.absolutePath, poFile.absolutePath)
                    .inheritIO().start().waitFor()
                // Sort the PO file alphabetically by msgid
                ProcessBuilder("msgcat", "-s", "-o", poFile.absolutePath, poFile.absolutePath)
                    .inheritIO().start().waitFor()
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

tasks.register("cleanTranslationFiles") {
    group = "translation"
    description = "Cleans up .po and .pot files (removes timestamps)"

    doLast {
        fileTree(poDir) { include("*.po", "*.pot") }.forEach { file ->
            var content = file.readText()
            content = content.replace(Regex("\"POT-Creation-Date:.*\\n"), "")
            file.writeText(content)
        }
    }
}

tasks.register("updateTranslation") {
    group = "translation"
    description = "Updates all translation files"
    dependsOn(mergeTranslation, generateExtraTranslations)
    finalizedBy("cleanTranslationFiles")
}
