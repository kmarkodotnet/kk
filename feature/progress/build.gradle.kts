// feature/progress – Compose UI + ViewModel.
// SZABÁLY (ADR-001, L3): itt NEM lehet Room entitás, DAO, DTO vagy kotlinx.serialization típus.
// Csak domain modell (core:model), repository interfész (core:data api) és core:quiz.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "hu.kyokushin.trainer.feature.progress"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // `BuildConfig.DEBUG` dönti el a „Tartalomdiagnosztika" menüpont láthatóságát
        // (FR-32, T-508) – ugyanaz a minta, mint a `feature/library` „Diagnosztika"
        // gombjánál (T-210): kizárólag ehhez kell, nem a tartalmi logikának.
        buildConfig = true
    }

    testOptions {
        unitTests {
            // A „Gyakorlást igénylő" tétel akadálymentes leírása a TÉNYLEGES `strings.xml`
            // fölött ellenőrzött (NFR-02, NFR-04, T-504) – ehhez a teszt JVM-nek látnia
            // kell az Android erőforrásokat. Ugyanaz a konvenció, mint a `feature/library`-ban.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:quiz"))
    implementation(project(":core:data"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
