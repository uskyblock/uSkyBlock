import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/public")
    }

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.codemc.org/repository/maven-public")
    }

    maven {
        url = uri("https://www.uskyblock.ovh/maven/dependencies/")
    }

    maven {
        url = uri("https://www.uskyblock.ovh/maven/uskyblock/")
    }

    maven {
        url = uri("https://repo.onarandombox.com/content/groups/public/")
        content { includeGroupByRegex("""org\.mvplugins.*""") }
    }

    maven {
        url = uri("https://repo.helpch.at/releases/")
        content { includeGroup("me.clip") }
    }
}

group = "ovh.uskyblock"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withJavadocJar()
}

// Precompiled script plugins can't use the generated `libs` accessor, so reach the catalog directly.
// The JUnit test stack is uniform across every module, so it lives here rather than being copy-pasted.
val libs = the<VersionCatalogsExtension>().named("libs")
dependencies {
    "testImplementation"(libs.findLibrary("org-junit-jupiter-junit-jupiter-api").get())
    "testRuntimeOnly"(libs.findLibrary("org-junit-jupiter-junit-jupiter-engine").get())
    "testRuntimeOnly"(libs.findLibrary("org-junit-platform-junit-platform-launcher").get())
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
    repositories {
        maven {
            name = "Staging"
            url = uri(layout.buildDirectory.dir("mvn-repo"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    val standardOptions = options as StandardJavadocDocletOptions
    // Define @implNote so the Javadoc tool recognizes it
    standardOptions.tags("implNote:a:Implementation Note:")
    // 2. Suppress the "missing" check in doclint
    // "all,-missing" means: check everything EXCEPT missing comments
    standardOptions.addStringOption("Xdoclint:all,-missing", "-quiet")
}
