package hu.kyokushin.trainer.core.quiz.architecture

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * TR-07: a rétegszabály (ADR-001, NFR-03) és a determinizmus (NFR-07) **gépi**
 * ellenőrzése, a valódi forrásfákon.
 *
 * A `core/model` és a `core/quiz` Kotlin JVM modul volta (lásd a két modul
 * `build.gradle.kts`-ét) már fordítási időben kizárja az `android.*` importot – ez a
 * teszt a maradék, fordítás által nem elkapott eseteket fedi le (lásd a task „Figyelj
 * rá" pontját):
 *
 * 1. tiltott import (`android.`/`androidx.`/`room`/`compose`) a két tiszta Kotlin
 *    modulban – akkor is elkapja, ha valaki egy jövőbeli Gradle-átalakítással Android
 *    modullá tenné az egyiket, és így a fordítási idejű védelem megszűnne;
 * 2. közvetlen rendszeróra-/véletlenhívás (`Random.Default`, `System.currentTimeMillis`,
 *    `Instant.now`, `Math.random`) ugyanott (NFR-07) – a `Clock`/`Random` mindig
 *    paraméterként/kötésként érkezik, a két modulnak sosincs saját forrása;
 * 3. Room entitás-/DAO-/DTO-típus importja a feature modulokban – a `core/data`
 *    Room-típusai `internal`-ok (lásd `docs/CONVENTIONS.md` 1. szakasz), tehát ez már a
 *    Kotlin modul-láthatóság miatt sem fordulna le, de ez a teszt forráskód-szinten,
 *    olvashatóan jelzi a problémát, mielőtt a fordító egyáltalán nekiállna.
 *
 * A szabálylogika ([LayeringRuleDetector]) szintetikus bemeneten való, önálló
 * bizonyítását lásd [LayeringRuleDetectorTest] – az bizonyítja, hogy ez a teszt tényleg
 * **elbukna** egy tiltott importtal/hívással, nem csak azt, hogy a mai forrás tiszta.
 *
 * A fájlokat a `kyo.repoRoot` rendszertulajdonságon (`core/quiz/build.gradle.kts`,
 * lásd a `RealKnowledgeBaseFixture`-nél is alkalmazott mintát) keresztül éri el, mert a
 * teszt JVM munkakönyvtára a modul saját könyvtára, nem a repó gyökere.
 */
class LayeringRuleTest {
    @Test
    fun coreModelAndCoreQuiz_haveNoForbiddenImport_TR07_NFR03() {
        val violations = sourceFiles(CORE_MODULES).flatMap { file ->
            LayeringRuleDetector.forbiddenImportsIn(file.readText())
                .map { importLine -> "${file.relativeTo(repoRoot())}: $importLine" }
        }

        // A hibalista a hibaüzenet része: ha elbukik, látni kell, melyik fájl melyik
        // importja a gond.
        assertThat(violations).isEmpty()
    }

    @Test
    fun coreModelAndCoreQuiz_haveNoDirectClockOrRandomCall_TR07_NFR07() {
        val violations = sourceFiles(CORE_MODULES).flatMap { file ->
            LayeringRuleDetector.forbiddenRuntimeCallsIn(file.readText())
                .map { call -> "${file.relativeTo(repoRoot())}: $call" }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun featureModules_doNotImportRoomEntityDaoOrDtoType_TR07_NFR03() {
        val violations = sourceFiles(FEATURE_MODULES).flatMap { file ->
            LayeringRuleDetector.forbiddenDataLayerImportsIn(file.readText())
                .map { importLine -> "${file.relativeTo(repoRoot())}: $importLine" }
        }

        assertThat(violations).isEmpty()
    }

    private fun sourceFiles(modules: List<String>): List<File> =
        LayeringRuleDetector.mainSourceFiles(repoRoot(), modules)

    private fun repoRoot(): File {
        val repoRoot = requireNotNull(System.getProperty("kyo.repoRoot")) {
            "A kyo.repoRoot rendszertulajdonság nincs beállítva – lásd core/quiz/build.gradle.kts."
        }
        return File(repoRoot)
    }

    private companion object {
        val CORE_MODULES = listOf("core/model", "core/quiz")
        val FEATURE_MODULES = listOf("feature/learn", "feature/library", "feature/progress", "feature/quiz")
    }
}
