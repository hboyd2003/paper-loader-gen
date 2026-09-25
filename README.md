# Paper Loader Gen

This Gradle plugin allows for the automatic generation of Minecraft
[Paper loader classes](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#loaders) based off of a projects
configured dependencies and repositories.

## Usage

Apply plugin

```groovy
plugins {
    id("dev.hboyd.paperloadergen").version("1.0.0")
}
```

Configure task

```groovy
tasks {
    generatePaperLoaderGen {
        classPath = "fully.qualified.path.to.generated.class"
        additionalDependencies.add("additional.dependency:1.0.0")
    }
}
```

Include dependencies by using the `paperRuntime` dependency configuration

```groovy
dependencies {
    paperRuntime("org.jspecify:jspecify:1.0.0")
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
