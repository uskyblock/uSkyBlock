plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    compileOnly(project(":uSkyBlock-Core"))
    compileOnly(libs.org.spigotmc.spigot.api)
    compileOnly(libs.com.sk89q.worldguard.worldguard.bukkit)

    testImplementation(libs.org.junit.jupiter.junit.jupiter.api)
    testRuntimeOnly(libs.org.junit.jupiter.junit.jupiter.engine)
    testRuntimeOnly(libs.org.junit.platform.junit.platform.launcher)
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
