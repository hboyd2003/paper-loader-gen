import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.pluginPublish)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

val pluginId = (project.group as String) + "." + project.name

gradlePlugin {
    website = "https://github.com/hboyd2003/paper-loader-gen"
    vcsUrl = "https://github.com/hboyd2003/paper-loader-gen"

    plugins {
        create(pluginId) {
            id = pluginId
            displayName = "Paper Loader Gen"
            description = "Gradle plugin to automatically generate Minecraft Paper loader classes."
            implementationClass = "dev.hboyd.paperloadergen.PaperLoaderGen"
            tags.set(listOf("minecraft", "paper", "codegen"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}
