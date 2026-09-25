# Paper Loader Gen

This Gradle plugin allows for the automatic generation of Minecraft
[Paper loader classes](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#loaders) based off of a projects
configured dependencies and repositories.

## Usage

### Apply plugin

```groovy
plugins {
    id("dev.hboyd.paperloadergen").version("1.0.0")
}
```

### Configure the task

```groovy
tasks {
    generatePaperLoader {
        // Required: fully qualified name of the generated loader class
        classPath = "com.example.myplugin.MyPluginLoader"

        // Change where the source is generated to
        // Defaults to "build/generated/sources/<task name>/java/main"
        generatedSrcRoot = layout.buildDirectory.dir("generated/sources/paperLoader")

        // Replace all repositories
        setRepositories(project.repositories.withType(MavenArtifactRepository).matching { it.name == "papermc-repo" })

        // Or add a repository
        addRepository(project.repositories.maven {
            name = "extra-repo"
            url = "https://repo.example.com/releases"
            mavenContent { releasesOnly() } // Content type is also respected
        })

        // Replace the paperRuntime dependencies with another configuration's dependencies
        setDependencies(configurations.named("myLibraries").map { it.dependencies })

        // Or add a dependency
        addDependency(project.dependencies.create("org.jspecify:jspecify:1.0.0"))
    }
}
```

By default, the loader includes every HTTP(S) Maven repository declared in the project or settings (excluding maven
central), and every dependency declared in the `paperRuntime` scope. Maven Central is always included through Paper's
default mirror.

If the task ends up with no dependencies, no loader is generated.

Include dependencies by using the `paperRuntime` dependency configuration. Repositories already declared in the project,
or in `settings.gradle`'s `dependencyResolutionManagement`, are picked up automatically, so there's usually nothing else
to configure.

```groovy
dependencies {
    paperRuntime("org.jspecify:jspecify:1.0.0")

    // Exclusions are respected
    paperRuntime("com.example:some-library:1.0.0") {
        exclude group: "com.example", module: "unwanted-transitive-dependency"
    }
}
```

Repositories that require password credentials are also supported. The generated loader pulls the credentials from the
environment. The environment variable are based off of the repository name/id converted into snake case and appended
with either `_REPO_USERNAME` or `_REPO_PASSWORD` for the username and password respectively.

```groovy
repositories {
    maven {
        name = "example-private"
        url = "https://repo.example.com/private"
        credentials {
            // Loader reads these from the EXAMPLE_PRIVATE_REPO_USERNAME and
            // EXAMPLE_PRIVATE_REPO_PASSWORD environment variables at runtime
            username = "placeholder"
            password = "placeholder"
        }
    }
}
```

## Versioning

Versions follow the [SemVer 2.0.0](http://semver.org/) versioning standard. For the versions available, see the
[tags on this repository](https://github.com/hboyd2003/paper-loader-gen/tags).

## Authors

* **Harrison Boyd** – *Initial work* - [Hboyd2003](https://github.com/hboyd2003)

See also the list of [contributors](https://github.com/hboyd2003/paper-loader-gen/contributors) who participated in this
project.

## License

This project is licensed under the LGPLv3 License – see the [LICENSE.md](LICENSE.md) file for details
