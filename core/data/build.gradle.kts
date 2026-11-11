// core/data – Room perzisztencia, assets import, repository implementációk.
// SZABÁLY (ADR-001, L3/L4): a Room entitások és DAO-k `internal` láthatóságúak.
// Kifelé csak repository INTERFÉSZ és domain modell látszik.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "hu.kyokushin.trainer.core.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // A Room séma tényleges viselkedését (UNIQUE index, cascade hiánya) Robolectric
            // alatt, in-memory adatbázison mérjük – emulátor nélkül. Ehhez a unit teszt
            // JVM-nek látnia kell az Android erőforrásokat és a csomagolt manifestet.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    // A Room sémaexport a migrációk ellenőrzéséhez (DR-08).
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:quiz"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
}

tasks.withType<Test>().configureEach {
    // A tartalomimport tesztjei a valódi tudástáron is lefutnak
    // (`knowledgebase_transfer/`, 161 rekord), a teszt JVM munkakönyvtára viszont a modul
    // könyvtára. Az útvonalat ezért rendszertulajdonságként adjuk át, nem relatív `../..`
    // hivatkozással – az csendben elromlana, ha a modul máshová kerül. Ugyanaz a
    // konvenció, mint a `core/quiz`-ben. (A tartalmat az `app` assetsbe a T-601 másolja.)
    systemProperty("kyo.repoRoot", rootDir.absolutePath)
}
