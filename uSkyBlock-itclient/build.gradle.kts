import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-conventions")
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://repo.opencollab.dev/main/") {
        name = "opencollab"
    }
}

val mcProtocolLibVersion = providers.gradleProperty("mcProtocolLibVersion").orElse("1.21.11-1")

dependencies {
    // Version is overridable per Minecraft build via the mcProtocolLibVersion property (the harness
    // sets it), so it stays a dynamic coordinate rather than a fixed catalog entry.
    implementation("org.geysermc.mcprotocollib:protocol:${mcProtocolLibVersion.get()}")
    runtimeOnly(libs.org.slf4j.slf4j.simple)
}

description = "Pinned offline-mode presence client for the uSkyBlock live-server harness"

tasks.jar {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("uSkyBlock-itclient")
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("META-INF/maven/**")
    manifest {
        attributes["Main-Class"] = "us.talabrek.ultimateskyblock.itclient.PresenceClient"
        attributes["MCProtocolLib-Version"] = mcProtocolLibVersion.get()
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
