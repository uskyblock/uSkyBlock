import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
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
