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
        // LiteRT-LM (litertlm-android) requires API 26+.
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

    // Local AI on-device inference via Google's LiteRT-LM runtime — the recommended
    // successor to the maintenance-mode MediaPipe LLM Inference API. Runs .litertlm
    // models (Qwen2-VL, MiniCPM, Gemma, ...). Native libraries ship inside the AAR,
    // so no NDK/CMake is needed in this build.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.14.0")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
