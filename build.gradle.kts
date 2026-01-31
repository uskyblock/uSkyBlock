import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


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
