import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")

    if (project.name == "uSkyBlock-Core") {
        relocate("dk.lockfuglsang.minecraft", "us.talabrek.ultimateskyblock.utils")
        relocate("io.papermc.lib", "us.talabrek.ultimateskyblock.paperlib")
        relocate("org.bstats", "us.talabrek.ultimateskyblock.metrics")
    }

    if (project.name == "uSkyBlock-Plugin") {
        mergeServiceFiles()
    }
}
