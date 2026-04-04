plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "com.example"
version = "1.0-SNAPSHOT"
repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.status.pages)

    // ✅ Google Cloud BOM (κλειδώνει σε υπαρκτές εκδόσεις στο Maven Central)
    implementation(platform("com.google.cloud:libraries-bom:26.45.0"))

    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation("com.google.cloud:google-cloud-firestore") // ✅ χωρίς version

    implementation("com.google.auth:google-auth-library-oauth2-http:1.24.1")
    implementation("com.google.cloud:google-cloud-vertexai:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation(libs.androidx.datastore.preferences.core.jvm)
    implementation(libs.androidx.room.common.jvm)
}



tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "app.MainKt"
    }
    mergeServiceFiles()
    archiveFileName.set("shadow.jar")
}