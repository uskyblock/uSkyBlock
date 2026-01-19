plugins {
    id("buildlogic.java-conventions")
    id("buildlogic.shadow-conventions")
}

dependencies {
    implementation(project(":uSkyBlock-API"))
    implementation(project(":uSkyBlock-APIv2"))
    implementation(project(":uSkyBlock-Core"))
    implementation(project(":uSkyBlock-FAWE"))
}

description = "uSkyBlock-Plugin"

tasks.processResources {
    val props = mapOf(
        "project.version" to project.version,
        "env.GITHUB_RUN_NUMBER" to (System.getenv("GITHUB_RUN_NUMBER") ?: "DEV"),
        "spigotapi.version" to libs.versions.org.spigotmc.spigot.api.get()
    )
    inputs.properties(props)
    filesMatching("**/version.json") {
        expand(props)
    }
}

tasks.jar {
    enabled = false
    dependsOn(tasks.named("shadowJar"))
}
