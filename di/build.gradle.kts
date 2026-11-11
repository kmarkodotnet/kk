// di – Hilt kötések központilag (ADR-001).
// Itt kötődik: repository interfész -> implementáció, Room, QuizGenerator,
// KnowledgeValidator, Clock és a seedelt RNG (NFR-07).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "hu.kyokushin.trainer.di"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // `api`, nem `implementation`: a `KyokushinGraph` belépési pont a `core/data` és a
    // `core/quiz` publikus típusait adja vissza (`TechniqueRepository`, `ContentImporter`,
    // `QuizGenerator`, `KnowledgeValidator`), és a dispatcher-qualifierek is a
    // `core/data`-ból jönnek. Ezek a típusok a modul publikus szerződésének részei.
    api(project(":core:model"))
    api(project(":core:quiz"))
    api(project(":core:data"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // `api`: a `KyokushinGraph` dispatcher-kötései `CoroutineDispatcher`-t adnak vissza.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
