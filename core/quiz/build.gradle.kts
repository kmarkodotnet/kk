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

// A globális Definition of Done a `./gradlew testDebugUnitTest` parancsot írja elő.
// Kotlin JVM modulban ilyen task nem létezik (itt a teszttask neve `test`), ezért
// azonos nevű gyűjtőtaskot veszünk fel – e nélkül a kötelező DoD-parancs csendben
// kihagyná ennek a modulnak a tesztjeit.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Alias a `test` taskra, hogy a globális DoD parancs ezt a modult is lefedje."
    dependsOn(tasks.named("test"))
}
