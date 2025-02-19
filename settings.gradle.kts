pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://a8c-libs.s3.amazonaws.com/android")
    }
}

rootProject.name = "cis-android"
include(
    ":app",
    ":benchmark"
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")