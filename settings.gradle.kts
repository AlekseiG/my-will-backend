pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // Optional, but good practice
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "MyWill"
include("app")
include("client")
include("androidApp")
