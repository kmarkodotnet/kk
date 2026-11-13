// app – alkalmazásmodul: Application, MainActivity, téma, manifest, assets.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "hu.kyokushin.trainer"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "hu.kyokushin.trainer"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Offline app: nincs INTERNET permission (NFR-01, TR-08)
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
            // A `HiltGraphTest` Robolectric alatt felépíti a teljes Hilt gráfot: ehhez a
            // teszt JVM-nek látnia kell az Android erőforrásokat és a csomagolt
            // manifestet (ugyanaz a konvenció, mint a `core/data`-ban).
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Minden könyvtármodul (`feature/*`, `core/*`, `navigation`) lint-találata is
        // bekerül az `app` összevont jelentésébe – ez az egyetlen modul, amely minden
        // másikra függ, tehát ez a reális "teljes app" nézet (T-607, NFR-11).
        checkDependencies = true

        // A `local.properties` gépspecifikus, `.gitignore`-olt fájl (lásd a fájl saját
        // megjegyzését és docs/CONVENTIONS.md 3.5. szakasz "local.properties útvonal"
        // sorát): a `sdk.dir` szándékosan PERJELLEL írt útvonalat tartalmaz, mert az
        // escape-elt (`\\`) alak a `SdkLocator`-t egy VALÓDI `IOException`-nel buktatja
        // Windows alatt. A "PropertyEscape" lint-ellenőrzés ugyanerre a sorra egy
        // MÁSIK, a Java properties-fájlformátum szerint technikailag helytálló, de itt
        // lényegtelen panaszt tesz (a kettőspont escape-elését kéri) egy fájlra, amely
        // sosem kerül verziókezelésbe, és amelynek a tartalmát minden fejlesztői gépen
        // a build környezet (nem a projekt) állítja elő. Elnyomva, hogy a build ne
        // bukjon egy nem-commitolt, gépspecifikus fájl miatt.
        disable += "PropertyEscape"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:quiz"))
    implementation(project(":navigation"))
    implementation(project(":di"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    // A Hilt gráf futásidejű ellenőrzése emulátor nélkül (T-203, DoD: „az app elindul, a
    // Hilt gráf felépül"). A `kspTest` kell, hogy a `HiltTestApplication` komponense a
    // teszt source setben is legenerálódjon.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
