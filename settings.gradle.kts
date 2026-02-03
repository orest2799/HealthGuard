pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // ΑΛΛΑΓΗ: Επιτρέπουμε στα sub-projects (backend) να έχουν δικά τους repositories αν χρειαστεί
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "HealthGuard"
include(":app", ":backend")