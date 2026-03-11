import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException


plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow).apply(false)
}

tasks.register("translation") {
    group = "translation"
    description = "Alias for :uSkyBlock-Core:updateTranslation"
    dependsOn(":uSkyBlock-Core:updateTranslation")
}

tasks.runServer {
    minecraftVersion("1.21.11")
    val shaded = project(":uSkyBlock-Plugin").tasks.named<ShadowJar>("shadowJar")
    dependsOn(shaded)
    pluginJars.from(shaded.flatMap { it.archiveFile })

    downloadPlugins {
        url("https://hangarcdn.papermc.io/plugins/EngineHub/WorldEdit/versions/7.4.0/PAPER/worldedit-bukkit-7.4.0.jar")
        url("https://cdn.modrinth.com/data/DKY9btbd/versions/WaElxvDz/worldguard-bukkit-7.0.15.jar")
    }
}

tasks.register("printPluginVersion") {
    group = "release"
    description = "Print the configured plugin version."
    doLast {
        println(project(":uSkyBlock-Plugin").version)
    }
}

tasks.register("verifyReleaseTag") {
    group = "release"
    description = "Verify that the provided release tag matches the configured plugin version."
    doLast {
        val releaseTag = providers.gradleProperty("releaseTag")
            .orElse(providers.environmentVariable("RELEASE_TAG"))
            .orNull
            ?: throw GradleException("Provide -PreleaseTag=vX.Y.Z or set RELEASE_TAG.")
        val configuredVersion = project(":uSkyBlock-Plugin").version.toString()
        val normalizedTag = releaseTag.removePrefix("refs/tags/").removePrefix("v")
        if (normalizedTag != configuredVersion) {
            throw GradleException(
                "Release tag '$releaseTag' does not match configured version '$configuredVersion'."
            )
        }
        println("Verified release tag '$releaseTag' matches Gradle version '$configuredVersion'.")
    }
}
