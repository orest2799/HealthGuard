@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.healthguard"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.healthguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    /* -------------------- Networking -------------------- */
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    // Pick ONE OkHttp line; here we keep your newer alias:
    implementation(libs.okhttp.v521)
    implementation(libs.com.google.firebase.firebase.auth.ktx)
    implementation(libs.androidx.browser)
    debugImplementation(libs.logging.interceptor)

    /* -------------------- Firebase (via BoM) -------------------- */
    // Use BoM and NO versions on individual Firebase artifacts
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    /* (Remove duplicate Firebase lines coming from catalog if they pinned versions)
       If your version catalog aliases like libs.com.google.firebase.firebase.auth.ktx
       are versioned, DO NOT use them together with the BoM. Hence removed. */

    /* -------------------- Google Sign-In / Credentials -------------------- */
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.moshi.kotlin)

    /* -------------------- CameraX -------------------- */
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.concurrent.futures)

    /* -------------------- Compose / AndroidX -------------------- */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.datastore.preferences)

    /* -------------------- ML / TFLite -------------------- */
    implementation(libs.tensorflow.lite)
    implementation(libs.litert)
    implementation(libs.androidx.exifinterface)

    /* -------------------- Tests -------------------- */
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    // This should be debugImplementation, not implementation
    debugImplementation(libs.androidx.ui.test.manifest)
}


