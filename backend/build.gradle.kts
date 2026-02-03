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

    // Οι υπόλοιπες βιβλιοθήκες σου...
    implementation("com.google.cloud:google-cloud-vertexai:1.7.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.24.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation(libs.androidx.datastore.preferences.core.jvm)
}

// Προσοχή: Στο Shadow 8.3.5 το task ονομάζεται shadowJar
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "app.MainKt"
    }
    mergeServiceFiles()
    archiveFileName.set("shadow.jar")
}