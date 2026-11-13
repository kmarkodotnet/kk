package hu.kyokushin.trainer.core.quiz.architecture

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A [LayeringRuleDetector] szintetikus bemeneten – ez bizonyítja a task „Kész, ha"
 * pontját: **a teszt elbukik, ha valaki véletlenül behúz egy tiltott importot**, nem
 * csak azt, hogy a `core/quiz`, a `core/model` és a feature modulok mai forrása tiszta (azt a
 * [LayeringRuleTest] méri). Minden pozitív (elkap) esethez tartozik egy negatív
 * (nem akad bele a megengedett/dokumentáló kódba) eset is.
 */
class LayeringRuleDetectorTest {

    // ------------------------------------------------------------------------------
    // Tiltott import – core/model, core/quiz
    // ------------------------------------------------------------------------------

    @Test
    fun forbiddenImportsIn_androidImport_isDetected_TR07() {
        val source = """
            package hu.kyokushin.trainer.core.quiz.generator

            import android.os.Parcelable
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenImportsIn(source)).containsExactly("import android.os.Parcelable")
    }

    @Test
    fun forbiddenImportsIn_androidxRoomAndComposeImport_isDetected_TR07() {
        val source = """
            import androidx.room.Entity
            import androidx.compose.runtime.Composable
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenImportsIn(source))
            .containsExactly("import androidx.room.Entity", "import androidx.compose.runtime.Composable")
    }

    @Test
    fun forbiddenImportsIn_onlyKotlinAndCoreModelImports_isEmpty_TR07() {
        val source = """
            package hu.kyokushin.trainer.core.quiz.generator

            import hu.kyokushin.trainer.core.model.TechniqueId
            import kotlin.random.Random
            import kotlinx.serialization.Serializable
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenImportsIn(source)).isEmpty()
    }

    // ------------------------------------------------------------------------------
    // Tiltott rendszeróra-/véletlenhívás – core/model, core/quiz
    // ------------------------------------------------------------------------------

    @Test
    fun forbiddenRuntimeCallsIn_directRandomDefaultCall_isDetected_TR07_NFR07() {
        val source = """
            class Shuffler {
                fun shuffle() = Random.Default.nextInt()
            }
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenRuntimeCallsIn(source)).containsExactly("Random.Default")
    }

    @Test
    fun forbiddenRuntimeCallsIn_allFourForbiddenCalls_areDetected_TR07_NFR07() {
        val source = """
            class Clock {
                fun now() = Instant.now()
                fun millis() = System.currentTimeMillis()
                fun seed() = Random.Default.nextLong()
                fun chance() = Math.random()
            }
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenRuntimeCallsIn(source))
            .containsExactly("Random.Default", "System.currentTimeMillis", "Instant.now", "Math.random")
    }

    @Test
    fun forbiddenRuntimeCallsIn_mentionOnlyInKdocComment_isIgnored_TR07_NFR07() {
        // Ugyanaz a mintázat, mint a valódi `QuizGenerator.kt`/`SeededShuffle.kt`: a KDoc
        // dokumentálja, hogy a hívás NINCS a kódban – ez nem minősül találatnak.
        val source = """
            /**
             * A modulban nincs `Random.Default`, nincs `Instant.now()`, nincs
             * `System.currentTimeMillis()` és nincs `Math.random()` – a seedet és az
             * órát mindig a hívó adja át.
             */
            class DefaultQuizGenerator
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenRuntimeCallsIn(source)).isEmpty()
    }

    @Test
    fun forbiddenRuntimeCallsIn_mentionOnlyInLineComment_isIgnored_TR07_NFR07() {
        val source = """
            class DefaultQuizGenerator {
                // nincs itt Random.Default, a seedet a kérés hordozza
                fun generate(seed: Long) = Random(seed)
            }
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenRuntimeCallsIn(source)).isEmpty()
    }

    // ------------------------------------------------------------------------------
    // Room entitás/DAO/DTO import – feature/*
    // ------------------------------------------------------------------------------

    @Test
    fun forbiddenDataLayerImportsIn_roomAnnotationImport_isDetected_TR07_NFR03() {
        val source = "import androidx.room.Dao"

        assertThat(LayeringRuleDetector.forbiddenDataLayerImportsIn(source)).containsExactly("import androidx.room.Dao")
    }

    @Test
    fun forbiddenDataLayerImportsIn_entityDaoAndDtoTypeImport_isDetected_TR07_NFR03() {
        val source = """
            import hu.kyokushin.trainer.core.data.db.TechniqueEntity
            import hu.kyokushin.trainer.core.data.db.TechniqueDao
            import hu.kyokushin.trainer.core.data.content.KnowledgeDto
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenDataLayerImportsIn(source))
            .containsExactly(
                "import hu.kyokushin.trainer.core.data.db.TechniqueEntity",
                "import hu.kyokushin.trainer.core.data.db.TechniqueDao",
                "import hu.kyokushin.trainer.core.data.content.KnowledgeDto",
            )
    }

    @Test
    fun forbiddenDataLayerImportsIn_repositoryAndDomainTypeImport_isEmpty_TR07_NFR03() {
        val source = """
            import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
            import hu.kyokushin.trainer.core.model.Technique
            import hu.kyokushin.trainer.core.model.TechniqueId
        """.trimIndent()

        assertThat(LayeringRuleDetector.forbiddenDataLayerImportsIn(source)).isEmpty()
    }
}
