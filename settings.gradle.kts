pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    // Only declare VERSIONS here. Do NOT apply plugins here.
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "HealthGuard"
include(":backend", ":app")

