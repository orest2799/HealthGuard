plugins {
    kotlin("jvm")
    application
   // id("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()
    google()
}

kotlin { jvmToolchain(17) }

application {
    mainClass.set("app.ServerKt")
}

dependencies {
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
    implementation(libs.google.cloud.firestore)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.content.negotiation)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.logback.classic)
    implementation(libs.dotenv.kotlin)
    // only if actually used in backend:
     implementation(libs.okhttp)
     implementation(libs.json)
     implementation(libs.google.cloud.vision)
}

