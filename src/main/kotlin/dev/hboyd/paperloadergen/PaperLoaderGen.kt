/*
 * paper-loader-gen
 * Copyright (C) 2026 Harrison Boyd
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import java.nio.file.Path

abstract class PaperLoaderGen : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        pluginManager.apply(JavaPlugin::class.java)

        val idk = configurations.dependencyScope("paperRuntime") {
            it.extendsFrom( configurations.getByName("compileOnly"))
        }

        val generatedOutputDir: Path = project.layout.buildDirectory.get().asFile.toPath()
            .resolve("generated/PaperLoaderGen/main")

        val paperLoaderGenTask: TaskProvider<PaperLoaderGenTask> = project.tasks.register("generatePaperLoader", PaperLoaderGenTask::class.java) {
            val filteredRepositories = project.repositories
                .filterIsInstance<MavenArtifactRepository>()
                .filter { repo -> !repo.url.toString().startsWith("file") }
                .filter { repo -> !repo.url.toString().contains("repo.maven.apache.org/maven2/") } // Remove central repo which is against TOS
                .associateBy { repo -> repo.url }

            it.repositories.convention(filteredRepositories.values)
            it.dependencies.convention(project.configurations.getByName("paperRuntime").dependencies)
            it.generatedOutputDir.convention(generatedOutputDir)
        }

        configurations.matching { it.name == "compileClasspath" }
            .configureEach {
                it.extendsFrom(idk)
            }

        val mainSourceSet: SourceSet = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets.getByName("main")
        mainSourceSet.java.srcDir(generatedOutputDir)
        project.tasks.named(mainSourceSet.compileJavaTaskName).configure { it.dependsOn(paperLoaderGenTask) }
    }
}