plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.3.1")
    }
}

android {
    namespace = "com.lumacam.feature.ai"
    compileSdk = 34

    defaultConfig {
        // MediaPipe's LLM Inference (tasks-genai) AAR requires API 26.
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.coroutines)

    // Luma Vision (PRD §4 Tier 2) — bundled, offline ML Kit detectors.
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.`object`.detection)
    implementation(libs.mlkit.image.labeling)

    // Cloud AI (PRD §4 Tier 4) — REST clients + JSON parsing for vision providers.
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.kotlinx.serialization.json)

    // Local AI on-device inference (MediaPipe LLM Inference API / LiteRT GenAI).
    // Native libraries ship inside the AAR — no NDK/CMake needed in this build.
    implementation("com.google.mediapipe:tasks-genai:0.10.27")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
