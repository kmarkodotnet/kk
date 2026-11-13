// Gyökér build fájl – csak a pluginek deklarálása, alkalmazás nélkül.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

// T-607: ktlint minden modulban, központilag bekötve – a szabályokat a gyökér
// `.editorconfig` adja (ktlint 1.x óta ez a kanonikus konfigurációs hely, nem a
// Gradle DSL). Minden modul kapja, mert mindegyik Kotlin forrást tartalmaz
// (a tiszta JVM modulok is, `core/model`/`core/quiz`).
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
