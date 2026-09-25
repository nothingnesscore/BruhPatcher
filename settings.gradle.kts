pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        // Xposed API — used by lsposed-lgbar module (compileOnly)
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "BruhPatcher"
include(":app")
// LiquidGlassBar — standalone LSposed module for HyperOS 4 miuix app navigation injection
include(":lsposed-lgbar")
