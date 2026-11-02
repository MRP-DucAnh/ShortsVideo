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

rootProject.name = "FlowDrama"

include(":app")

include(":core:domain")

include(":core:data")

include(":core:designsystem")

include(":core:player")

include(":core:ads")

include(":feature:onboarding")

include(":feature:home")

include(":feature:search")

include(":feature:browse")

include(":feature:reels")

include(":feature:detail")

include(":feature:playback")

include(":feature:library")

include(":feature:settings")
