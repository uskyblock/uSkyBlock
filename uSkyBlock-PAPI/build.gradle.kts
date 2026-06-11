plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":uSkyBlock-Core"))
    compileOnly(libs.me.clip.placeholderapi)
    compileOnly(libs.org.spigotmc.spigot.api)
    compileOnly(libs.net.kyori.adventure.text.serializer.legacy)

    testImplementation(libs.me.clip.placeholderapi)
    testImplementation(libs.org.spigotmc.spigot.api)
    testImplementation(libs.net.kyori.adventure.text.serializer.legacy)
    testImplementation(libs.org.junit.jupiter.junit.jupiter.api)
    testRuntimeOnly(libs.org.junit.jupiter.junit.jupiter.engine)
    testRuntimeOnly(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.org.mockito.mockito.core)
    testImplementation(libs.org.hamcrest.hamcrest)
}

description = "uSkyBlock-PAPI"

java {
    withJavadocJar()
}
