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
        writeGradleBuildFile(additionalDependencies = "paperRuntime(\"org.jspecify:jspecify:1.0.1\")")

        val gradleResult = executeGradleRun("build")
        gradleResult.tasks.forEach {
            assert(it.outcome != TaskOutcome.FAILED)
        }

        ZipFile(testProjectDir.resolve("build/libs/${testProjectDir.name}.jar").toFile()).use {
            assertNotNull(it.getEntry("dev/hboyd/testplugin/TestPluginLoader.class"))
        }
    }

    @Test
    fun `generated source includes maven central mirror repo`() {
        writeGradleBuildFile(repositories = "")

        val gradleResult = executeGradleRun("generatePaperLoader")
        gradleResult.tasks.forEach {
            assert(it.outcome != TaskOutcome.FAILED)
        }

        assertGeneratedSourceContainsLines(
            setOf(
                "        resolver.addRepository(new RemoteRepository.Builder(\"maven-central\", \"default\", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());"
            )
        )
    }

    @Test
    fun `generated source includes additional dependencies`() {
        writeGradleBuildFile(
            repositories = "",
            additionalDependencies = "paperRuntime(\"org.jspecify:jspecify:1.0.0\")",
            additionalGeneratePaperLoaderTaskConfig = "additionalDependencies.add(\"net.kyori:adventure-api:4.26.1\")"
        )

        val gradleResult = executeGradleRun("generatePaperLoader")
        gradleResult.tasks.forEach {
            assert(it.outcome != TaskOutcome.FAILED)
        }

        assertGeneratedSourceContainsLines(setOf("        resolver.addDependency(new Dependency(new DefaultArtifact(\"net.kyori:adventure-api:4.26.1\"), null));"))
    }

    @Test
    fun `generated source includes setting based repositories`() {
        writeGradleBuildFile(repositories = "")

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

        assertGeneratedSourceContainsLines(
            setOf(
                "        resolver.addRepository(new RemoteRepository.Builder(\"papermc-repo\", \"default\", \"https://repo.papermc.io/repository/maven-public/\").build());",
                "        resolver.addRepository(new RemoteRepository.Builder(\"hboyd-dev-repo\", \"default\", \"https://repo.hboyd.dev/snapshots/\").build());"
            )
        )
    }

    private fun writeGradleBuildFile(
        repositories: String = """
            maven {
                name = "papermc-repo"
                url = "https://repo.papermc.io/repository/maven-public/"
            }
            maven {
                name = "hboyd-dev-repo"
                url = "https://repo.hboyd.dev/snapshots/"
            }
            """.trimIndent(),
        additionalRepositories: String? = null,
        dependencies: String = "compileOnly \"io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT\"",
        additionalDependencies: String? = null,
        generatePaperLoaderTaskConfig: String = "classPath = \"dev.hboyd.testplugin.TestPluginLoader\"",
        additionalGeneratePaperLoaderTaskConfig: String? = null
    ) {
        PrintWriter(testProjectDir.resolve("build.gradle").writer()).use {
            it.format(
                """
                plugins {
                    id 'dev.hboyd.paper-loader-gen'
                }
                
                repositories {
                    %s
                    %s
                }
                
                dependencies {
                    %s
                    %s
                }
                
                tasks {
                    generatePaperLoader {
                        %s
                        %s
                    }
                }
        
                """.trimIndent(),
                repositories,
                additionalRepositories,
                dependencies,
                additionalDependencies,
                generatePaperLoaderTaskConfig,
                additionalGeneratePaperLoaderTaskConfig
            )
        }
    }

    private fun assertGeneratedSourceContainsLines(lines: Set<String>) {
        val unseenLines = lines.toMutableList()
        unseenLines.removeAll(Files.lines(testProjectDir
            .resolve("build/generated/sources/generatePaperLoader/java/main/dev/hboyd/testplugin/TestPluginLoader.java")).toList())

        assert(unseenLines.isEmpty()) {
            "Generated source did not include expected lines: ${unseenLines.joinToString("\", ", "[\"", "\"]")}"
        }
    }

    private fun executeGradleRun(task: String): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(task)
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()
            .build()
}
