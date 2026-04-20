@file:Suppress("DEPRECATION")

import java.io.FileInputStream
import java.util.Properties


plugins {
    // 1. Always use 'alias' for catalog plugins
    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)

}

android {
    namespace = "com.example.healthguard"
    compileSdk = 36 // Required for Gradle 9/AGP 9 compatibility

    defaultConfig {
        applicationId = "com.example.healthguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secretsFile = rootProject.file("secrets.properties")
        val secrets = Properties()
        if (secretsFile.exists()) {
            FileInputStream(secretsFile).use { fis ->
                secrets.load(fis)
            }
        }
        val geminiKey = secrets.getProperty("GEMINI_API_KEY", "")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    //kotlin {
    //    compilerOptions {
    //        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    //         freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    //      }
//    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += listOf("tflite")
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.protolite.well.known.types)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.appcompat)
    val room_version = "2.6.1"
    // 2. Core AndroidX - Accessors mapped from your TOML
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.browser)
    implementation("io.noties.markwon:core:4.6.2")

    // 3. Firebase (BOM 34.4.0)
    implementation(platform(libs.firebase.bom))
    // Removed -ktx: Modern Firebase BoM (34.0+) includes Kotlin support in main modules
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-functions")
    implementation(libs.firebase.dataconnect)
    implementation(libs.moshi.kotlin)
    implementation("com.google.guava:guava:33.5.0-android")
    // Other existing dependencies
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    // 4. Gemini & AI
    implementation(libs.generativeai)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.compose.material.icons.extended)
    // 5. Compose - Use aliases exactly as defined in your [libraries] section
    implementation(platform(libs.androidx.compose.bom))
    // If your TOML has 'androidx-ui', use 'libs.androidx.ui'
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    // 6. Networking & Tools
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor.v500alpha14)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation("com.google.code.gson:gson:2.13.2")
    // 7. CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)
    implementation(libs.googleid)
    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0")
    implementation(libs.androidx.compose.foundation)
    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Use 'ksp' if you have the KSP plugin, otherwise use 'kapt'

}