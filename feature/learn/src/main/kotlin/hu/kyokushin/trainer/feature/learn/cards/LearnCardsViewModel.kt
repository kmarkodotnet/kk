package hu.kyokushin.trainer.feature.learn.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.localizedName
import hu.kyokushin.trainer.core.ui.locale.currentLanguageTag
import hu.kyokushin.trainer.feature.learn.HideMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * A tanulókártya munkamenet állapotkezelése (T-303, FR-09, NFR-07, FUNCTIONAL_SPEC 4.2,
 * US-09, G-04 csoport; a felső fülváltó T-706, FR-40).
 *
 * ## Bemenet: a `learn/setup` (T-302) kijelölése
 *
 * A kijelölt kategóriákat a [savedStateHandle] hordozza, a [KEY_CATEGORY_IDS] kulcson.
 * Ugyanaz a minta, mint a
 * [hu.kyokushin.trainer.feature.library.detail.LibraryDetailViewModel] `techniqueId`
 * argumentumánál (T-209): a kulcs csak **névben** egyezik a navigációs argumentummal, a
 * `navigation` modul importálása nélkül (L5).
 *
 * ## Az elrejtési irány T-706 óta élőben váltható, nem a `learn/setup` egyszeri bemenete
 *
 * A [HideMode] eredetileg (T-302/T-303) a `learn/setup`-ból érkező, a munkamenet elején
 * **egyszer** rögzített érték volt. T-706-tal ez megszűnt: a kártya felső fülváltója
 * ([onSelectHideMode]) bármikor átállíthatja, a [hideModeFlow] ezért a [KEY_CATEGORY_IDS]-
 * hez hasonlóan `SavedStateHandle`-ben él, de **reaktívan** (`getStateFlow`), nem egyszer
 * kiolvasott `val`-ként – a `learn/setup` (és a `navigation` route) innentől **nem** ad
 * át kezdeti elrejtési irányt, a kártyaképernyő saját alapértelmezéssel indul
 * ([DEFAULT_HIDE_MODE]).
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
 * ## Előző/következő határokkal, és a záróképernyő (T-307)
 *
 * A [onNext] és a [onPrevious] a nyers pozíciót írja; az alsó határt a [onPrevious]
 * maga szorítja `0`-ra, a felső határnak viszont **nincs** hasonló szorítása [onNext]-ben
 * – a `lastIndex` utáni pozíció szándékosan **nem** kerül vissza az utolsó kártyára, mert
 * ez a [LearnCardsUiState.Finished] záróképernyő bemenete (FUNCTIONAL_SPEC 4.2: „Az
 * utolsó kártya után záróképernyő"). A [buildState] tehát két esetet különböztet meg: ha
 * a pozíció a `techniques.size`-nál kisebb, a megfelelő [LearnCardsUiState.Content] jön;
 * ha nem, [LearnCardsUiState.Finished]. A UI (T-304) a „Következő" gombot ezért az
 * utolsó kártyán **sem** tiltja le – egyetlen további lépés a záróképernyőre visz.
 *
 * A [onRestart] a záróképernyő „Újrakezdés" gombjának bemenete: visszaállítja a pozíciót
 * `0`-ra és törli a felfedettségi készletet, de **nem** sorsol új [seed]-et – ugyanazon a
 * (már egyszer sorsolt) sorrenden megy újra végig a felhasználó, csak elölről és üres
 * felfedettséggel, hogy a kártyák ismét „kérdésként" hassanak.
 *
 * ## Önértékelés és automatikus továbblépés (T-306, FR-10, FR-28)
 *
 * A [onSelfRate] ([onKnew]/[onNeedsPractice] rövidítéseivel) a [progressRepository]-n
 * keresztül ment egy `StudyReview` rekordot, majd a mentés után a meglévő [onNext]-et
 * hívja – ugyanaz a nyers pozíciólépés, mint a sima „Következő" gombnál, tehát az utolsó
 * kártyán az önértékelés is a [LearnCardsUiState.Finished] záróképernyőre vezet. A
 * mentés időbélyege az **injektált** `Clock`-ból jön, de **nem** ebben a ViewModelben:
 * a [ProgressRepository] egyetlen megvalósítása ([hu.kyokushin.trainer.core.data.repository.RoomProgressRepository])
 * olvassa ki a saját `Clock`-ját (NFR-07) – ez a ViewModel ezért szándékosan nem kap
 * külön `Clock`-ot, mert nincs saját időfüggő döntése (CONVENTIONS 4.1: „Nem injektálunk,
 * amire nincs szükség").
 *
 * A letiltás ([LearnCardUiModel.revealed] `false`) a UI (T-304) felelőssége, de csak
 * **elsődleges** védelem, ugyanúgy, mint a kvíz egyszeri válasza (ADR-005): a
 * [onSelfRate] maga is ellenőrzi a jelenlegi [uiState] felfedettségét, és no-op, ha a
 * kártya még nincs felfedve – sem `StudyReview` nem íródik, sem továbblépés nem
 * történik.
 *
 * @property repository a tudástár olvasási felülete; innen jön a kijelölt kategóriák
 *   technikalistája (a visszavontak már kiszűrve, ADR-004).
 * @property progressRepository az önértékelés írási felülete (FR-10) – kizárólag a
 *   `study_review` táblát érinti, a kvízstatisztikától függetlenül (FR-28).
 * @property random az alkalmazás egyetlen véletlenforrása, csak a [seed] sorsolására
 *   (NFR-07).
 * @property defaultDispatcher CPU-kötött háttérmunka dispatchere (a rendezés és a
 *   keverés).
 * @property savedStateHandle a bemenetet és a munkamenet állapotát hordozza.
 */
@HiltViewModel
class LearnCardsViewModel
    @Inject
    constructor(
        private val repository: TechniqueRepository,
        private val progressRepository: ProgressRepository,
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

        /**
         * Melyik nevet rejti el a kártya – T-706 óta **élőben váltható** a kártya felső
         * fülváltójával ([onSelectHideMode]), nem csak a `learn/setup`-ból érkező,
         * egyszer beállított érték (lásd az osztály KDoc-ja).
         */
        private val hideModeFlow: Flow<HideMode> =
            savedStateHandle.getStateFlow(KEY_HIDE_MODE, DEFAULT_HIDE_MODE.name)
                .map { raw -> runCatching { HideMode.valueOf(raw) }.getOrDefault(DEFAULT_HIDE_MODE) }

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
            repository.observeCategories(),
            positionFlow,
            revealedPositionsFlow,
            hideModeFlow,
        ) { techniques, categories, position, revealedPositions, hideMode ->
            buildState(techniques, categories, position, revealedPositions, hideMode)
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
         * Önértékelés mentése, majd automatikus továbblépés (T-306, FR-10, FR-28,
         * FUNCTIONAL_SPEC 4.2/4.: „Megnyomás után: `StudyReview` rekord mentése … automatikus
         * továbblépés a következő kártyára").
         *
         * No-op, ha az aktuális kártya nincs felfedve – lásd az osztály KDoc-ja. A UI (T-304)
         * ezért a két gombot felfedésig letiltva tartja, de ez csak elsődleges védelem.
         */
        fun onSelfRate(rating: SelfRating) {
            val state = uiState.value
            if (state !is LearnCardsUiState.Content || !state.card.revealed) return
            val techniqueId = state.card.techniqueId
            viewModelScope.launch {
                progressRepository.recordStudyReview(techniqueId, rating)
                onNext()
            }
        }

        /** A „Tudtam" gomb (FUNCTIONAL_SPEC 4.2/4., [SelfRating.KNEW]). */
        fun onKnew() = onSelfRate(SelfRating.KNEW)

        /** A „Gyakorolnom kell" gomb (FUNCTIONAL_SPEC 4.2/4., [SelfRating.NEEDS_PRACTICE]). */
        fun onNeedsPractice() = onSelfRate(SelfRating.NEEDS_PRACTICE)

        /**
         * A következő kártyára lépés (FUNCTIONAL_SPEC 4.2/5.).
         *
         * A nyers pozíció nő, szorítás nélkül: az utolsó kártyán hívva a [buildState]
         * [LearnCardsUiState.Finished]-t ad, azaz a záróképernyőre lép (T-307, lásd az
         * osztály KDoc-ja). A UI (T-304) emiatt a „Következő" gombot az utolsó kártyán sem
         * tiltja le.
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

        /**
         * A felső fülváltó (T-706, FR-40): melyik nevet mutassa elsőként a kártya.
         *
         * Az **aktuális** kártya felfedettsége törlődik (US-10 3. elfogadási kritériuma:
         * a felirat elrejtése nem sérülhet – ha a kártya már fel volt fedve a régi
         * irányban, a fülváltás után újra el kell rejteni, hogy a felidézés valódi
         * próba maradjon), de a **pozíció nem változik**, és a többi, már megtekintett
         * kártya felfedettsége is érintetlen marad – csak az éppen látott kártyáé törlődik.
         */
        fun onSelectHideMode(hideMode: HideMode) {
            savedStateHandle[KEY_HIDE_MODE] = hideMode.name
            val position = savedStateHandle.get<Int>(KEY_POSITION) ?: 0
            val revealed = savedStateHandle.get<ArrayList<String>>(KEY_REVEALED).orEmpty()
            if (position.toString() in revealed) {
                savedStateHandle[KEY_REVEALED] = ArrayList(revealed - position.toString())
            }
        }

        /**
         * Visszaállás az első kártyára, üres felfedettségi készlettel (T-307, a záróképernyő
         * „Újrakezdés" gombja). A [seed] – és vele a kártyák sorrendje – **nem** változik,
         * lásd az osztály KDoc-ja.
         */
        fun onRestart() {
            savedStateHandle[KEY_POSITION] = 0
            savedStateHandle[KEY_REVEALED] = ArrayList<String>()
        }

        private fun buildState(
            techniques: List<Technique>,
            categories: List<Category>,
            position: Int,
            revealedPositions: Set<Int>,
            hideMode: HideMode,
        ): LearnCardsUiState {
            if (techniques.isEmpty()) return LearnCardsUiState.Empty

            val ordered = shuffledOrder(techniques)

            // A `lastIndex`-en túli pozíció a záróképernyő bemenete, nem egy "túllépett"
            // érték, amit vissza kellene szorítani – lásd az osztály KDoc-ja (T-307).
            if (position >= ordered.size) {
                return LearnCardsUiState.Finished(total = ordered.size, categoryIds = selectedCategories)
            }

            val languageTag = currentLanguageTag()
            val categoryNameById: Map<CategoryId, String> =
                categories.associate { it.id to it.localizedName(languageTag) }
            val clampedPosition = position.coerceIn(0, ordered.lastIndex)
            val technique = ordered[clampedPosition]

            return LearnCardsUiState.Content(
                card = LearnCardUiModel(
                    techniqueId = technique.id,
                    categoryName = categoryNameById[technique.categoryId].orEmpty(),
                    romaji = technique.romaji,
                    hungarian = technique.localizedName(languageTag),
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

            /**
             * A [HideMode] enum neve sztringként. T-706 óta **nem** navigációs argumentum
             * (a fülváltás a kártya képernyőjén élőben történik, nem a `learn/setup`-ból jön)
             * – a láthatóság a tesztfixtúra (`LearnCardsViewModelTest.handleFor`) direkt
             * `SavedStateHandle`-előtöltése miatt maradt nem `private`.
             */
            const val KEY_HIDE_MODE = "hideMode"

            /** Elválasztó a [KEY_CATEGORY_IDS] értékében; a `CategoryId` nem tartalmazhat vesszőt. */
            const val CATEGORY_ID_SEPARATOR = ","

            /**
             * Alapértelmezett irány, amíg a felhasználó nem váltott fület (T-706): a
             * "Japán nevek" fül aktív először – FR-40/a felhasználói kérés szövege
             * ("Kiindulás: Japán nevek, Magyar nevek") ezt sorolja elsőként.
             */
            private val DEFAULT_HIDE_MODE = HideMode.HIDE_HUNGARIAN

            private const val KEY_SEED = "learn.cards.seed"
            private const val KEY_POSITION = "learn.cards.position"
            private const val KEY_REVEALED = "learn.cards.revealedPositions"

            /** A konfigurációváltás alatt életben tartja a feliratkozást. */
            private const val STOP_TIMEOUT_MS = 5_000L
        }
    }
