import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.api.GradleException

plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.hangar.publish)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.shadow)
}

// A dedicated configuration that will be the *only* input to shadowJar
val shade = configurations.create("shade") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // Modules that should be included inside the final jar
    shade(projects.uSkyBlockAPI) { isTransitive = false }
    shade(projects.uSkyBlockAPIv2) { isTransitive = false }
    shade(projects.uSkyBlockCore) { isTransitive = false }
    shade(projects.uSkyBlockFAWE) { isTransitive = false }
    shade(projects.uSkyBlockPAPI) { isTransitive = false }
    shade(projects.bukkitUtils) { isTransitive = false }
    shade(projects.poUtils) { isTransitive = false }

    // External dependencies to be shaded
    shade(libs.io.papermc.paperlib) { isTransitive = false }
    shade(libs.org.bstats.bstats.bukkit) { isTransitive = true }
}

description = "uSkyBlock-Plugin"

tasks.processResources {
    val props = mapOf(
        "projectVersion" to project.version,
        "githubRunNumber" to (System.getenv("GITHUB_RUN_NUMBER") ?: "DEV"),
        "spigotApiVersion" to libs.versions.org.spigotmc.spigot.api.get()
    )
    inputs.properties(props)
    filesMatching("**/version.json") {
        expand(props)
    }
}

tasks.jar {
    enabled = false
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("uSkyBlock")
    archiveClassifier.set("")
    includeEmptyDirs = false

    // Shade ONLY the 'shade' configuration - exclude anything else
    configurations = listOf(shade)

    relocate("dk.lockfuglsang.minecraft", "us.talabrek.ultimateskyblock.utils")
    relocate("io.papermc.lib", "us.talabrek.ultimateskyblock.paperlib")
    relocate("org.bstats", "us.talabrek.ultimateskyblock.metrics")

    mergeServiceFiles()
}

// Fix for publishing: use the shadow jar instead of the disabled jar
configurations.apiElements {
    outgoing.artifacts.clear()
    outgoing.artifact(shadowJar)
}
configurations.runtimeElements {
    outgoing.artifacts.clear()
    outgoing.artifact(shadowJar)
}

val hangarJarOverride = providers.environmentVariable("HANGAR_JAR").orNull
val hangarVersion = providers.environmentVariable("HANGAR_VERSION")
    .orElse(providers.provider { project.version.toString() })
val hangarChannel = providers.environmentVariable("HANGAR_CHANNEL")
    .orElse(
        providers.provider {
            if (project.version.toString().contains('-')) "Snapshot" else "Release"
        }
    )
val hangarChangelog = providers.environmentVariable("HANGAR_CHANGELOG")
    .orElse("See the canonical GitHub release notes.")
val minecraftVersions = ((findProperty("minecraftVersions") as String?) ?: "")
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

if (minecraftVersions.isEmpty()) {
    throw GradleException("Set the minecraftVersions Gradle property to a comma-separated list of supported versions.")
}

hangarPublish {
    publications.register("plugin") {
        version.set(hangarVersion)
        channel.set(hangarChannel)
        changelog.set(hangarChangelog)
        id.set("uSkyBlock")
        apiKey.set(providers.environmentVariable("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.PAPER) {
                if (hangarJarOverride != null) {
                    jar.set(rootProject.layout.projectDirectory.file(hangarJarOverride))
                } else {
                    jar.set(shadowJar.flatMap { it.archiveFile })
                }
                platformVersions.set(minecraftVersions)
            }
        }
    }
}

val modrinthJarOverride = providers.environmentVariable("MODRINTH_JAR").orNull
val modrinthVersion = providers.environmentVariable("MODRINTH_VERSION")
    .orElse(providers.provider { project.version.toString() })
val modrinthChannel = providers.environmentVariable("MODRINTH_CHANNEL")
    .orElse(
        providers.provider {
            if (project.version.toString().contains('-')) "beta" else "release"
        }
    )
val modrinthChangelog = providers.environmentVariable("MODRINTH_CHANGELOG")
    .orElse("See the canonical GitHub release notes.")

modrinth {
    token.set(providers.environmentVariable("MODRINTH_API_TOKEN"))
    projectId.set("uskyblock")
    versionNumber.set(modrinthVersion)
    versionType.set(modrinthChannel)
    if (modrinthJarOverride != null) {
        uploadFile.set(rootProject.layout.projectDirectory.file(modrinthJarOverride))
        autoAddDependsOn.set(false)
    } else {
        uploadFile.set(shadowJar.flatMap { it.archiveFile })
    }
    gameVersions.set(minecraftVersions)
    loaders.set(listOf("paper", "spigot"))
    changelog.set(modrinthChangelog)
    detectLoaders.set(false)
}

// Minotaur hardcodes `modrinth.dependsOn(assemble)`, dragging the full build chain
// (incl. the gettext-dependent extractTranslation) into the task. When publishing a
// prebuilt release asset, drop those dependencies so the task only uploads the jar
// (no rebuild, no gettext needed), mirroring the Hangar publish path.
if (modrinthJarOverride != null) {
    tasks.named("modrinth") {
        setDependsOn(emptyList<Any>())
    }
}
