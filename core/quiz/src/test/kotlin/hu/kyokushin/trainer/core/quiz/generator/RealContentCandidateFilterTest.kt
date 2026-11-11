package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.RealKnowledgeBase
import org.junit.Test

/**
 * A jelöltszűrés a **valódi tartalmon** (T-108, TR-03, FR-11, FR-13, CR-05).
 *
 * Miért kell ez a szintetikus készlet mellé? Mert a szintetikus teszt bemenetét a teszt
 * írta: pontosan azt az alakot tartalmazza, amit a szűrő kezelni tud. Ez a teszt a
 * `knowledgebase_transfer/` tényleges 161 rekordján fut, és azt a konkrét állítást
 * rögzíti, ami a mostani tartalomra igaz: a képes (C)/(D) kvíztípus **csak** az
 * `stances` kategóriában áll össze, 7 kvízképes képpel – a három feliratos kép kiesik
 * (ADR-011). Ha a tartalom változik, ez a teszt kikényszeríti, hogy valaki tudomásul vegye.
 */
class RealContentCandidateFilterTest {

    private val report = DefaultKnowledgeValidator().validate(RealKnowledgeBase.load())

    private fun realRequest(
        enabledTypes: Set<QuestionType> = QuestionType.entries.toSet(),
        categoryFilter: Set<String> = emptySet(),
    ) = QuizRequest(
        candidates = QuizCandidateSet(
            techniques = report.accepted,
            synonymGroups = report.synonymGroups,
            // A tartalmi ujjlenyomatok a képfájlokból a `core/data` importban készülnek
            // (T-115). A szűrés szempontjából a hiányzó ujjlenyomat nem bizonyít
            // egyezést: minden kvízképes kép önálló opciónak számít.
            illustrationDigests = emptyMap(),
        ),
        enabledTypes = enabledTypes,
        categoryFilter = categoryFilter.mapTo(LinkedHashSet(), CategoryId::of),
        requestedCount = 10,
        seed = 1L,
        datasetTrust = report.datasetTrust,
    )

    @Test
    fun realContent_imageTypes_haveExactlySevenQuizUsableCandidates_TR03_CR05() {
        val request = realRequest()

        listOf(QuestionType.IMAGE_TO_JP, QuestionType.JP_TO_IMAGE).forEach { type ->
            val pool = QuizCandidateFilter.correctAnswerPool(request, type)
            assertThat(pool).hasSize(RealKnowledgeBase.EXPECTED_QUIZ_USABLE_ILLUSTRATION_COUNT)
            // A három feliratos kép (KY-0045, KY-0050, KY-0052) nincs köztük.
            assertThat(pool.map { it.id.value })
                .containsNoneOf("KY-0045", "KY-0050", "KY-0052")
        }
    }

    @Test
    fun realContent_imageCandidates_areAllInTheStancesCategory_TR03() {
        val request = realRequest()

        val categories = QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_IMAGE)
            .map { it.categoryId.value }
            .toSet()

        assertThat(categories).containsExactly(RealKnowledgeBase.ILLUSTRATED_CATEGORY_ID)
    }

    @Test
    fun realContent_imageTypesOutsideStances_areSkippedWithDocumentedReason_TR03_FR13() {
        val request = realRequest(categoryFilter = setOf("punches"))

        val reasons = QuizCandidateFilter.skipReasons(request)

        assertThat(reasons[QuestionType.IMAGE_TO_JP]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        assertThat(reasons[QuestionType.JP_TO_IMAGE]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        // A szöveges típusok ugyanebben a kategóriában viszont összeállnak.
        assertThat(reasons).doesNotContainKey(QuestionType.JP_TO_HU)
        assertThat(reasons).doesNotContainKey(QuestionType.HU_TO_JP)
    }

    @Test
    fun realContent_stancesCategory_buildsEveryQuestionType_TR03() {
        val request = realRequest(categoryFilter = setOf(RealKnowledgeBase.ILLUSTRATED_CATEGORY_ID))

        assertThat(QuizCandidateFilter.skipReasons(request)).isEmpty()
        val counts = QuizCandidateFilter.buildableQuestionCounts(request)
        assertThat(counts[QuestionType.JP_TO_IMAGE])
            .isEqualTo(RealKnowledgeBase.EXPECTED_QUIZ_USABLE_ILLUSTRATION_COUNT)
        assertThat(counts[QuestionType.IMAGE_TO_JP])
            .isEqualTo(RealKnowledgeBase.EXPECTED_QUIZ_USABLE_ILLUSTRATION_COUNT)
    }

    @Test
    fun realContent_categoryFilter_neverLeaksTechniqueFromAnotherCategory_FR11() {
        val request = realRequest(categoryFilter = setOf("kata"))

        val scope = QuizCandidateFilter.inCategoryScope(request)

        assertThat(scope).isNotEmpty()
        assertThat(scope.map { it.categoryId.value }.toSet()).containsExactly("kata")
        assertThat(scope.size).isLessThan(RealKnowledgeBase.EXPECTED_ENTRY_COUNT)
    }

    @Test
    fun realContent_everyTextTypeCandidate_isEligibleAcrossAllCategories_FR14() {
        val request = realRequest()

        // Egyetlen rekord sem `INCOMPLETE`, `quizExcluded` vagy `retired` a mostani
        // tartalomban, ezért a szöveges típusok teljes jelöltkészlete a 161 rekord.
        assertThat(QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_HU))
            .hasSize(RealKnowledgeBase.EXPECTED_ENTRY_COUNT)
        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.HU_TO_JP))
            .isEqualTo(RealKnowledgeBase.EXPECTED_ENTRY_COUNT)
    }
}
