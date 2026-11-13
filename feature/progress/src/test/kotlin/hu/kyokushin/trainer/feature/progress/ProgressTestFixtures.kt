package hu.kyokushin.trainer.feature.progress

import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.content.ImportResult
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
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.scoring.Score
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

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
 * A [ProgressRepository] tesztdublőre a Haladás áttekintő ViewModeljéhez (T-503/T-504/T-505).
 *
 * [observeOverallProgress], [observeCategoryProgress], [observeNeedsPractice] és
 * [observeStudyReviewSummary] kell a `ProgressOverviewViewModel`-nek – a többi tag nem
 * hívott, ezért kivételt dob.
 */
internal class FakeProgressOverviewRepository(
    overallProgress: OverallProgress = OverallProgress(score = Score(0, 0, null)),
    categoryProgress: List<CategoryProgress> = emptyList(),
    needsPractice: List<TechniqueProgress> = emptyList(),
    studyReviewSummary: StudyReviewSummary = StudyReviewSummary(knownCount = 0, needsPracticeCount = 0),
) : ProgressRepository {

    private val overallProgressFlow = MutableStateFlow(overallProgress)
    private val categoryProgressFlow = MutableStateFlow(categoryProgress)
    private val needsPracticeFlow = MutableStateFlow(needsPractice)
    private val studyReviewSummaryFlow = MutableStateFlow(studyReviewSummary)

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

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> = studyReviewSummaryFlow.asStateFlow()

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        throw UnsupportedOperationException("A Haladás áttekintő nem olvas technikánkénti bontást (ez a technika-részletező, T-506 felelőssége).")

    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        needsPracticeFlow.asStateFlow().map { it.take(limit) }

    override fun observeStudyReviewHistory(techniqueId: TechniqueId): Flow<List<StudyReview>> =
        throw UnsupportedOperationException("A Haladás áttekintő nem olvas önértékelési előzményt (ez a technika-részletező, T-506 felelőssége).")

    override suspend fun clearAllProgress() {
        throw UnsupportedOperationException("A Haladás áttekintő nem törli a haladást (ez a `settings`, T-507 felelőssége).")
    }
}

/**
 * A [TechniqueRepository] tesztdublőre a Haladás áttekintő ViewModeljéhez (T-503/T-504)
 * és a technika-részletezőhöz (T-506).
 *
 * [observeCategories] (a kategórianevekhez), [observeTechniques] (a „Gyakorlást igénylő"
 * lista névfeloldásához, T-504), [getTechnique] (a technika-részletezőhöz, T-506; a
 * [techniques] listával ellentétben a visszavontakat is tartalmazhatja, lásd
 * [TechniqueRepository.getTechnique] KDoc-ja) és [observeContentState] (a
 * lektorálatlanság-szalaghoz, ADR-010) kell.
 */
internal class FakeProgressTechniqueRepository(
    categories: List<Category> = listOf(CATEGORY_DACHI, CATEGORY_TSUKI),
    techniques: List<Technique> = emptyList(),
    private val techniqueById: Map<TechniqueId, Technique> = emptyMap(),
    contentState: ContentState = progressImportedState(),
) : TechniqueRepository {

    private val categoriesFlow = MutableStateFlow(categories)
    private val techniquesFlow = MutableStateFlow(techniques)
    private val contentStateFlow = MutableStateFlow(contentState)

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow.asStateFlow()

    override fun observeContentState(): Flow<ContentState> = contentStateFlow.asStateFlow()

    override fun observeTechniques(query: String, categories: Set<CategoryId>): Flow<List<Technique>> =
        techniquesFlow.asStateFlow()

    override fun observeTechniqueMatches(query: String, categories: Set<CategoryId>): Flow<List<TechniqueMatch>> =
        throw UnsupportedOperationException("A Haladás áttekintő nem keres technikát (T-503).")

    override suspend fun getTechnique(id: TechniqueId): Technique? = techniqueById[id]

    override suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet =
        throw UnsupportedOperationException("A Haladás áttekintő nem generál kvízt.")
}

/**
 * A [ProgressRepository] tesztdublőre a Beállítások ViewModeljéhez (T-507/T-509).
 *
 * Kizárólag [clearAllProgress] kell a `ProgressSettingsViewModel`-nek – a hívások száma
 * megszámolható ([clearCallCount]), hogy a teszt bizonyítsa: a kétlépcsős megerősítés
 * előtt egyszer sem, a végső megerősítés után pontosan egyszer hívódik (FR-35).
 */
internal class FakeProgressSettingsRepository : ProgressRepository {

    var clearCallCount: Int = 0
        private set

    override suspend fun clearAllProgress() {
        clearCallCount++
    }

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        throw UnsupportedOperationException("A Beállítások nem nyit kvízmenetet.")

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean =
        throw UnsupportedOperationException("A Beállítások nem rögzít kvízválaszt.")

    override suspend fun finishSession(id: SessionId) {
        throw UnsupportedOperationException("A Beállítások nem zár le menetet.")
    }

    override suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult? =
        throw UnsupportedOperationException("A Beállítások nem olvas vissza kvízmenetet.")

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        throw UnsupportedOperationException("A Beállítások nem rögzít önértékelést.")
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        throw UnsupportedOperationException(
            "A Beállítások nem olvassa az önértékelés-összegzést (ez az áttekintő, T-505 felelőssége).",
        )

    override fun observeOverallProgress(): Flow<OverallProgress> =
        throw UnsupportedOperationException("A Beállítások nem olvassa az aggregált statisztikát (T-501).")

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> =
        throw UnsupportedOperationException("A Beállítások nem olvassa a kategóriánkénti statisztikát (T-501).")

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        throw UnsupportedOperationException(
            "A Beállítások nem olvassa a technikánkénti statisztikát (T-506 felelőssége).",
        )

    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        throw UnsupportedOperationException(
            "A Beállítások nem olvassa a gyakorlást igénylő listát (T-504 felelőssége).",
        )

    override fun observeStudyReviewHistory(techniqueId: TechniqueId): Flow<List<StudyReview>> =
        throw UnsupportedOperationException(
            "A Beállítások nem olvassa az önértékelési előzményt (T-506 felelőssége).",
        )
}

/**
 * A [ProgressRepository] tesztdublőre, amely a [FakeProgressOverviewRepository]-val
 * ellentétben **valóban** nullázza a kvízstatisztikát a [clearAllProgress] hívásra
 * (T-509 „Teendők" 3.: „Törlés hatása: a statisztika nullázódik, a tartalom megmarad").
 *
 * Ez az egyetlen dublőr, ahol [clearAllProgress] nem dob kivételt: egyetlen példányát
 * egyszerre használja egy `ProgressOverviewViewModel` és egy `ProgressSettingsViewModel`
 * a teszten belül, bizonyítva, hogy a Beállításokon elvégzett törlés az áttekintőn is
 * nullázza a statisztikát – ugyanazt a hatást, amit a valódi `RoomProgressRepository`
 * (T-502) dokumentáltan garantál. A tartalominfó ([TechniqueRepository]) ettől a
 * repositorytól **teljesen független** forrás, lásd a tesztet.
 */
internal class FakeResettableProgressRepository(
    overallProgress: OverallProgress = OverallProgress(score = Score(answered = 10, correct = 7, ratio = 0.7)),
    categoryProgress: List<CategoryProgress> = emptyList(),
    needsPractice: List<TechniqueProgress> = emptyList(),
    studyReviewSummary: StudyReviewSummary = StudyReviewSummary(knownCount = 3, needsPracticeCount = 1),
) : ProgressRepository {

    private val overallProgressFlow = MutableStateFlow(overallProgress)
    private val categoryProgressFlow = MutableStateFlow(categoryProgress)
    private val needsPracticeFlow = MutableStateFlow(needsPractice)
    private val studyReviewSummaryFlow = MutableStateFlow(studyReviewSummary)

    var clearCallCount: Int = 0
        private set

    override fun observeOverallProgress(): Flow<OverallProgress> = overallProgressFlow.asStateFlow()

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> = categoryProgressFlow.asStateFlow()

    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        needsPracticeFlow.asStateFlow().map { it.take(limit) }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> = studyReviewSummaryFlow.asStateFlow()

    override suspend fun clearAllProgress() {
        clearCallCount++
        overallProgressFlow.value = OverallProgress(score = Score(answered = 0, correct = 0, ratio = null))
        categoryProgressFlow.value = categoryProgressFlow.value.map { it.copy(score = Score(0, 0, null)) }
        needsPracticeFlow.value = emptyList()
        studyReviewSummaryFlow.value = StudyReviewSummary(knownCount = 0, needsPracticeCount = 0)
    }

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        throw UnsupportedOperationException("Ez a dublőr nem nyit kvízmenetet (T-509).")

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean =
        throw UnsupportedOperationException("Ez a dublőr nem rögzít kvízválaszt (T-509).")

    override suspend fun finishSession(id: SessionId) {
        throw UnsupportedOperationException("Ez a dublőr nem zár le menetet (T-509).")
    }

    override suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult? =
        throw UnsupportedOperationException("Ez a dublőr nem olvas vissza kvízmenetet (T-509).")

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        throw UnsupportedOperationException("Ez a dublőr nem rögzít önértékelést (T-509).")
    }

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        throw UnsupportedOperationException("Ez a dublőr nem olvassa a technikánkénti statisztikát (T-509).")

    override fun observeStudyReviewHistory(techniqueId: TechniqueId): Flow<List<StudyReview>> =
        throw UnsupportedOperationException("Ez a dublőr nem olvassa az önértékelési előzményt (T-509).")
}

/**
 * A [ProgressRepository] tesztdublőre a technika-részletező ViewModeljéhez (T-506).
 *
 * Kizárólag [observeTechniqueProgress] és [observeStudyReviewHistory] kell a
 * `ProgressTechniqueViewModel`-nek – a többi tag nem hívott, ezért kivételt dob.
 */
internal class FakeProgressTechniqueDetailRepository(
    techniqueProgress: TechniqueProgress,
    studyReviewHistory: List<StudyReview> = emptyList(),
) : ProgressRepository {

    private val techniqueProgressFlow = MutableStateFlow(techniqueProgress)
    private val studyReviewHistoryFlow = MutableStateFlow(studyReviewHistory)

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        techniqueProgressFlow.asStateFlow()

    override fun observeStudyReviewHistory(techniqueId: TechniqueId): Flow<List<StudyReview>> =
        studyReviewHistoryFlow.asStateFlow()

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        throw UnsupportedOperationException("A technika-részletező nem nyit kvízmenetet.")

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean =
        throw UnsupportedOperationException("A technika-részletező nem rögzít kvízválaszt.")

    override suspend fun finishSession(id: SessionId) {
        throw UnsupportedOperationException("A technika-részletező nem zár le menetet.")
    }

    override suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult? =
        throw UnsupportedOperationException("A technika-részletező nem olvas vissza egyetlen kvízmenetet.")

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        throw UnsupportedOperationException("A technika-részletező nem rögzít önértékelést.")
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        throw UnsupportedOperationException("A technika-részletező nem olvassa az összesített önértékelést (ez az áttekintő, T-505 felelőssége).")

    override fun observeOverallProgress(): Flow<OverallProgress> =
        throw UnsupportedOperationException("A technika-részletező nem olvassa az aggregált statisztikát (T-501).")

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> =
        throw UnsupportedOperationException("A technika-részletező nem olvassa az aggregált statisztikát (T-501).")

    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        throw UnsupportedOperationException("A technika-részletező nem olvassa a gyakorlást igénylő listát (ez az áttekintő, T-504 felelőssége).")

    override suspend fun clearAllProgress() {
        throw UnsupportedOperationException("A technika-részletező nem törli a haladást (ez a `settings`, T-507 felelőssége).")
    }
}

/**
 * A [ContentImporter] tesztdublőre a fejlesztői diagnosztika ViewModeljéhez (T-508).
 *
 * Kizárólag [lastValidationReport] kell a `ProgressDiagnosticsViewModel`-nek – az import
 * futtatása ([importIfNeeded]) az app indulásának felelőssége, nem a diagnosztika
 * képernyőé, ezért az kivételt dob, ugyanazzal a mintával, mint a többi dublőr.
 */
internal class FakeContentImporter(
    private val report: ValidationReport?,
) : ContentImporter {

    var lastValidationReportCallCount: Int = 0
        private set

    override suspend fun importIfNeeded(): ImportResult =
        throw UnsupportedOperationException("A fejlesztői diagnosztika nem futtat importot.")

    override suspend fun lastValidationReport(): ValidationReport? {
        lastValidationReportCallCount++
        return report
    }
}
