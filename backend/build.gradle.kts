plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "8.3.1"
}


kotlin {
    jvmToolchain(17)
}

val ktorVersion = "2.3.12"

dependencies {
    // Ktor (pick one engine; you only need CIO OR Netty)
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // HTTP clients / JSON helpers used by your providers
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    // Google Cloud Vision (only if you actually call Vision from backend)
    implementation("com.google.cloud:google-cloud-vision:3.44.0")
}

application {
    // Fully-qualified name of main() in your Ktor app
    mainClass.set("backend.ServerKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("healthguard-backend")
    archiveClassifier.set("all")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "backend.ServerKt"
    }
}
