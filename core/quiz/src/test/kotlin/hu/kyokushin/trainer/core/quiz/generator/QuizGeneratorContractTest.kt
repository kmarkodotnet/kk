package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import org.junit.Test

/**
 * A [QuizGenerator] interfész szerződése – implementáció **nélkül** (T-107).
 *
 * A tényleges algoritmus a T-108…T-110 tárgya. Amit itt bizonyítunk: a szerződés
 * teljesíthető tiszta Kotlinban, kivétel nélkül, és a `dryRun`/`generate` egyezési
 * követelmény ellenőrizhető formában van kimondva. Az alábbi minimális implementáció
 * **nem** a motor: egyetlen dolgot tesz, azt, amit a szerződés elégtelen adatra előír –
 * üres, de megindokolt tervet ad.
 */
class QuizGeneratorContractTest {

    /**
     * A szerződés legszűkebb teljesítése: nincs használható jelölt, tehát nincs kérdés.
     * Ez a „degenerált, de érvényes" eset, amit a UI-nak kezelnie kell (FR-22).
     */
    private class NothingAvailableGenerator : QuizGenerator {

        override fun generate(request: QuizRequest): QuizPlan = QuizPlan(
            questions = emptyList(),
            skippedTypes = skipReasons(request),
            shortfall = Shortfall(
                requestedCount = request.requestedCount,
                actualCount = 0,
                reason = ShortfallReason.ALL_TYPES_SKIPPED,
            ),
        )

        override fun dryRun(request: QuizRequest): QuizReadiness = QuizReadiness(
            requestedCount = request.requestedCount,
            expectedQuestionCount = 0,
            availableByType = emptyMap(),
            skippedTypes = skipReasons(request),
            expectedShortfall = Shortfall(
                requestedCount = request.requestedCount,
                actualCount = 0,
                reason = ShortfallReason.ALL_TYPES_SKIPPED,
            ),
        )

        private fun skipReasons(request: QuizRequest): Map<QuestionType, SkipReason> =
            QuestionType.entries.associateWith { type ->
                if (type in request.enabledTypes) {
                    SkipReason.NOT_ENOUGH_CANDIDATES
                } else {
                    SkipReason.TYPE_DISABLED
                }
            }
    }

    private val generator: QuizGenerator = NothingAvailableGenerator()

    private fun request(requestedCount: Int = 10) = QuizRequest(
        candidates = QuizCandidateSet(),
        enabledTypes = setOf(QuestionType.JP_TO_HU, QuestionType.HU_TO_JP),
        requestedCount = requestedCount,
        seed = 1234L,
        datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    )

    @Test
    fun generator_returnsExplainedPlanInsteadOfThrowing_FR22() {
        val plan = generator.generate(request())

        assertThat(plan.canStart).isFalse()
        assertThat(plan.skippedTypes[QuestionType.JP_TO_HU]).isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
        assertThat(plan.skippedTypes[QuestionType.JP_TO_IMAGE]).isEqualTo(SkipReason.TYPE_DISABLED)
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.ALL_TYPES_SKIPPED)
    }

    @Test
    fun dryRunPromise_matchesGeneratedQuestionCount_FR13() {
        val subject = request(requestedCount = 20)

        assertThat(generator.dryRun(subject).expectedQuestionCount)
            .isEqualTo(generator.generate(subject).questionCount)
    }

    @Test
    fun dryRunAndGenerate_reportTheSameSkipReasons_FR22() {
        val subject = request()

        assertThat(generator.dryRun(subject).skippedTypes)
            .isEqualTo(generator.generate(subject).skippedTypes)
    }

    /**
     * Determinizmus (NFR-07, ADR-007): ugyanaz a kérés – ugyanaz a seed – ugyanaz a terv.
     * Az algoritmus ezt a T-110-ben teljesíti; a szerződés szintjén annyit rögzítünk,
     * hogy a kérés önhordó, tehát a kétszeri hívás összehasonlítható.
     */
    @Test
    fun sameRequestTwice_producesEqualPlans_NFR07() {
        val subject = request()

        assertThat(generator.generate(subject)).isEqualTo(generator.generate(subject))
    }
}
