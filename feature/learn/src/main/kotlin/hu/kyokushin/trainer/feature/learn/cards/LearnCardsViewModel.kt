package hu.kyokushin.trainer.feature.learn.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.feature.learn.HideMode
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * A tanulókártya munkamenet állapotkezelése (T-303, FR-09, NFR-07, FUNCTIONAL_SPEC 4.2,
 * US-09, G-04 csoport).
 *
 * ## Bemenet: a `learn/setup` (T-302) kijelölése
 *
 * A kijelölt kategóriákat és az elrejtési módot a [savedStateHandle] hordozza, a
 * [KEY_CATEGORY_IDS] és a [KEY_HIDE_MODE] kulcson. Ugyanaz a minta, mint a
 * [hu.kyokushin.trainer.feature.library.detail.LibraryDetailViewModel] `techniqueId`
 * argumentumánál (T-209): a kulcsok csak **névben** egyeznek majd a jövőbeli navigációs
 * argumentummal, a `navigation` modul importálása nélkül (L5). Az aktuális navigációs
 * bekötés (route-argumentumok, a `learn/cards` útvonalminta bővítése) a kártya UI-t építő
 * T-304 tárgya – ott derül ki, milyen alakban kapja a képernyő a két értéket. Eddig a
 * pontig ez a ViewModel önmagában, kézzel összerakott `SavedStateHandle`-lel tesztelt
 * (lásd `LearnCardsViewModelTest`).
 *
 * ## Seedelt keverés – egyszer sorsolva, a munkamenet elején
 *
 * A [seed] a konstruktorban dől el: ha a [savedStateHandle] még nem tartalmazza (első
 * megnyitás), az injektált [random]-ból húzunk egy `Long`-ot, és azonnal elmentjük –
 * innentől a `shuffled(Random(seed))` hívás **mindig** ugyanazt a sorrendet adja ugyanarra
 * a technikalistára, forgatás és folyamat-újraindítás után is (ADR-007 mintája, lásd
 * `DefaultQuizGenerator`). Nincs `Random.Default` és nincs `System.currentTimeMillis()`
 * üzleti kódban (NFR-07) – az egyetlen véletlenforrás az injektált [random], ugyanaz a
 * kötés, amit a kvízmotor is kap (`di.QuizModule`).
 *
 * ## A pozíció és a felfedettség `SavedStateHandle`-ben
 *
 * A pozíció ([KEY_POSITION], `Int`) és a felfedett pozíciók halmaza ([KEY_REVEALED],
 * `ArrayList<String>`-ként tárolt egész számok) is itt él, ugyanazért, amiért a Tudástár
 * keresőszövege és kategóriaszűrője: túléli a képernyőforgatást és a rendszer által
 * kényszerített folyamat-újraindítást (FR-09 „Kész ha").
 *
 * A felfedettséget **pozíció** szerint tároljuk, nem technikaazonosító szerint – a kettő
 * egy munkameneten belül egyenértékű, mert a sorrend egyszer sorsolt és nem változik
 * (lásd fent), így egy adott pozíció mindig ugyanazt a kártyát jelenti; ez a leképezés
 * viszont jóval egyszerűbb, mint azonosító-halmazt egyeztetni az éppen aktuális
 * sorrenddel.
 *
 * ## Előző/következő határokkal
 *
 * A [onNext] és a [onPrevious] a nyers pozíciót írja (a felső határt a hívó oldalon a UI
 * a [LearnCardsUiState.Content.hasNext]/`hasPrevious` alapján tiltja le, T-304), a
 * [buildState] pedig **mindig** a `0..lastIndex` tartományba szorítja a megjelenítendő
 * pozíciót – véletlen, tartományon kívüli érték esetén sem omlik össze (NFR-06).
 *
 * @property repository a tudástár olvasási felülete; innen jön a kijelölt kategóriák
 *   technikalistája (a visszavontak már kiszűrve, ADR-004).
 * @property random az alkalmazás egyetlen véletlenforrása, csak a [seed] sorsolására
 *   (NFR-07).
 * @property defaultDispatcher CPU-kötött háttérmunka dispatchere (a rendezés és a
 *   keverés).
 * @property savedStateHandle a bemenetet és a munkamenet állapotát hordozza.
 */
@HiltViewModel
class LearnCardsViewModel @Inject constructor(
    private val repository: TechniqueRepository,
    private val random: Random,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedCategories: Set<CategoryId> =
        savedStateHandle.get<String>(KEY_CATEGORY_IDS)
            .orEmpty()
            .split(CATEGORY_ID_SEPARATOR)
            .filter { it.isNotBlank() }
            .map(CategoryId::of)
            .toSet()

    private val hideMode: HideMode =
        savedStateHandle.get<String>(KEY_HIDE_MODE)
            ?.let { runCatching { HideMode.valueOf(it) }.getOrNull() }
            ?: HideMode.HIDE_JAPANESE

    /** Egyszer sorsolva: ha már volt, a mentett értéket olvassuk vissza (lásd osztály KDoc). */
    private val seed: Long =
        savedStateHandle.get<Long>(KEY_SEED)
            ?: random.nextLong().also { savedStateHandle[KEY_SEED] = it }

    private val positionFlow: Flow<Int> = savedStateHandle.getStateFlow(KEY_POSITION, 0)

    private val revealedPositionsFlow: Flow<Set<Int>> =
        savedStateHandle.getStateFlow(KEY_REVEALED, ArrayList<String>())
            .map { raw -> raw.mapNotNull(String::toIntOrNull).toSet() }

    val uiState: StateFlow<LearnCardsUiState> = combine(
        repository.observeTechniques(categories = selectedCategories),
        positionFlow,
        revealedPositionsFlow,
    ) { techniques, position, revealedPositions ->
        buildState(techniques, position, revealedPositions)
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LearnCardsUiState.Loading,
        )

    /** Az aktuális kártya felfedése (FR-08). Már felfedett kártyán no-op. */
    fun onReveal() {
        val position = savedStateHandle.get<Int>(KEY_POSITION) ?: 0
        val revealed = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED).orEmpty()
        if (position.toString() !in revealed) {
            savedStateHandle[KEY_REVEALED] = ArrayList(revealed + position.toString())
        }
    }

    /**
     * A következő kártyára lépés (FUNCTIONAL_SPEC 4.2/5.).
     *
     * A nyers pozíció nő; a [buildState] szorítja érvényes tartományba, tehát az utolsó
     * kártyán ismételt hívás biztonságos no-op a megjelenítés szintjén (NFR-06) – a
     * tényleges letiltást a UI végzi `hasNext`-ből (T-304).
     */
    fun onNext() {
        val position = savedStateHandle.get<Int>(KEY_POSITION) ?: 0
        savedStateHandle[KEY_POSITION] = position + 1
    }

    /** Az előző kártyára lépés, határral az elején (FUNCTIONAL_SPEC 4.2/5.). */
    fun onPrevious() {
        val position = savedStateHandle.get<Int>(KEY_POSITION) ?: 0
        savedStateHandle[KEY_POSITION] = (position - 1).coerceAtLeast(0)
    }

    private fun buildState(
        techniques: List<Technique>,
        position: Int,
        revealedPositions: Set<Int>,
    ): LearnCardsUiState {
        if (techniques.isEmpty()) return LearnCardsUiState.Empty

        val ordered = shuffledOrder(techniques)
        val clampedPosition = position.coerceIn(0, ordered.lastIndex)
        val technique = ordered[clampedPosition]

        return LearnCardsUiState.Content(
            card = LearnCardUiModel(
                techniqueId = technique.id,
                romaji = technique.romaji,
                hungarian = technique.hungarian,
                illustration = technique.illustration,
                status = technique.status,
                revealed = clampedPosition in revealedPositions,
            ),
            position = clampedPosition,
            total = ordered.size,
            hasPrevious = clampedPosition > 0,
            hasNext = clampedPosition < ordered.lastIndex,
            hideMode = hideMode,
        )
    }

    /**
     * Seedelt keverés kanonikusan rendezett bemenetből.
     *
     * A bemenet a `technique.id` szerint rendezett, **mielőtt** a `shuffled(Random(seed))`
     * megkeveri: a [TechniqueRepository.observeTechniques] sorrendje nem specifikált
     * szerződés (csak a tartalma az), ezért anélkül a kiinduló sorrend – és vele a
     * kimenet – a repository belső rendezésétől függhetne. Rendezett bemenet mellett a
     * kimenet kizárólag a [seed]-től függ (NFR-07, ugyanaz a garancia, mint a
     * `DefaultQuizGenerator`-nál).
     */
    private fun shuffledOrder(techniques: List<Technique>): List<Technique> =
        techniques.sortedBy { it.id.value }.shuffled(Random(seed))

    companion object {
        /**
         * A kijelölt kategóriák kulcsa, [CATEGORY_ID_SEPARATOR]-rel elválasztva.
         *
         * **Nem** `private`: ez a név lesz majd a jövőbeli navigációs argumentum neve is
         * (T-304), ugyanúgy, mint a `LibraryDetailViewModel.KEY_TECHNIQUE_ID`.
         */
        const val KEY_CATEGORY_IDS = "categoryIds"

        /** A [HideMode] enum neve sztringként. Nem `private`, lásd [KEY_CATEGORY_IDS]. */
        const val KEY_HIDE_MODE = "hideMode"

        /** Elválasztó a [KEY_CATEGORY_IDS] értékében; a `CategoryId` nem tartalmazhat vesszőt. */
        const val CATEGORY_ID_SEPARATOR = ","

        private const val KEY_SEED = "learn.cards.seed"
        private const val KEY_POSITION = "learn.cards.position"
        private const val KEY_REVEALED = "learn.cards.revealedPositions"

        /** A konfigurációváltás alatt életben tartja a feliratkozást. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
