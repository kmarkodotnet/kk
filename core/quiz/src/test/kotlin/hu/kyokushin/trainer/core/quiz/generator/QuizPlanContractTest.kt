package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A [QuizPlan], a [Shortfall] és a [QuizReadiness] szerződése (FR-14, FR-22).
 *
 * A központi állítás: elégtelen adat **nem hiba**, hanem strukturált, a UI számára
 * értelmezhető indoklás. A motor nem dob kivételt, és nem gyárt hibás kérdést.
 */
class QuizPlanContractTest {

    private val json = Json

    @Test
    fun emptyPlan_isValidAndCannotStart_FR22() {
        val plan = QuizPlan(
            questions = emptyList(),
            skippedTypes = mapOf(
                QuestionType.JP_TO_HU to SkipReason.NOT_ENOUGH_CANDIDATES,
                QuestionType.IMAGE_TO_JP to SkipReason.NOT_ENOUGH_IMAGES,
                QuestionType.JP_TO_IMAGE to SkipReason.TYPE_DISABLED,
            ),
            shortfall = Shortfall(
                requestedCount = 10,
                actualCount = 0,
                reason = ShortfallReason.ALL_TYPES_SKIPPED,
            ),
        )

        assertThat(plan.canStart).isFalse()
        assertThat(plan.questionCount).isEqualTo(0)
        assertThat(plan.skippedTypes).hasSize(3)
        assertThat(plan.shortfall?.missingCount).isEqualTo(10)
    }

    @Test
    fun plan_rejectsDuplicateQuestionIds_ADR005() {
        assertThrows(IllegalArgumentException::class.java) {
            QuizPlan(questions = listOf(question(id = "Q-1"), question(id = "Q-1")))
        }
    }

    @Test
    fun plan_withAllRequestedQuestions_hasNoShortfall_FR22() {
        val plan = QuizPlan(questions = listOf(question(id = "Q-1"), question(id = "Q-2")))

        assertThat(plan.canStart).isTrue()
        assertThat(plan.shortfall).isNull()
        assertThat(plan.skippedTypes).isEmpty()
    }

    @Test
    fun shortfall_existsOnlyWhenFewerQuestionsThanRequested_FR22() {
        assertThrows(IllegalArgumentException::class.java) {
            Shortfall(requestedCount = 5, actualCount = 5, reason = ShortfallReason.NOT_ENOUGH_CANDIDATES)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Shortfall(requestedCount = 5, actualCount = 6, reason = ShortfallReason.NOT_ENOUGH_CANDIDATES)
        }

        val shortfall = Shortfall(
            requestedCount = 10,
            actualCount = 6,
            reason = ShortfallReason.NOT_ENOUGH_DISTRACTORS,
        )
        assertThat(shortfall.missingCount).isEqualTo(4)
    }

    /**
     * A félbehagyott kvíz a kérdéslistával és a [Shortfall]-lal együtt a
     * `SavedStateHandle`-be kerül (ADR-007, T-405), ezért a teljes tervnek körbe kell
     * fordulnia – a kihagyási okokkal együtt, mert a UI azokból magyaráz.
     */
    @Test
    fun plan_survivesSerializationRoundTrip_FR19() {
        val plan = QuizPlan(
            questions = listOf(
                question(id = "Q-1", type = QuestionType.JP_TO_HU),
                question(
                    id = "Q-2",
                    type = QuestionType.HU_TO_JP,
                    correct = "KY-0003",
                    options = listOf("KY-0003", "KY-0001", "KY-0004", "KY-0002"),
                ),
            ),
            skippedTypes = mapOf(QuestionType.JP_TO_IMAGE to SkipReason.NOT_ENOUGH_IMAGES),
            shortfall = Shortfall(
                requestedCount = 5,
                actualCount = 2,
                reason = ShortfallReason.NOT_ENOUGH_IMAGES,
            ),
        )

        val restored = json.decodeFromString(
            QuizPlan.serializer(),
            json.encodeToString(QuizPlan.serializer(), plan),
        )

        assertThat(restored).isEqualTo(plan)
    }

    @Test
    fun readiness_shortfallAgreesWithExpectedCount_FR22() {
        // Kevesebb kérdés várható a kértnél, mégsincs shortfall: a beállító képernyő
        // elhallgatná a hiányt.
        assertThrows(IllegalArgumentException::class.java) {
            QuizReadiness(requestedCount = 10, expectedQuestionCount = 6, expectedShortfall = null)
        }
        // Több kérdés, mint amennyit kértek: a terv soha nem ad többet.
        assertThrows(IllegalArgumentException::class.java) {
            QuizReadiness(requestedCount = 10, expectedQuestionCount = 12)
        }
        // Shortfall van, de más kérdésszámról beszél, mint az összegzés.
        assertThrows(IllegalArgumentException::class.java) {
            QuizReadiness(
                requestedCount = 10,
                expectedQuestionCount = 6,
                expectedShortfall = Shortfall(10, 7, ShortfallReason.NOT_ENOUGH_CANDIDATES),
            )
        }

        val readiness = QuizReadiness(
            requestedCount = 10,
            expectedQuestionCount = 6,
            availableByType = mapOf(QuestionType.JP_TO_HU to 4, QuestionType.HU_TO_JP to 2),
            skippedTypes = mapOf(QuestionType.IMAGE_TO_JP to SkipReason.NOT_ENOUGH_IMAGES),
            expectedShortfall = Shortfall(10, 6, ShortfallReason.NOT_ENOUGH_CANDIDATES),
        )

        assertThat(readiness.canStart).isTrue()
        // Az availableByType típusonkénti **kapacitás**, nem a terv felosztása (T-111):
        // csak nem kihagyott típus szerepel benne, mindegyik legalább 1 kérdéssel, és az
        // összeg a várható kérdésszám felső korlátja – nem feltétlenül egyenlő vele.
        assertThat(readiness.availableByType.keys).containsNoneIn(readiness.skippedTypes.keys)
        assertThat(readiness.availableByType.values.min()).isAtLeast(1)
        assertThat(readiness.availableByType.values.sum()).isAtLeast(readiness.expectedQuestionCount)
    }

    /**
     * Az `availableByType` nem lehet ellentmondásban a `skippedTypes`-szal, és nulla
     * kapacitású típust sem hordozhat (T-111, FR-22).
     */
    @Test
    fun readiness_availableByTypeIsDisjointFromSkippedTypes_FR22() {
        assertThrows(IllegalArgumentException::class.java) {
            QuizReadiness(
                requestedCount = 5,
                expectedQuestionCount = 2,
                availableByType = mapOf(QuestionType.JP_TO_HU to 2, QuestionType.HU_TO_JP to 0),
                skippedTypes = mapOf(QuestionType.HU_TO_JP to SkipReason.NOT_ENOUGH_CANDIDATES),
                expectedShortfall = Shortfall(5, 2, ShortfallReason.NOT_ENOUGH_CANDIDATES),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            QuizReadiness(
                requestedCount = 5,
                expectedQuestionCount = 2,
                availableByType = mapOf(QuestionType.JP_TO_HU to 2),
                skippedTypes = mapOf(QuestionType.JP_TO_HU to SkipReason.TYPE_DISABLED),
                expectedShortfall = Shortfall(5, 2, ShortfallReason.NOT_ENOUGH_CANDIDATES),
            )
        }
        // Van kapacitás, mégsem áll össze kérdés – ez a beállító képernyőn „nem indítható"
        // állapotot mutatna hiányzó indok nélkül.
        assertThrows(IllegalArgumentException::class.java) {
            QuizReadiness(
                requestedCount = 5,
                expectedQuestionCount = 0,
                availableByType = mapOf(QuestionType.JP_TO_HU to 3),
                expectedShortfall = Shortfall(5, 0, ShortfallReason.ALL_TYPES_SKIPPED),
            )
        }
    }

    @Test
    fun readiness_withoutAnyQuestion_cannotStart_FR22() {
        val readiness = QuizReadiness(
            requestedCount = 5,
            expectedQuestionCount = 0,
            skippedTypes = QuestionType.entries.associateWith { SkipReason.NOT_ENOUGH_CANDIDATES },
            expectedShortfall = Shortfall(5, 0, ShortfallReason.ALL_TYPES_SKIPPED),
        )

        assertThat(readiness.canStart).isFalse()
    }

    @Test
    fun request_emptyCategoryFilterMeansAllCategories_FR11() {
        val request = QuizRequest(
            candidates = QuizCandidateSet(techniques = listOf(technique())),
            enabledTypes = setOf(QuestionType.JP_TO_HU),
            requestedCount = 5,
            seed = 42L,
            datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
        )

        assertThat(request.allCategories).isTrue()
    }

    @Test
    fun request_rejectsNonPositiveQuestionCount_FR13() {
        assertThrows(IllegalArgumentException::class.java) {
            QuizRequest(
                candidates = QuizCandidateSet(),
                enabledTypes = setOf(QuestionType.JP_TO_HU),
                requestedCount = 0,
                seed = 1L,
                datasetTrust = DatasetTrust.REVIEWED,
            )
        }
    }

    /** Üres típushalmaz érvényes bemenet – a motor nem dob kivételt, a UI indokol (FR-22). */
    @Test
    fun request_acceptsNoEnabledType_FR22() {
        val request = QuizRequest(
            candidates = QuizCandidateSet(),
            enabledTypes = emptySet(),
            requestedCount = 5,
            seed = 1L,
            datasetTrust = DatasetTrust.REVIEWED,
        )

        assertThat(request.enabledTypes).isEmpty()
    }

    @Test
    fun candidateSet_exposesMutualSynonymExclusion_CR07() {
        val a = technique(id = "KY-0001", romaji = "Gedan barai")
        val b = technique(id = "KY-0002", romaji = "Gedan uke")
        val c = technique(id = "KY-0003", romaji = "Mae geri")
        val set = QuizCandidateSet(
            techniques = listOf(a, b, c),
            synonymGroups = listOf(setOf(a.id, b.id)),
        )

        assertThat(set.synonymsOf(a.id)).containsExactly(b.id)
        assertThat(set.synonymsOf(b.id)).containsExactly(a.id)
        assertThat(set.synonymsOf(c.id)).isEmpty()
    }

    @Test
    fun candidateSet_rejectsSynonymGroupOutsideTheSet_CR07() {
        val a = technique(id = "KY-0001")
        assertThrows(IllegalArgumentException::class.java) {
            QuizCandidateSet(
                techniques = listOf(a),
                synonymGroups = listOf(setOf(a.id, TechniqueId.of("KY-0099"))),
            )
        }
    }

    @Test
    fun candidateSet_rejectsSingleMemberSynonymGroup_CR07() {
        val a = technique(id = "KY-0001")
        assertThrows(IllegalArgumentException::class.java) {
            QuizCandidateSet(techniques = listOf(a), synonymGroups = listOf(setOf(a.id)))
        }
    }

    @Test
    fun candidateSet_rejectsDuplicateTechnique() {
        assertThrows(IllegalArgumentException::class.java) {
            QuizCandidateSet(techniques = listOf(technique(id = "KY-0001"), technique(id = "KY-0001")))
        }
    }

    @Test
    fun candidateSet_exposesVisualIdentityOfIllustrations_TR02() {
        val a = technique(id = "KY-0001")
        val b = technique(id = "KY-0002")
        val c = technique(id = "KY-0003")
        val set = QuizCandidateSet(
            techniques = listOf(a, b, c),
            illustrationDigests = mapOf(a.id to "aaa", b.id to "aaa"),
        )

        assertThat(set.illustrationDigestOf(a.id)).isEqualTo(set.illustrationDigestOf(b.id))
        assertThat(set.illustrationDigestOf(c.id)).isNull()
    }
}
