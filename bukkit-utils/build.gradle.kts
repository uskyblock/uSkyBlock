plugins {
    id("buildlogic.java-conventions")
    id("java-test-fixtures")
}

dependencies {
    api(projects.poUtils)
    api(libs.org.spigotmc.spigot.api)
    api(libs.net.milkbowl.vault.vaultapi)
    api(libs.net.kyori.adventure.api)
    implementation(libs.net.kyori.adventure.text.serializer.gson)
    testImplementation(libs.org.hamcrest.hamcrest)
    testImplementation(libs.org.mockito.mockito.core)
    testImplementation(libs.com.google.code.gson.gson)

    testFixturesApi(libs.org.spigotmc.spigot.api)
    testFixturesApi(libs.org.mockito.mockito.core)
    testFixturesApi(libs.org.hamcrest.hamcrest)
}

description = "bukkit-utils"

val testsJar = tasks.register<Jar>("testsJar") {
    archiveClassifier = "tests"
    from(sourceSets["test"].output)
}

val testFixturesApiElements = configurations.named("testFixturesApiElements")
val testFixturesRuntimeElements = configurations.named("testFixturesRuntimeElements")

// This removes the test-fixtures variants from the GMM and POM
publishing {
    publications.withType<MavenPublication> {
        suppressPomMetadataWarningsFor(testFixturesApiElements.get().name)
        suppressPomMetadataWarningsFor(testFixturesRuntimeElements.get().name)
    }
}

// To completely prevent them from being part of the 'java' component used for publishing
components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(testFixturesApiElements.get()) { skip() }
    withVariantsFromConfiguration(testFixturesRuntimeElements.get()) { skip() }
}
