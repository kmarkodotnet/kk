// core/ui – megosztott Compose komponensek (jelölők, üres állapotok, illusztráció).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "hu.kyokushin.trainer.core.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Az akadálymentes leírások tesztje a TÉNYLEGES `strings.xml` fölött fut
            // (NFR-04): ehhez a teszt JVM-nek látnia kell az Android erőforrásokat.
            // Ugyanaz a konvenció, mint a `core/data`-ban és az `app`-ban.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))

    implementation(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.material3)
    api(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    // A képdekódolás `Dispatchers.IO`-n fut, nem a kompozícióban – explicit függőség,
    // nem a Compose runtime tranzitív coroutines-ára támaszkodunk.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
