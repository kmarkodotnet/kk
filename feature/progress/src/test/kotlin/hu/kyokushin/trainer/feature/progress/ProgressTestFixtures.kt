package hu.kyokushin.trainer.feature.progress

import hu.kyokushin.trainer.core.data.repository.CategoryProgress
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.NewAttempt
import hu.kyokushin.trainer.core.data.repository.OverallProgress
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.QuizSessionResult
import hu.kyokushin.trainer.core.data.repository.StudyReviewSummary
import hu.kyokushin.trainer.core.data.repository.TechniqueMatch
import hu.kyokushin.trainer.core.data.repository.TechniqueProgress
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.scoring.Score
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Közös tesztsegédek a `feature/progress` modulhoz (T-503).
 *
 * Ugyanaz a mintázat, mint a `feature/quiz`/`feature/learn`-ben (lásd
 * `docs/CONVENTIONS.md` 1. szakasz „Egy `ProgressRepository`-bővítés minden
 * tesztdublőrjét érinti" sora): a dublőrök a `core/data` publikus szerződését
 * valósítják meg, a nem hívott tagok kivételt dobnak, hogy egy véletlen felhasználásuk
 * azonnal kiderüljön, ne csendben hamis adatot adjon.
 */
internal val FIXED_IMPORTED_AT: Instant = Instant.parse("2026-11-13T08:00:00Z")

internal val CATEGORY_DACHI = Category(id = CategoryId.of("dachi"), nameHu = "Állások", sortOrder = 1)
internal val CATEGORY_TSUKI = Category(id = CategoryId.of("tsuki"), nameHu = "Ütések", sortOrder = 2)

internal fun progressImportedState(
    activeTechniqueCount: Int = 10,
    datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
): ContentState.Imported = ContentState.Imported(
    schemaVersion = "1.0.0",
    contentVersion = "test-1",
    datasetTrust = datasetTrust,
    importedAt = FIXED_IMPORTED_AT,
    acceptedCount = activeTechniqueCount,
    rejectedCount = 0,
    blockingIssueCount = 0,
    warningIssueCount = 0,
    datasetRejected = false,
    activeTechniqueCount = activeTechniqueCount,
)

/**
 * A [ProgressRepository] tesztdublőre a Haladás áttekintő ViewModeljéhez (T-503).
 *
 * Kizárólag [observeOverallProgress] és [observeCategoryProgress] kell a
 * `ProgressOverviewViewModel`-nek – a többi tag nem hívott, ezért kivételt dob.
 */
internal class FakeProgressOverviewRepository(
    overallProgress: OverallProgress = OverallProgress(score = Score(0, 0, null)),
    categoryProgress: List<CategoryProgress> = emptyList(),
) : ProgressRepository {

    private val overallProgressFlow = MutableStateFlow(overallProgress)
    private val categoryProgressFlow = MutableStateFlow(categoryProgress)

    override fun observeOverallProgress(): Flow<OverallProgress> = overallProgressFlow.asStateFlow()

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> = categoryProgressFlow.asStateFlow()

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        throw UnsupportedOperationException("A Haladás áttekintő nem nyit kvízmenetet.")

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean =
        throw UnsupportedOperationException("A Haladás áttekintő nem rögzít kvízválaszt.")

    override suspend fun finishSession(id: SessionId) {
        throw UnsupportedOperationException("A Haladás áttekintő nem zár le menetet.")
    }

    override suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult? =
        throw UnsupportedOperationException("A Haladás áttekintő nem olvas vissza egyetlen kvízmenetet.")

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        throw UnsupportedOperationException("A Haladás áttekintő nem rögzít önértékelést.")
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        throw UnsupportedOperationException("A Haladás áttekintő kerete (T-503) még nem olvassa az önértékelés-összegzést (T-505).")

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        throw UnsupportedOperationException("A Haladás áttekintő kerete (T-503) technikánkénti bontást nem olvas.")

    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        throw UnsupportedOperationException("A Haladás áttekintő kerete (T-503) még nem olvassa a gyakorlást igénylő listát (T-504).")

    override suspend fun clearAllProgress() {
        throw UnsupportedOperationException("A Haladás áttekintő nem törli a haladást (ez a `settings`, T-507 felelőssége).")
    }
}

/**
 * A [TechniqueRepository] tesztdublőre a Haladás áttekintő ViewModeljéhez (T-503).
 *
 * Kizárólag [observeCategories] (a kategórianevekhez) és [observeContentState] (a
 * lektorálatlanság-szalaghoz, ADR-010) kell.
 */
internal class FakeProgressTechniqueRepository(
    categories: List<Category> = listOf(CATEGORY_DACHI, CATEGORY_TSUKI),
    contentState: ContentState = progressImportedState(),
) : TechniqueRepository {

    private val categoriesFlow = MutableStateFlow(categories)
    private val contentStateFlow = MutableStateFlow(contentState)

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow.asStateFlow()

    override fun observeContentState(): Flow<ContentState> = contentStateFlow.asStateFlow()

    override fun observeTechniques(query: String, categories: Set<CategoryId>): Flow<List<Technique>> =
        throw UnsupportedOperationException("A Haladás áttekintő nem listáz technikát (T-503).")

    override fun observeTechniqueMatches(query: String, categories: Set<CategoryId>): Flow<List<TechniqueMatch>> =
        throw UnsupportedOperationException("A Haladás áttekintő nem keres technikát (T-503).")

    override suspend fun getTechnique(id: TechniqueId): Technique? =
        throw UnsupportedOperationException("A Haladás áttekintő kerete (T-503) nem old fel egyetlen technikát sem.")

    override suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet =
        throw UnsupportedOperationException("A Haladás áttekintő nem generál kvízt.")
}
