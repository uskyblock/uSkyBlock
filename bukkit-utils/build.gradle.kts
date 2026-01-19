plugins {
    id("buildlogic.java-conventions")
    id("java-test-fixtures")
}

dependencies {
    api(project(":po-utils"))
    api(libs.org.spigotmc.spigot.api)
    api(libs.net.milkbowl.vault.vaultapi)
    testImplementation(libs.org.hamcrest.hamcrest.core)
    testImplementation(libs.org.hamcrest.hamcrest.library)
    testImplementation(libs.junit.junit)
    testImplementation(libs.org.mockito.mockito.core)
    testImplementation(libs.com.google.code.gson.gson)

    testFixturesApi(libs.org.spigotmc.spigot.api)
    testFixturesApi(libs.org.mockito.mockito.core)
    testFixturesApi(libs.org.hamcrest.hamcrest.library)
}

description = "bukkit-utils"

val testsJar by tasks.registering(Jar::class) {
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
