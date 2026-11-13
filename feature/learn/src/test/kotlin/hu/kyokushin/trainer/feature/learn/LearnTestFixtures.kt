package hu.kyokushin.trainer.feature.learn

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
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.text.NameNormalizer
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Közös tesztsegédek a `feature/learn` modulhoz (T-302, T-303, G-04 csoport).
 *
 * Ugyanaz a mintázat, mint a `feature/library`-ban (`LibraryListFakes.kt`): a dublőrök a
 * `core/data` publikus szerződését valósítják meg, nem a Room-ot.
 */
internal val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-12T08:00:00Z"), ZoneOffset.UTC)

internal val CATEGORY_DACHI = Category(id = CategoryId.of("dachi"), nameHu = "Állások", sortOrder = 1)
internal val CATEGORY_TSUKI = Category(id = CategoryId.of("tsuki"), nameHu = "Ütések", sortOrder = 2)

internal val TECHNIQUE_HEISOKU = learnTechnique(
    id = "KY-0001",
    categoryId = "dachi",
    romaji = "Heisoku Dachi",
    hungarian = "Zárt lábú állás",
)

internal val TECHNIQUE_FUDO = learnTechnique(
    id = "KY-0002",
    categoryId = "dachi",
    romaji = "Fudo Dachi",
    hungarian = "Természetes állás",
    status = VerificationStatus.AWAITING_REVIEW,
)

internal val TECHNIQUE_CHUDAN = learnTechnique(
    id = "KY-0003",
    categoryId = "tsuki",
    romaji = "Seiken Chudan Tsuki",
    hungarian = "Középszintű ütés",
)

internal val ALL_LEARN_CATEGORIES = listOf(CATEGORY_DACHI, CATEGORY_TSUKI)
internal val ALL_LEARN_TECHNIQUES = listOf(TECHNIQUE_HEISOKU, TECHNIQUE_FUDO, TECHNIQUE_CHUDAN)

internal fun learnTechnique(
    id: String,
    categoryId: String,
    romaji: String,
    hungarian: String,
    status: VerificationStatus = VerificationStatus.VERIFIED,
    retired: Boolean = false,
    illustration: IllustrationRef? = null,
): Technique = Technique(
    id = TechniqueId.of(id),
    categoryId = CategoryId.of(categoryId),
    romaji = romaji,
    romajiAscii = NameNormalizer.toAscii(romaji),
    hungarian = hungarian,
    status = status,
    retired = retired,
    datasetVersion = "test-1",
    illustration = illustration,
)

/** Feliratos kép (T-305): a fájl a technika nevét is tartalmazza, ezért felfedésig rejtve marad. */
internal fun captionedIllustration(ref: String = "KY-0045"): IllustrationRef.Asset = IllustrationRef.Asset(
    ref = ref,
    fileName = "$ref.jpg",
    quizUsable = false,
    hideableOnStudyCard = false,
)

/** Normál (felirat nélküli) kép: a tanulókártyán felfedés előtt is látható. */
internal fun plainIllustration(ref: String = "KY-0044"): IllustrationRef.Asset = IllustrationRef.Asset(
    ref = ref,
    fileName = "$ref.jpg",
    quizUsable = true,
    hideableOnStudyCard = true,
)

internal fun learnImportedState(
    activeTechniqueCount: Int,
    datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
): ContentState.Imported = ContentState.Imported(
    schemaVersion = "1.0.0",
    contentVersion = "test-1",
    datasetTrust = datasetTrust,
    importedAt = FIXED_CLOCK.instant(),
    acceptedCount = activeTechniqueCount,
    rejectedCount = 0,
    blockingIssueCount = 0,
    warningIssueCount = 0,
    datasetRejected = false,
    activeTechniqueCount = activeTechniqueCount,
)

/**
 * A [TechniqueRepository] tesztdublőre a `feature/learn` teszjeihez.
 *
 * Egyszerűbb, mint a `feature/library` megfelelője: a tanulás nem keres szabad
 * szöveggel, ezért a [observeTechniqueMatches] csak a kategóriaszűrést és a `retired`
 * kizárást végzi, egyezés-jelzés nélkül.
 */
internal class FakeLearnTechniqueRepository(
    categories: List<Category> = ALL_LEARN_CATEGORIES,
    techniques: List<Technique> = ALL_LEARN_TECHNIQUES,
    contentState: ContentState = learnImportedState(ALL_LEARN_TECHNIQUES.size),
) : TechniqueRepository {
    private val categoriesFlow = MutableStateFlow(categories)
    private val techniquesFlow = MutableStateFlow(techniques)
    private val contentStateFlow = MutableStateFlow(contentState)

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow.asStateFlow()

    override fun observeTechniques(query: String, categories: Set<CategoryId>): Flow<List<Technique>> =
        techniquesFlow.map { all ->
            all.filter { !it.retired }
                .filter { categories.isEmpty() || it.categoryId in categories }
        }

    override fun observeTechniqueMatches(
        query: String,
        categories: Set<CategoryId>,
    ): Flow<List<TechniqueMatch>> =
        observeTechniques(query, categories).map { list -> list.map { TechniqueMatch(it, null) } }

    override suspend fun getTechnique(id: TechniqueId): Technique? =
        techniquesFlow.value.firstOrNull { it.id == id }

    override suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet =
        throw UnsupportedOperationException("A tanulás nem kér kvízjelölteket.")

    override fun observeContentState(): Flow<ContentState> = contentStateFlow.asStateFlow()
}

/**
 * A [ProgressRepository] tesztdublőre a `feature/learn` teszjeihez (T-306, T-308).
 *
 * A tanulás kizárólag [recordStudyReview]-t hívja (FR-10, FR-28) – a kvízmenethez tartozó
 * tagok itt nem várt hívások, ezért kivételt dobnak, hogy egy véletlen felhasználásuk
 * azonnal kiderüljön, ne csendben nullázzon valamit.
 */
internal class FakeLearnProgressRepository : ProgressRepository {
    private val _recordedReviews = mutableListOf<Pair<TechniqueId, SelfRating>>()

    /** Minden [recordStudyReview] hívás, a hívás sorrendjében. */
    val recordedReviews: List<Pair<TechniqueId, SelfRating>> get() = _recordedReviews

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        throw UnsupportedOperationException("A tanulás nem indít kvízmenetet.")

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean =
        throw UnsupportedOperationException("A tanulás nem rögzít kvízválaszt.")

    override suspend fun finishSession(id: SessionId): Unit =
        throw UnsupportedOperationException("A tanulás nem zár le kvízmenetet.")

    override suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult? =
        throw UnsupportedOperationException("A tanulás nem olvas vissza kvízeredményt.")

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        _recordedReviews += techniqueId to rating
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        throw UnsupportedOperationException("A tanulókártya ViewModel nem olvassa az összegzést.")

    override fun observeOverallProgress(): Flow<OverallProgress> =
        throw UnsupportedOperationException("A tanulás nem olvassa az aggregált statisztikát (T-501).")

    override fun observeCategoryProgress(): Flow<List<CategoryProgress>> =
        throw UnsupportedOperationException("A tanulás nem olvassa az aggregált statisztikát (T-501).")

    override fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress> =
        throw UnsupportedOperationException("A tanulás nem olvassa az aggregált statisztikát (T-501).")

    override fun observeNeedsPractice(limit: Int): Flow<List<TechniqueProgress>> =
        throw UnsupportedOperationException("A tanulás nem olvassa az aggregált statisztikát (T-501).")

    override fun observeStudyReviewHistory(techniqueId: TechniqueId): Flow<List<StudyReview>> =
        throw UnsupportedOperationException("A tanulás nem olvassa az önértékelési előzményt (T-506).")

    override suspend fun clearAllProgress(): Unit =
        throw UnsupportedOperationException("A tanulás nem törli a haladást.")
}

/** A tartalomimport tesztdublőre; a kimenetét és a kivételét a teszt dönti el. */
internal class FakeLearnContentImporter(
    private val result: () -> ImportResult = { ImportResult.UpToDate("test-1") },
) : ContentImporter {
    var importCount: Int = 0
        private set

    override suspend fun importIfNeeded(): ImportResult {
        importCount++
        return result()
    }

    override suspend fun lastValidationReport(): ValidationReport? = null
}

/**
 * Olyan import, amely addig nem tér vissza, amíg a teszt nem engedi – a `Loading`
 * állapot egyetlen honest mérése (lásd `LibraryListFakes.GatedContentImporter`).
 */
internal class GatedLearnContentImporter(private val gate: Job) : ContentImporter {
    override suspend fun importIfNeeded(): ImportResult {
        gate.join()
        return ImportResult.UpToDate("test-1")
    }

    override suspend fun lastValidationReport(): ValidationReport? = null
}
