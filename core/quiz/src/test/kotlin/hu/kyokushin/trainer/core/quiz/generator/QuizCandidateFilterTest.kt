package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.validation.QuizEligibility
import org.junit.Test

/**
 * A jelöltszűrés és a jogosultsági kapuk (T-108, TR-03, FR-11, FR-13, FR-14, CR-05).
 *
 * Amit ez a készlet bizonyít:
 *
 * 1. a kategóriaszűrőn kívüli technika egyetlen típusba sem kerül be (FR-11),
 * 2. az `INCOMPLETE`, a `quizExcluded` és a `retired` rekord semelyik típusban nem jelenik
 *    meg (CR-02, ADR-010),
 * 3. a feliratos kép (`quizUsable = false`) sem helyesként, sem distraktorként nem jöhet
 *    szóba a (C)/(D) típusban (CR-05, ADR-011),
 * 4. a kihagyott típus **dokumentált okot** kap, és az ok elhatárolása helyes (FR-22).
 *
 * A szűrés nem sorsol, ezért itt nincs seed-függő elvárás: a determinizmust a rendezett
 * kimenet biztosítja (T-110 a keverésé).
 */
class QuizCandidateFilterTest {

    // ----------------------------------------------------------------------------
    // Kategóriaszűrő (FR-11, TR-03)
    // ----------------------------------------------------------------------------

    @Test
    fun inCategoryScope_emptyFilter_meansEveryCategory_FR11() {
        val request = request(
            techniques = textCandidates("geri") + textCandidates("tsuki").map {
                it.copy(id = TechniqueId.of(it.id.value + "-T"))
            },
            categoryFilter = emptySet(),
        )

        assertThat(QuizCandidateFilter.inCategoryScope(request)).hasSize(8)
        assertThat(request.allCategories).isTrue()
    }

    @Test
    fun inCategoryScope_filteredCategory_excludesEveryOtherTechnique_TR03() {
        val request = request(
            techniques = textCandidates("geri") + technique(id = "KY-9001", categoryId = "tsuki"),
            categoryFilter = setOf("geri"),
        )

        val scope = QuizCandidateFilter.inCategoryScope(request)

        assertThat(scope.map { it.categoryId.value }.toSet()).containsExactly("geri")
        assertThat(scope.map { it.id.value }).doesNotContain("KY-9001")
        // A kategórián kívüli rekord egyetlen típus jelöltkészletében sem szerepel.
        QuestionType.entries.forEach { type ->
            assertThat(QuizCandidateFilter.optionPool(request, type).map { it.id.value })
                .doesNotContain("KY-9001")
        }
    }

    @Test
    fun inCategoryScope_isOrderedById_forDeterminism_NFR07() {
        val reversed = textCandidates().reversed()

        val scope = QuizCandidateFilter.inCategoryScope(request(techniques = reversed))

        assertThat(scope.map { it.id.value }).isInOrder()
    }

    // ----------------------------------------------------------------------------
    // Jogosultsági kapu – a formula a QuizEligibility, nem egy második szabály (ADR-010)
    // ----------------------------------------------------------------------------

    @Test
    fun correctAnswerPool_usesTheSingleEligibilityFormula_ADR010() {
        val request = request(
            techniques = textCandidates() + technique(id = "KY-0009", status = VerificationStatus.INCOMPLETE),
        )

        QuestionType.entries.forEach { type ->
            val expected = QuizCandidateFilter.inCategoryScope(request)
                .filter { QuizEligibility.isEligible(it, type, request.datasetTrust) }
                .map { it.id.value }
            assertThat(QuizCandidateFilter.correctAnswerPool(request, type).map { it.id.value })
                .isEqualTo(expected)
        }
    }

    @Test
    fun correctAnswerPool_incompleteRecord_appearsInNoQuestionType_CR02() {
        val request = request(
            techniques = textCandidates() + technique(
                id = "KY-0009",
                status = VerificationStatus.INCOMPLETE,
                illustration = asset(ref = "KY-0009"),
            ),
        )

        QuestionType.entries.forEach { type ->
            assertThat(QuizCandidateFilter.correctAnswerPool(request, type).map { it.id.value })
                .doesNotContain("KY-0009")
            assertThat(QuizCandidateFilter.optionPool(request, type).map { it.id.value })
                .doesNotContain("KY-0009")
        }
    }

    @Test
    fun correctAnswerPool_quizExcludedOrRetired_appearsInNoQuestionType_ADR010() {
        val request = request(
            techniques = textCandidates() +
                technique(id = "KY-0008", quizExcluded = true, illustration = asset(ref = "KY-0008")) +
                technique(id = "KY-0009", retired = true, illustration = asset(ref = "KY-0009")),
        )

        QuestionType.entries.forEach { type ->
            val ids = QuizCandidateFilter.optionPool(request, type).map { it.id.value }
            assertThat(ids).doesNotContain("KY-0008")
            assertThat(ids).doesNotContain("KY-0009")
        }
    }

    // ----------------------------------------------------------------------------
    // Típusonkénti mezőkövetelmény (FR-13, CR-05, ADR-011)
    // ----------------------------------------------------------------------------

    @Test
    fun optionPool_textTypes_needRomajiAndHungarianOnly_FR14() {
        val request = request(techniques = textCandidates())

        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.JP_TO_HU)).hasSize(4)
        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.HU_TO_JP)).hasSize(4)
        // Kép nélkül a képes típusokban nincs jelölt.
        assertThat(QuizCandidateFilter.correctAnswerPool(request, QuestionType.IMAGE_TO_JP)).isEmpty()
        assertThat(QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_IMAGE)).isEmpty()
    }

    @Test
    fun correctAnswerPool_captionedImage_isNeverAnImageCandidate_CR05_ADR011() {
        val captioned = technique(id = "KY-0005", illustration = asset(ref = "KY-0005", quizUsable = false))
        val request = request(techniques = textCandidates() + captioned)

        // A rekord szakmailag hiteles és a szöveges típusokban használható…
        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.JP_TO_HU).map { it.id.value })
            .contains("KY-0005")
        // …de a feliratos képe sem helyesként, sem opcióként nem kerül képes kérdésbe.
        listOf(QuestionType.IMAGE_TO_JP, QuestionType.JP_TO_IMAGE).forEach { type ->
            assertThat(QuizCandidateFilter.correctAnswerPool(request, type).map { it.id.value })
                .doesNotContain("KY-0005")
        }
        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.JP_TO_IMAGE).map { it.id.value })
            .doesNotContain("KY-0005")
    }

    @Test
    fun correctAnswerPool_placeholderIllustration_isNotAValidatedImage_CR05() {
        val placeholder = technique(id = "KY-0005", illustration = IllustrationRef.Placeholder)
        val request = request(techniques = textCandidates() + placeholder)

        listOf(QuestionType.IMAGE_TO_JP, QuestionType.JP_TO_IMAGE).forEach { type ->
            assertThat(QuizCandidateFilter.correctAnswerPool(request, type)).isEmpty()
        }
    }

    @Test
    fun optionPool_imageToJp_optionsAreNamesNotImages_FR13() {
        // Egy képes rekord + három kép nélküli: a (C) kérdés képe a helyesé, az opciók nevek.
        val withImage = technique(id = "KY-0001", illustration = asset(ref = "KY-0001"))
        val request = request(techniques = listOf(withImage) + textCandidates().drop(1))

        assertThat(QuizCandidateFilter.correctAnswerPool(request, QuestionType.IMAGE_TO_JP)).hasSize(1)
        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.IMAGE_TO_JP)).hasSize(4)
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.IMAGE_TO_JP)).isNull()
        // A (D) típushoz mind a négy opciónak kép kellene – egy képpel nem áll össze.
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_IMAGE))
            .isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
    }

    @Test
    fun optionPool_jpToImage_visuallyIdenticalImagesCountAsOneOption_FR13() {
        val illustrated = (1..4).map { index ->
            technique(id = "KY-000$index", illustration = asset(ref = "KY-000$index"))
        }
        // Négy kvízképes kép, de kettő ujjlenyomata megegyezik → három megkülönböztethető opció.
        val request = request(
            techniques = illustrated,
            illustrationDigests = mapOf(
                "KY-0001" to "aaaa",
                "KY-0002" to "aaaa",
                "KY-0003" to "bbbb",
                "KY-0004" to "cccc",
            ),
        )

        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.JP_TO_IMAGE)).hasSize(4)
        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_IMAGE)).isEqualTo(0)
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_IMAGE))
            .isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
    }

    @Test
    fun buildableQuestionCount_jpToImage_fourDistinctImages_isBuildable_FR13() {
        val illustrated = (1..4).map { index ->
            technique(id = "KY-000$index", illustration = asset(ref = "KY-000$index"))
        }
        val request = request(
            techniques = illustrated,
            illustrationDigests = (1..4).associate { "KY-000$it" to "digest-$it" },
        )

        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_IMAGE)).isEqualTo(4)
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_IMAGE)).isNull()
    }

    // ----------------------------------------------------------------------------
    // Típusonkénti jelöltszám és a kihagyás oka (FR-14, FR-22, TR-03)
    // ----------------------------------------------------------------------------

    @Test
    fun buildableQuestionCount_fewerThanFourOptions_yieldsNoQuestion_FR14() {
        val request = request(techniques = textCandidates().take(3))

        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_HU)).isEqualTo(0)
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_HU))
            .isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
    }

    @Test
    fun buildableQuestionCount_countsEveryEligibleCorrectAnswer_FR14() {
        val request = request(techniques = textCandidates() + technique(id = "KY-0005"))

        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_HU)).isEqualTo(5)
    }

    @Test
    fun skipReasons_disabledType_isReportedAsDisabledNotAsMissingData_FR12() {
        val request = request(
            techniques = textCandidates(),
            enabledTypes = setOf(QuestionType.JP_TO_HU),
        )

        val reasons = QuizCandidateFilter.skipReasons(request)

        assertThat(reasons).doesNotContainKey(QuestionType.JP_TO_HU)
        assertThat(reasons[QuestionType.HU_TO_JP]).isEqualTo(SkipReason.TYPE_DISABLED)
        // Noha képek sincsenek, a letiltott típus oka a letiltás – nem adathiány.
        assertThat(reasons[QuestionType.IMAGE_TO_JP]).isEqualTo(SkipReason.TYPE_DISABLED)
        assertThat(reasons[QuestionType.JP_TO_IMAGE]).isEqualTo(SkipReason.TYPE_DISABLED)
    }

    @Test
    fun skipReasons_noValidatedImage_skipsImageTypesWithDocumentedReason_TR03() {
        val request = request(techniques = textCandidates())

        val reasons = QuizCandidateFilter.skipReasons(request)

        assertThat(reasons[QuestionType.IMAGE_TO_JP]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        assertThat(reasons[QuestionType.JP_TO_IMAGE]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        assertThat(reasons).doesNotContainKey(QuestionType.JP_TO_HU)
        assertThat(reasons).doesNotContainKey(QuestionType.HU_TO_JP)
    }

    @Test
    fun skipReasons_imageToJp_imagePresentButTooFewNames_isCandidateShortage_FR14() {
        // Van kvízképes kép, de összesen három rekord – négy opció nem áll össze.
        val request = request(
            techniques = listOf(technique(id = "KY-0001", illustration = asset(ref = "KY-0001"))) +
                textCandidates().drop(2),
        )

        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.IMAGE_TO_JP))
            .isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
    }

    @Test
    fun skipReasons_emptyCandidateSet_skipsEveryEnabledType_FR22() {
        val request = request(techniques = emptyList())

        val reasons = QuizCandidateFilter.skipReasons(request)

        assertThat(reasons.keys).containsExactlyElementsIn(QuestionType.entries)
        assertThat(reasons[QuestionType.JP_TO_HU]).isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
        assertThat(reasons[QuestionType.JP_TO_IMAGE]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
    }

    @Test
    fun buildableQuestionCounts_coversEveryQuestionType_FR13() {
        val counts = QuizCandidateFilter.buildableQuestionCounts(request())

        assertThat(counts.keys).containsExactlyElementsIn(QuestionType.entries)
        assertThat(counts[QuestionType.JP_TO_HU]).isEqualTo(4)
        assertThat(counts[QuestionType.IMAGE_TO_JP]).isEqualTo(0)
    }

    @Test
    fun skipReason_everyDatasetTrustLevel_keepsTheGateDownwardOnly_ADR010() {
        DatasetTrust.entries.forEach { trust ->
            val request = request(techniques = textCandidates(), datasetTrust = trust)
            // A bizalmi szint a szöveges típusokat engedi, a képhiányt nem írja felül.
            assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_HU)).isNull()
            assertThat(QuizCandidateFilter.skipReason(request, QuestionType.IMAGE_TO_JP))
                .isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        }
    }
}
