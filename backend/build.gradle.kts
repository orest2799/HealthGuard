plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    archiveClassifier.set("all")
    manifest {
        attributes(mapOf("Main-Class" to "app.MainKt"))
    }
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn("shadowJar")
}

dependencies {
    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.default.headers)

    // JSON/Jackson
    implementation(libs.jackson.module.kotlin)
    implementation(libs.json)

    // HTTP client (Gemini + any calls)
    implementation(libs.okhttp)

    // Google Cloud Vision (keep ONE)
    implementation(libs.google.cloud.vision.v3780)

    // Firestore (if you really use it)
    implementation(libs.google.cloud.firestore)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.logback.classic)

    // HTML parsing (keep ONE)
    implementation(libs.jsoup)

    // If you really use these in backend:
    implementation(libs.kmongo.coroutine)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.dotenv.kotlin)

    testImplementation(kotlin("test"))
}
