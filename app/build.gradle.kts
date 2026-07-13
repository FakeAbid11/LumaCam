import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.3.1")
    }
}

// Signing material is NEVER committed. It is supplied either by a local, git-ignored
// keystore.properties (developer machine) or by environment variables exported from
// GitHub Secrets in CI (KEYSTORE_PATH/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD). When
// neither is present the release build is simply left unsigned so CI can still run
// assembleRelease as an R8/keep-rule compile-check.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}
fun signingValue(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.lumacam.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lumacam.app"
        // LiteRT-LM (litertlm-android) requires API 26+.
        minSdk = 26
        targetSdk = 34
        // Monotonic in CI (base + run number); falls back to 1 for local builds.
        versionCode = 1 + (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0)
        // Overridable by the release workflow (derived from the git tag).
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("storeFile", "KEYSTORE_PATH")
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = signingValue("storePassword", "STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign only when signing material resolved; otherwise leave unsigned so
            // the CI compile-check of the minified release still succeeds.
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Lint is run in CI (lintDebug) and is now a real gate: any lint *error*
    // fails the pipeline (warnings do not). test sources are excluded to avoid
    // noise from sample code.
    lint {
        abortOnError = true
        checkReleaseBuilds = false
        ignoreTestSources = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:camera"))
    implementation(libs.androidx.camera.view)
    implementation(project(":feature:ai"))
    implementation(project(":feature:gallery"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.coroutines)

    // Cloud AI key storage — encrypted at rest, never plaintext.
    implementation(libs.androidx.security.crypto)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
