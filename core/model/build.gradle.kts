// core/model – tiszta Kotlin domain modellek.
// SZABÁLY (ADR-001, NFR-03): ebben a modulban nincs Android, Room, Compose vagy
// szerializációs függőség. Ez Kotlin JVM modul, ezért az android.* import nem is fordul le.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
