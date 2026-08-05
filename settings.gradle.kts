pluginManagement {
    includeBuild("build-logic")
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarSystemUI"
include(":app")
include(":core:common")
include(":core:platform")
include(":feature:climate:domain")
include(":feature:climate:data")
include(":feature:climate:presentation")
include(":feature:launcher:domain")
include(":feature:launcher:data")
include(":feature:launcher:presentation")
include(":feature:notifications:domain")
include(":feature:notifications:data")
include(":feature:notifications:presentation")
include(":feature:systembar:domain")
include(":feature:systembar:data")
include(":feature:systembar:presentation")
 
