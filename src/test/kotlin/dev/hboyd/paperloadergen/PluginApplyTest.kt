/*
 * paper-loader-gen
 * Copyright (c) 2026 Harrison Boyd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.hboyd.paperloadergen

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PluginApplyTest {
    @field:TempDir(cleanup = CleanupMode.NEVER)
    lateinit var testProjectDir: Path

    val baseGradleBuild: String =
        """
        plugins {
            id 'dev.hboyd.paper-loader-gen'
        }
        
        repositories {
            maven {
                name = "papermc-repo"
                url = "https://repo.papermc.io/repository/maven-public/"
            }
            maven {
                name = "hboyd-dev-repo"
                url = "https://repo.hboyd.dev/snapshots/"
            }
        }
        
        dependencies {
            compileOnly "io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT"
            %s
        }
        
        tasks {
            generatePaperLoader {
                classPath = "dev.hboyd.testplugin.TestPluginLoader"
                %s
            }
        }

    """.trimIndent()

    @BeforeEach
    fun copyProjectToTempDir() {
        Path.of("src/test/resources/TestProject").copyToRecursively(
            testProjectDir,
            { _, _, _ -> OnErrorResult.TERMINATE },
            true,
            overwrite = true
        )
    }

    // TODO: More tests

    @Test
    fun `project compiles with generated source test`() {
        PrintWriter(testProjectDir.resolve("build.gradle").writer()).use {
            it.format(baseGradleBuild,
                "paperRuntime(\"org.jspecify:jspecify:1.0.1\")", "")
        }

        val gradleResult = executeGradleRun("build")
        gradleResult.tasks.forEach {
            assert(it.outcome != TaskOutcome.FAILED)
        }

        ZipFile(testProjectDir.resolve("build/libs/${testProjectDir.name}.jar").toFile()).use {
            assertNotNull(it.getEntry("dev/hboyd/testplugin/TestPluginLoader.class"))
        }
    }

    @Test
    fun `generated source includes additional dependencies`() {
        PrintWriter(testProjectDir.resolve("build.gradle").writer()).use {
            it.format(baseGradleBuild,
                    """
                    paperRuntime("org.jspecify:jspecify:1.0.0")
                """.trimIndent(),
                    """
                    additionalDependencies.add("net.kyori:adventure-api:4.26.1")
                """.trimIndent())
        }

        val gradleResult = executeGradleRun("generatePaperLoader")
        gradleResult.tasks.forEach {
            assert(it.outcome != TaskOutcome.FAILED)
        }

        assert(Files.lines(testProjectDir.resolve("build/generated/PaperLoaderGen/main/dev/hboyd/testplugin/TestPluginLoader.java"))
            .filter { it.contains("        resolver.addDependency(new Dependency(new DefaultArtifact(\"net.kyori:adventure-api:4.26.1\"), null));") }
            .count().toInt() == 1)

    }

    @Test
    fun `generated source includes setting based repositories`() {
        PrintWriter(testProjectDir.resolve("build.gradle").writer()).use {
            it.write(
                """
                plugins {
                    id 'dev.hboyd.paper-loader-gen'
                }
                
                dependencies {
                    compileOnly "io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT"
                    paperRuntime("org.jspecify:jspecify:1.0.0")
                }
                
                tasks {
                    generatePaperLoader {
                        classPath = "dev.hboyd.testplugin.TestPluginLoader"
                    }
                }
        
            """.trimIndent())
        }

        PrintWriter(testProjectDir.resolve("settings.gradle").writer()).use {
            it.write(
                """
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        mavenCentral()
                        maven {
                            name = "papermc-repo"
                            url = "https://repo.papermc.io/repository/maven-public/"
                        }
                        maven {
                            name = "hboyd-dev-repo"
                            url = "https://repo.hboyd.dev/snapshots/"
                        }
                    }
                }
            """.trimIndent())
        }

        val gradleResult = executeGradleRun("generatePaperLoader")
        gradleResult.tasks.forEach {
            assert(it.outcome != TaskOutcome.FAILED)
        }

        assert(Files.lines(testProjectDir.resolve("build/generated/PaperLoaderGen/main/dev/hboyd/testplugin/TestPluginLoader.java"))
            .filter { it.contains("        resolver.addDependency(new Dependency(new DefaultArtifact(\"org.jspecify:jspecify:1.0.0\"), null));") }
            .count().toInt() == 1)

    }

    private fun executeGradleRun(task: String): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(task)
            .withPluginClasspath()
            .forwardOutput()
            .build()
}
