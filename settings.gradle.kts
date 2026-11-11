pluginManagement {
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "KyokushinKarateTrainer"

// Alkalmazásmodul
include(":app")

// Tiszta Kotlin mag – Android-függőség nélkül (ADR-001)
include(":core:model")
include(":core:quiz")

// Android library magmodulok
include(":core:data")
include(":core:ui")

// Funkciómodulok
include(":feature:library")
include(":feature:learn")
include(":feature:quiz")
include(":feature:progress")

// Keretmodulok
include(":navigation")
include(":di")
