# Paper Loader Gen

This plugin allows for the automatic generation of Minecraft [Paper loader classes](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#loaders)
based off of a projects configured dependencies and repositories.

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