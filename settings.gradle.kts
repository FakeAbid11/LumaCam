pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*kotlin.*")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LumaCam"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:camera")
include(":feature:ai")
include(":feature:gallery")
