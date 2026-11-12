package hu.kyokushin.trainer.feature.quiz

import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.content.ImportResult
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.NewAttempt
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.StudyReviewSummary
import hu.kyokushin.trainer.core.data.repository.TechniqueMatch
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.text.NameNormalizer
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Közös tesztsegédek a `feature/quiz` modulhoz (T-403).
 *
 * Ugyanaz a mintázat, mint a `feature/learn`-ben (`LearnTestFixtures.kt`): a dublőrök a
 * `core/data` publikus szerződését valósítják meg, nem a Room-ot. A kvízmotor
 * ([hu.kyokushin.trainer.core.quiz.generator.DefaultQuizGenerator]) nem dublőrözött – a
 * teszt a valódi motort futtatja, ugyanúgy, mint a `LearnCardsViewModelTest` a valódi
 * `shuffled(Random(seed))`-ot.
 */
internal val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-12T08:00:00Z"), ZoneOffset.UTC)

internal val CATEGORY_DACHI = Category(id = CategoryId.of("dachi"), nameHu = "Állások", sortOrder = 1)
internal val CATEGORY_TSUKI = Category(id = CategoryId.of("tsuki"), nameHu = "Ütések", sortOrder = 2)

/** Öt, szöveges mezőkben (romaji + magyar) teljes technika – elég A/B típushoz (1 helyes + 3 distraktor). */
internal val TECHNIQUE_HEISOKU = quizTechnique(
    id = "KY-0001",
    categoryId = "dachi",
    romaji = "Heisoku Dachi",
    hungarian = "Zárt lábú állás",
)
internal val TECHNIQUE_FUDO = quizTechnique(
    id = "KY-0002",
    categoryId = "dachi",
    romaji = "Fudo Dachi",
    hungarian = "Természetes állás",
)
internal val TECHNIQUE_MUSUBI = quizTechnique(
    id = "KY-0003",
    categoryId = "dachi",
    romaji = "Musubi Dachi",
    hungarian = "Összezárt sarkú állás",
)
internal val TECHNIQUE_CHUDAN = quizTechnique(
    id = "KY-0004",
    categoryId = "tsuki",
    romaji = "Seiken Chudan Tsuki",
    hungarian = "Középszintű ütés",
)
internal val TECHNIQUE_JODAN = quizTechnique(
    id = "KY-0005",
    categoryId = "tsuki",
    romaji = "Seiken Jodan Tsuki",
    hungarian = "Felső szintű ütés",
)

internal val ALL_QUIZ_CATEGORIES = listOf(CATEGORY_DACHI, CATEGORY_TSUKI)
internal val ALL_QUIZ_TECHNIQUES =
    listOf(TECHNIQUE_HEISOKU, TECHNIQUE_FUDO, TECHNIQUE_MUSUBI, TECHNIQUE_CHUDAN, TECHNIQUE_JODAN)

internal fun quizTechnique(
    id: String,
    categoryId: String,
    romaji: String,
    hungarian: String,
    status: VerificationStatus = VerificationStatus.VERIFIED,
    quizExcluded: Boolean = false,
    retired: Boolean = false,
    illustration: IllustrationRef? = null,
): Technique = Technique(
    id = TechniqueId.of(id),
    categoryId = CategoryId.of(categoryId),
    romaji = romaji,
    romajiAscii = NameNormalizer.toAscii(romaji),
    hungarian = hungarian,
    status = status,
    quizExcluded = quizExcluded,
    retired = retired,
    datasetVersion = "test-1",
    illustration = illustration,
)

/** Kvízben használható, validált kép – a fájlnév az ujjlenyomat (lásd `RoomTechniqueRepository`, T-402). */
internal fun quizUsableIllustration(ref: String): IllustrationRef.Asset = IllustrationRef.Asset(
    ref = ref,
    fileName = "$ref.jpg",
    quizUsable = true,
    hideableOnStudyCard = true,
)

internal fun quizImportedState(
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
 * A [TechniqueRepository] tesztdublőre a `feature/quiz` teszjeihez (T-403).
 *
 * A [getQuizCandidates] a valódi `RoomTechniqueRepository` szerződését tükrözi: a
 * `retired` sorok kimaradnak, az illusztráció-ujjlenyomat a csomagolt fájl neve
 * (`illustration.fileName`), és a szinonimacsoportok a konstruktoron keresztül adhatók meg
 * (alapértelmezésben nincs).
 */
internal class FakeQuizTechniqueRepository(
    categories: List<Category> = ALL_QUIZ_CATEGORIES,
    techniques: List<Technique> = ALL_QUIZ_TECHNIQUES,
    contentState: ContentState = quizImportedState(ALL_QUIZ_TECHNIQUES.size),
    private val synonymGroups: List<Set<TechniqueId>> = emptyList(),
) : TechniqueRepository {

    private val categoriesFlow = MutableStateFlow(categories)
    private val techniquesFlow = MutableStateFlow(techniques)
    private val contentStateFlow = MutableStateFlow(contentState)

    /** Hányszor hívták a [getQuizCandidates]-t – a candidateSetFlow frissülésének bizonyítéka. */
    var getQuizCandidatesCallCount: Int = 0
        private set

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

    override suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet {
        getQuizCandidatesCallCount++
        val active = techniquesFlow.value
            .filter { !it.retired }
            .filter { categories.isEmpty() || it.categoryId in categories }
        val activeIds = active.mapTo(HashSet()) { it.id }
        return QuizCandidateSet(
            techniques = active,
            synonymGroups = synonymGroups
                .map { group -> group.filterTo(LinkedHashSet()) { it in activeIds } }
                .filter { it.size >= 2 },
            illustrationDigests = active
                .mapNotNull { technique ->
                    (technique.illustration as? IllustrationRef.Asset)?.let { technique.id to it.fileName }
                }
                .toMap(),
        )
    }

    override fun observeContentState(): Flow<ContentState> = contentStateFlow.asStateFlow()
}

/**
 * A [ProgressRepository] tesztdublőre a `feature/quiz` teszjeihez (T-403).
 *
 * Kizárólag [startSession]-t vár a kvízbeállító ViewModel – a többi tag nem hívott,
 * ezért kivételt dob, hogy egy véletlen felhasználása azonnal kiderüljön.
 */
internal class FakeQuizProgressRepository(
    private val sessionIdPrefix: String = "session",
) : ProgressRepository {

    private val _startSessionCalls = mutableListOf<Pair<Int, Long>>()

    /** Minden [startSession] hívás paraméterei, a hívás sorrendjében. */
    val startSessionCalls: List<Pair<Int, Long>> get() = _startSessionCalls

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId {
        _startSessionCalls += requestedCount to seed
        return SessionId.of("$sessionIdPrefix-${_startSessionCalls.size}")
    }

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean =
        throw UnsupportedOperationException("A kvízbeállító nem rögzít választ.")

    override suspend fun finishSession(id: SessionId) {
        throw UnsupportedOperationException("A kvízbeállító nem zár le menetet.")
    }

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        throw UnsupportedOperationException("A kvízbeállító nem rögzít önértékelést.")
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        throw UnsupportedOperationException("A kvízbeállító nem olvassa az önértékelés-összegzést.")

    override suspend fun clearAllProgress() {
        throw UnsupportedOperationException("A kvízbeállító nem törli a haladást.")
    }
}

/**
 * A [ProgressRepository] tesztdublőre a `feature/quiz/run` tesztjeihez (T-405, T-406,
 * T-410).
 *
 * A [recordAttempt] alapértelmezésben a valódi `UNIQUE(sessionId, questionId)` kényszert
 * tükrözi: ugyanarra a (menet, kérdés) párra a második hívás `false`-t ad, és nem írja
 * felül a korábban rögzített próbálkozást (ADR-005, T-406 „Teendők" 2.). A
 * [forceRecordResult] ettől függetlenül minden hívásra ugyanazt az értéket adja vissza –
 * ezzel szimulálható a DB-szintű elutasítás (`false`) olyan helyzetben is, amikor a
 * ViewModel saját (`answers` map-alapú) ellenőrzése még nem látná ütközőnek a kérdést.
 */
internal class FakeQuizRunProgressRepository(
    private val forceRecordResult: Boolean? = null,
) : ProgressRepository {

    private val _allAttempts = mutableListOf<NewAttempt>()

    /** Minden [recordAttempt] hívás, a hívás sorrendjében – a visszatérési értéktől függetlenül. */
    val allAttempts: List<NewAttempt> get() = _allAttempts

    private val recordedKeys = mutableSetOf<Pair<SessionId, QuestionId>>()

    override suspend fun startSession(requestedCount: Int, seed: Long): SessionId =
        throw UnsupportedOperationException("A futó kvíz nem nyit új menetet (ez a `quiz/setup` felelőssége).")

    override suspend fun recordAttempt(attempt: NewAttempt): Boolean {
        _allAttempts += attempt
        forceRecordResult?.let { return it }
        return recordedKeys.add(attempt.sessionId to attempt.questionId)
    }

    override suspend fun finishSession(id: SessionId) {
        throw UnsupportedOperationException(
            "A G-07 csoport (T-405/T-406/T-410) szándékosan nem zár le menetet – lásd " +
                "QuizRunViewModel.onConfirmClose KDoc-ja.",
        )
    }

    override suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating) {
        throw UnsupportedOperationException("A futó kvíz nem rögzít önértékelést.")
    }

    override fun observeStudyReviewSummary(): Flow<StudyReviewSummary> =
        throw UnsupportedOperationException("A futó kvíz nem olvassa az önértékelés-összegzést.")

    override suspend fun clearAllProgress() {
        throw UnsupportedOperationException("A futó kvíz nem törli a haladást.")
    }
}

/** A tartalomimport tesztdublőre; a kimenetét és a kivételét a teszt dönti el. */
internal class FakeQuizContentImporter(
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
internal class GatedQuizContentImporter(private val gate: Job) : ContentImporter {

    override suspend fun importIfNeeded(): ImportResult {
        gate.join()
        return ImportResult.UpToDate("test-1")
    }

    override suspend fun lastValidationReport(): ValidationReport? = null
}
