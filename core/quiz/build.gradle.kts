// core/quiz – tiszta Kotlin kvízmotor és tudástár-validátor.
// SZABÁLY (ADR-001): csak a core:model-re függ. Nincs Android, nincs Room, nincs core:data.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
