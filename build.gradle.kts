plugins {
    idea
    alias(libs.plugins.kotlin)
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.indra)
    alias(libs.plugins.indraPluginPublishing)
    alias(libs.plugins.indraLicenserSpotless)
    alias(libs.plugins.gitSimpleSemver)
}

dependencies {
    implementation(gradleApi())
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

indra {
    javaVersions {
        target(21)
    }

    github("hboyd2003", "paper-loader-gen") {
        ci(true)
        publishing(false)
    }

    publishReleasesTo("gradlePluginPortal", "https://plugins.gradle.org/m2/")
    publishReleasesTo("hboydDev", "https://repo.hboyd.dev/releases")
    publishSnapshotsTo("hboydDev", "https://repo.hboyd.dev/snapshots")

    gpl3OrLaterLicense()

    signWithKeyFromPrefixedProperties("hboyd")

    configurePublications {
        pom {
            developers {
                developer {
                    id.set("hboyd2003")
                    name.set("Harrison Boyd")
                    email.set("8950185+hboyd2003@users.noreply.github.com")
                    timezone = "America/New_York"
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file(".spotless/license_header_template.txt"))
    newLine(true)
}

indraPluginPublishing {
    plugin(
        project.name,
        "dev.hboyd.paperloadergen.PaperLoaderGen",
        "Paper Loader Gen",
        "Automatically generates Paper loader classes for Minecraft: Java Edition.",
        listOf("minecraft", "paper", "codegen")
    )
    website("https://github.com/hboyd2003/paper-loader-gen")
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        expand(mapOf("version" to version))
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
