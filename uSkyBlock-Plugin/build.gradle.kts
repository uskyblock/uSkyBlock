import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.api.GradleException

plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    alias(libs.plugins.shadow)
}

// A dedicated configuration that will be the *only* input to shadowJar
val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // Modules that should be included inside the final jar
    shade(project(":uSkyBlock-API")) { isTransitive = false }
    shade(project(":uSkyBlock-APIv2")) { isTransitive = false }
    shade(project(":uSkyBlock-Core")) { isTransitive = false }
    shade(project(":uSkyBlock-FAWE")) { isTransitive = false }
    shade(project(":bukkit-utils")) { isTransitive = false }
    shade(project(":po-utils")) { isTransitive = false }

    // External dependencies to be shaded
    shade("io.papermc:paperlib:${libs.versions.io.papermc.paperlib.get()}") { isTransitive = false }
    shade("org.bstats:bstats-bukkit:${libs.versions.org.bstats.bstats.bukkit.get()}") { isTransitive = true }
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
