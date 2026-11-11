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
    // CSAK TESZT: a TR-01 készlet a `knowledgebase_transfer/` valódi 161 rekordján is
    // lefut, és ahhoz JSON-t kell olvasni. A `main` forráskészlet továbbra sem lát
    // szerializációt – a validátor bemenete `KnowledgeInput`, nem fájl (ADR-001).
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.withType<Test>().configureEach {
    // A valódi tartalom a repó gyökerében él (`knowledgebase_transfer/`), a teszt JVM
    // munkakönyvtára viszont a modul könyvtára. Az útvonalat ezért rendszertulajdonságként
    // adjuk át, nem relatív `../..` hivatkozással – az csendben elromlana, ha a modul
    // máshová kerül. (A tartalmat a T-601 másolja az `app` assetsbe.)
    systemProperty("kyo.repoRoot", rootDir.absolutePath)
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
