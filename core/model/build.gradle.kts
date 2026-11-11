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

// A globális Definition of Done a `./gradlew testDebugUnitTest` parancsot írja elő.
// Kotlin JVM modulban ilyen task nem létezik (itt a teszttask neve `test`), ezért
// azonos nevű gyűjtőtaskot veszünk fel, hogy a kötelező parancs a tiszta Kotlin
// magmodulok tesztjeit is lefuttassa, ne hagyja ki őket csendben.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Alias a `test` taskra, hogy a globális DoD parancs ezt a modult is lefedje."
    dependsOn(tasks.named("test"))
}
