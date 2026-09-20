dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.hboyd.dev/releases/") {
            name = "hboyd-dev-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.hboyd.dev/snapshots/") {
            name = "hboyd-dev-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.hboyd.dev/releases/") {
            name = "hboyd-dev-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.hboyd.dev/snapshots/") {
            name = "hboyd-dev-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}
