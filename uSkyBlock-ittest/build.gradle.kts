plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    compileOnly(projects.uSkyBlockCore)
    compileOnly(libs.org.spigotmc.spigot.api)
    compileOnly(libs.net.kyori.adventure.api)
    // Server-provided; used only to observe real console output in the message-delivery scenario.
    compileOnly(libs.org.apache.logging.log4j.core)
    // Server-provided; the island region scenarios build regions with it. The guava/gson exclusions
    // are uSkyBlock-Core's: the server ships both.
    compileOnly(libs.com.sk89q.worldedit.worldedit.bukkit) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
    }
    // See uSkyBlock-Core: WorldGuard is server-provided; drop its bundled WorldEdit lineage and the
    // guava/gson the server ships so its strict metadata pins don't conflict on the compile classpath.
    compileOnly(libs.com.sk89q.worldguard.worldguard.bukkit) {
        exclude(group = "com.sk89q.worldedit")
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
    }
}

description = "uSkyBlock live-server integration-test driver and result classifier"

tasks.processResources {
    val props = mapOf("projectVersion" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("uSkyBlock-ittest")
    manifest {
        attributes["Main-Class"] = "us.talabrek.ultimateskyblock.ittest.cli.HarnessCli"
    }
}
