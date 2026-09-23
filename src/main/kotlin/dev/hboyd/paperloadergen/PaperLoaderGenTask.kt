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

import dev.hboyd.paperloadergen.artifact.SerializableDependency
import dev.hboyd.paperloadergen.artifact.SerializableExcludeRule
import dev.hboyd.paperloadergen.artifact.SerializableMavenArtifactRepository
import dev.hboyd.paperloadergen.artifact.SerializablePasswordCredentials
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.GradleInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.artifacts.repositories.AuthenticationSupportedInternal
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/**
 * Task that generates the Paper Loader class.
 */
@CacheableTask
abstract class PaperLoaderGenTask @Inject constructor(
    layout: ProjectLayout,
    private val objectFactory: ObjectFactory
) : DefaultTask() {
    init {
        group = "generate"
    }

    /**
     * The fully qualified class path of the loader.
     */
    @get:Input
    abstract val classPath: Property<String>

    /**
     * Repositories included in the loader.
     */
    @get:Input
    private val repositories: ListProperty<SerializableMavenArtifactRepository> =
        objectFactory.listProperty(SerializableMavenArtifactRepository::class.java)
            .convention(project.provider {
                project.repositories
                    .plus((project.gradle as GradleInternal).settings.dependencyResolutionManagement.repositories) // Required due to https://github.com/gradle/gradle/issues/16616
                    .filterIsInstance<MavenArtifactRepository>()
                    .filter { repo ->
                        repo.url.scheme.startsWith("http") // Only http/https repos
                                && !repo.url.host.equals("repo.maven.apache.org")  // Ignore central repo which is against TOS to use
                    }.map { it.toSerializable() }
            })

    /**
     * Dependencies included in the loader.
     */
    @get:Input
    private val dependencies: ListProperty<SerializableDependency> =
        objectFactory.listProperty(SerializableDependency::class.java)
            .convention(project.provider {
                project.configurations.getByName("paperRuntime").dependencies
                    .map { it.toSerializable() }
            })

    /**
     * Source root of the loader.
     */
    @get:OutputDirectory
    val generatedSrcRoot: DirectoryProperty = objectFactory.directoryProperty()
        .convention(layout.buildDirectory.dir("generated/sources/${this.name}/java/main"))

    /**
     * Set the repositories in the loader with [repositories].
     */
    fun setRepositories(repositories: Iterable<MavenArtifactRepository>) {
        this.repositories.set(repositories.map { it.toSerializable() })
    }

    /**
     * Set the repositories in the loader with [repositories].
     */
    fun setRepositories(repositories: Provider<out Iterable<MavenArtifactRepository>>) {
        this.repositories.set(repositories.map { repos -> repos.map { it.toSerializable() } })
    }

    /**
     * Add the [repository] to the loader.
     */
    fun addRepository(repository: MavenArtifactRepository) {
        this.repositories.add(repository.toSerializable())
    }

    /**
     * Add the repository provided by [repositoryProvider] to the loader.
     */
    fun addRepository(repositoryProvider: Provider<out MavenArtifactRepository>) {
        this.repositories.add(repositoryProvider.map { it.toSerializable() })
    }

    /**
     * Add the [repositories] to the loader.
     */
    fun addRepositories(vararg repositories: MavenArtifactRepository) {
        addRepositories(repositories.asIterable())
    }

    /**
     * Add the [repositories] to the loader.
     */
    fun addRepositories(repositories: Iterable<MavenArtifactRepository>) {
        this.repositories.addAll(repositories.map { it.toSerializable() })
    }

    /**
     * Add the repositories provided by [repositoryProviders] to the loader.
     */
    fun addRepositories(repositoryProviders: Provider<out Iterable<MavenArtifactRepository>>) {
        this.repositories.addAll(repositoryProviders.map { repos -> repos.map { it.toSerializable() } })
    }

    /**
     * Set the dependencies in the loader with [dependencies].
     */
    fun setDependencies(dependencies: Iterable<Dependency>) {
        this.dependencies.set(dependencies.map { it.toSerializable() })
    }

    /**
     * Set the dependencies in the loader with [dependencies].
     */
    fun setDependencies(dependencies: Provider<out Iterable<Dependency>>) {
        this.dependencies.set(dependencies.map { dependencies -> dependencies.map { it.toSerializable() } })
    }

    /**
     * Add the [dependency] to the loader.
     */
    fun addDependency(dependency: Dependency) {
        this.dependencies.add(dependency.toSerializable())
    }

    /**
     * Add the dependency provided by [dependencyProvider] to the loader.
     */
    fun addDependency(dependencyProvider: Provider<out Dependency>) {
        this.dependencies.add(dependencyProvider.map { it.toSerializable() })
    }

    /**
     * Add the [dependencies] to the loader.
     */
    fun addDependencies(vararg dependencies: Dependency) {
        addDependencies(dependencies.asIterable())
    }

    /**
     * Add the [dependencies] to the loader.
     */
    fun addDependencies(dependencies: Iterable<Dependency>) {
        this.dependencies.addAll(dependencies.map { it.toSerializable() })
    }

    /**
     * Add the dependencies provided by [dependencyProviders] to the loader.
     */
    fun addDependencies(dependencyProviders: Provider<out Iterable<Dependency>>) {
        this.dependencies.addAll(dependencyProviders.map { dependencies -> dependencies.map { it.toSerializable() } })
    }

    @TaskAction
    fun generate() {
        val outputFile: Path = generatedSrcRoot.get()
            .dir(classPath.get().replace('.', '/') + ".java")
            .asFile
            .toPath()

        if (dependencies.getOrNull().isNullOrEmpty()) {
            logger.warn("Received no dependencies for task ${this.name}. No Paper loader will be generated.")
            Files.deleteIfExists(outputFile) // Delete old loader
            return
        }

        Files.createDirectories(outputFile.parent)

        PrintWriter(FileWriter(outputFile.toFile())).use { writer ->
            val timestamp: String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC")).format(Clock.System.now().toJavaInstant())
            writer.write(
                """
                //
                // DO NOT EDIT - File generated by the dev.hboyd.paperloadergen Gradle plugin.
                //
                
                package ${classPath.get().substringBeforeLast('.')};
                
                import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
                import io.papermc.paper.plugin.loader.PluginLoader;
                import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
                import org.eclipse.aether.artifact.DefaultArtifact;
                import org.eclipse.aether.graph.Dependency;
                import org.eclipse.aether.graph.Exclusion;
                import org.eclipse.aether.repository.RemoteRepository;
                import org.eclipse.aether.repository.LocalRepository;
                import org.eclipse.aether.util.repository.AuthenticationBuilder;
                import org.jspecify.annotations.NonNull;
                import javax.annotation.processing.Generated;
                import java.util.List;
                
                @Generated(value = "dev.hboyd.paperloadergen.PaperLoaderGenerationTask", date = "$timestamp", comments = "Version: ${PaperLoaderGen.pluginVersion()}")
                @SuppressWarnings({"UnstableApiUsage", "unused"})
                public final class ${classPath.get().substringAfterLast('.')} implements PluginLoader {
                    @Override
                    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
                        MavenLibraryResolver resolver = new MavenLibraryResolver();
                        resolver.addRepository(new RemoteRepository.Builder("maven-central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
                """.trimIndent(),
            )

            repositories.get().forEach {
                writer.write("\n        resolver.addRepository(new RemoteRepository.Builder(\"${it.name.get()}\", \"default\", \"${it.uri.get()}\")")

                if (it.credentials.isPresent) {
                    val nameScreamingSnakeCase = it.name.get().toScreamingSnakeCase() + "_"

                    writer.write(
                        """
                        
                        .setAuthentication(new AuthenticationBuilder()
                                .addUsername(System.getenv("${nameScreamingSnakeCase + "REPO_USERNAME"}"))
                                .addPassword(System.getenv("${nameScreamingSnakeCase + "REPO_PASSWORD"}"))
                                .build())
                                        
                        """.trimIndent().prependIndent("                ")
                    )
                }

                writer.write(".build());")
            }

            writer.write("\n")

            dependencies.get().forEach {
                var exclusionsString = "null"
                val excludeRules = it.excludeRules.get()
                if (excludeRules.isNotEmpty()) {
                    exclusionsString = excludeRules.joinToString(", ", "List.of(", ")") { excludeRule ->
                        """new Exclusion("${excludeRule.group.getOrElse("*")}", "${excludeRule.module.getOrElse("*")}", "*", "*")"""
                    }
                }
                writer.write("\n        resolver.addDependency(new Dependency(new DefaultArtifact(\"${it.coordinates().get()}\"), null, null, $exclusionsString));")
            }

            writer.write(
                """
                
                
                        classpathBuilder.addLibrary(resolver);
                    }
                }
                """.trimIndent()
            )
        }
    }

    private fun MavenArtifactRepository.toSerializable(): SerializableMavenArtifactRepository {
        val serializableMavenArtifactRepository = objectFactory.newInstance(SerializableMavenArtifactRepository::class.java)

        serializableMavenArtifactRepository.name.set(name)
        serializableMavenArtifactRepository.uri.set(url)
        if (this is AuthenticationSupportedInternal) { // Calling the normal getCredentials modifies the repo with blank credentials if it doesn't already have any
            val credentialsProvider = configuredCredentials.map { credentials ->
                if (credentials is PasswordCredentials) return@map credentials.toSerializable()

                logger.warn("Repository $name is configured with a type of credential that is unsupported.")
                return@map null
            }
            serializableMavenArtifactRepository.credentials.set(credentialsProvider)
        }

        return serializableMavenArtifactRepository
    }

    private fun PasswordCredentials.toSerializable(): SerializablePasswordCredentials {
        val serializablePasswordCredentials = objectFactory.newInstance(SerializablePasswordCredentials::class.java)

        serializablePasswordCredentials.username.set(username)
        serializablePasswordCredentials.password.set(password)

        return serializablePasswordCredentials
    }

    private fun Dependency.toSerializable(): SerializableDependency {
        val serializableDependency = objectFactory.newInstance(SerializableDependency::class.java)

        serializableDependency.name.set(this.name)
        serializableDependency.group.set(this.group)
        serializableDependency.version.set(this.version)

        if (this is ModuleDependency)
            serializableDependency.excludeRules.set(this.excludeRules.map { it.toSerializable() })

        return serializableDependency
    }

    private fun ExcludeRule.toSerializable(): SerializableExcludeRule {
        val serializableExcludeRule = objectFactory.newInstance(SerializableExcludeRule::class.java)

        serializableExcludeRule.group.set(this.group)
        serializableExcludeRule.module.set(this.module)

        return serializableExcludeRule
    }

    private fun String.toScreamingSnakeCase(): String {
        if (!contains("[a-z]".toRegex())) return this

        return replace("((?<!_-)[A-Z])|-".toRegex(), "_").uppercase()
    }
}
