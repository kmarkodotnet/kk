package hu.kyokushin.trainer.feature.library.list

import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.content.ImportResult
import hu.kyokushin.trainer.core.data.repository.ContentState
import hu.kyokushin.trainer.core.data.repository.TechniqueMatch
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
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
 * Fix óra a tesztekhez: a `ContentState.Imported.importedAt` így determinisztikus, és
 * egyetlen rendszeróra-hívás sem kerül a tesztbe (NFR-07).
 */
internal val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-11T08:00:00Z"), ZoneOffset.UTC)

internal val CATEGORY_DACHI = Category(id = CategoryId.of("dachi"), nameHu = "Állások", sortOrder = 1)
internal val CATEGORY_TSUKI = Category(id = CategoryId.of("tsuki"), nameHu = "Ütések", sortOrder = 2)

internal val TECHNIQUE_HEISOKU = technique(
    id = "KY-0001",
    categoryId = "dachi",
    romaji = "Heisoku Dachi",
    hungarian = "Zárt lábú állás",
)

internal val TECHNIQUE_FUDO = technique(
    id = "KY-0002",
    categoryId = "dachi",
    romaji = "Fudo Dachi",
    hungarian = "Természetes állás",
)

/**
 * Alias-hordozó technika.
 *
 * A valódi tartalomban minden `aliases` lista üres (CONVENTIONS 8.), ezért az alias-ág
 * **szintetikus** adaton mérhető csak – a task „Figyelj rá" pontja is ezt írja elő.
 */
internal val TECHNIQUE_CHUDAN = technique(
    id = "KY-0003",
    categoryId = "tsuki",
    romaji = "Seiken Chudan Tsuki",
    hungarian = "Középszintű ütés",
    aliases = listOf("chudan zuki"),
)

internal val ALL_CATEGORIES = listOf(CATEGORY_DACHI, CATEGORY_TSUKI)
internal val ALL_TECHNIQUES = listOf(TECHNIQUE_HEISOKU, TECHNIQUE_FUDO, TECHNIQUE_CHUDAN)

internal fun technique(
    id: String,
    categoryId: String,
    romaji: String,
    hungarian: String,
    aliases: List<String> = emptyList(),
    status: VerificationStatus = VerificationStatus.VERIFIED,
    retired: Boolean = false,
): Technique = Technique(
    id = TechniqueId.of(id),
    categoryId = CategoryId.of(categoryId),
    romaji = romaji,
    // Ugyanaz a származtatás, amit a validátor végez (V009) – nem kézzel írt alak.
    romajiAscii = NameNormalizer.toAscii(romaji),
    hungarian = hungarian,
    aliases = aliases,
    status = status,
    retired = retired,
    datasetVersion = "test-1",
)

internal fun importedState(
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
 * A [TechniqueRepository] tesztdublőre.
 *
 * ## Miért utánozza a keresést, és miért éppen a `NameNormalizer`-rel?
 *
 * Az SQL oldali `LIKE` keresés helyessége a `RoomTechniqueRepositoryTest` (T-202) dolga –
 * azt itt nem mérjük újra. Amit a ViewModel tesztje mér: a **nyers** keresőszöveg
 * változatlanul leér a repositoryig, és a találatból előálló állapot helyes. Hogy ez a
 * mérés ne egy kitalált illeszkedési szabályon álljon, a dublőr ugyanazt a
 * [NameNormalizer]-t használja, amiből a tárolt `search_romaji` / `search_hungarian` /
 * `aliasNormalized` oszlopok készülnek. Egy saját, „majdnem ugyanilyen" normalizálás
 * zöld tesztet adhatna olyan viselkedésre, amit az éles lekérdezés nem tud.
 *
 * A [observedQueries] azért van, hogy a debounce mérhető legyen: nem a kimenetből
 * következtetünk rá, hanem abból, hogy a repository hányszor kapott kérdést.
 */
internal class FakeTechniqueRepository(
    categories: List<Category> = ALL_CATEGORIES,
    techniques: List<Technique> = ALL_TECHNIQUES,
    contentState: ContentState = importedState(ALL_TECHNIQUES.size),
) : TechniqueRepository {
    private val categoriesFlow = MutableStateFlow(categories)
    private val techniquesFlow = MutableStateFlow(techniques)
    private val contentStateFlow = MutableStateFlow(contentState)

    /** Minden keresés, amit a ViewModel kiadott – sorrendben. */
    val observedQueries: MutableList<String> = mutableListOf()

    override fun observeCategories(): Flow<List<Category>> = categoriesFlow.asStateFlow()

    override fun observeTechniques(query: String, categories: Set<CategoryId>): Flow<List<Technique>> =
        observeTechniqueMatches(query, categories).map { it.map(TechniqueMatch::technique) }

    override fun observeTechniqueMatches(
        query: String,
        categories: Set<CategoryId>,
    ): Flow<List<TechniqueMatch>> {
        observedQueries += query
        return techniquesFlow.map { all -> search(all, query, categories) }
    }

    override suspend fun getTechnique(id: TechniqueId): Technique? =
        techniquesFlow.value.firstOrNull { it.id == id }

    override suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet =
        throw UnsupportedOperationException("A lista ViewModel nem kér kvízjelölteket.")

    override fun observeContentState(): Flow<ContentState> = contentStateFlow.asStateFlow()

    private fun search(
        all: List<Technique>,
        query: String,
        categories: Set<CategoryId>,
    ): List<TechniqueMatch> {
        val needle = NameNormalizer.normalize(query)
        return all.asSequence()
            // A `retired` szűrés a repository felelőssége (DR-07) – a dublőr is így teszi.
            .filter { !it.retired }
            .filter { categories.isEmpty() || it.categoryId in categories }
            .mapNotNull { candidate -> candidate.matchOrNull(needle) }
            .sortedBy { it.technique.romajiAscii }
            .toList()
    }

    private fun Technique.matchOrNull(needle: String): TechniqueMatch? {
        if (needle.isEmpty()) return TechniqueMatch(this, matchedAlias = null)
        val nameMatched = listOf(romaji, romajiAscii, hungarian)
            .any { NameNormalizer.normalize(it).contains(needle) }
        if (nameMatched) return TechniqueMatch(this, matchedAlias = null)
        val matchedAlias = aliases
            .filter { NameNormalizer.normalize(it).contains(needle) }
            .minOrNull()
            ?: return null
        return TechniqueMatch(this, matchedAlias = matchedAlias)
    }
}

/** A tartalomimport tesztdublőre; a kimenetét és a kivételét a teszt dönti el. */
internal class FakeContentImporter(
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
 * Olyan import, amely addig nem tér vissza, amíg a teszt nem engedi.
 *
 * Ez a `Loading` állapot egyetlen őszinte mérése: ha az import azonnal visszatérne, a
 * `Loading` csak a `stateIn` kezdőértéke lenne, és a teszt nem azt mérné, amit állít.
 */
internal class GatedContentImporter(private val gate: Job) : ContentImporter {
    override suspend fun importIfNeeded(): ImportResult {
        gate.join()
        return ImportResult.UpToDate("test-1")
    }

    override suspend fun lastValidationReport(): ValidationReport? = null
}
