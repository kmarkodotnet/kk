package hu.kyokushin.trainer.feature.quiz.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.NewAttempt
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizPlan
import hu.kyokushin.trainer.core.quiz.generator.QuizRunStateCodec
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A futó kvíz állapotgépe (T-405, T-406, T-410; ARCHITECTURE 6.2 „a legkritikusabb
 * pont"; FR-16, FR-18, FR-19, G-07 csoport).
 *
 * ## Bemenet: honnan jön a terv
 *
 * A `quiz/setup` (T-403/T-404) a „Kvíz indítása" gombra egy [hu.kyokushin.trainer.feature.quiz.setup.QuizStartResult]-ot
 * ad (sessionId + terv + seed), de ezt **nem** a `quiz/run/{sessionId}` route path-argumentuma
 * hordozza majd – egy teljes [QuizPlan] túl nagy és túl összetett egy URL-argumentumhoz, és a
 * `feature/quiz` modul nem is látna `kotlinx.serialization`-t a kódolásához (ADR-001, L3,
 * lásd `QuizRunStateCodec` KDoc-ja). A bemenet ehelyett a cél [SavedStateHandle]-jébe kerül
 * **közvetlenül**, a hívó (a `quiz/setup` képernyő navigációs bekötése) által, a
 * `navController.getBackStackEntry(route).savedStateHandle[key] = value` mintával – ugyanaz a
 * technika, mint a `docs/CONVENTIONS.md` 4.1 „Eredmény visszaadása navigáció közben" sora, csak
 * **előre** irányban használva (ott egy korábbi képernyő állapotát módosítja egy későbbi,
 * itt egy későbbi képernyő bemenetét adja egy korábbi). Ez a navigációs bekötés **nem** ennek a
 * csoportnak (G-07) a tárgya – a `feature/quiz` modulhatárt nem lépi át (lásd
 * `docs/backlog/GROUPS.md`), és csak ott dönthető el érdemben, amikor a `quiz/run` route
 * argumentuma és a `KyokushinNavHost` ág is készül.
 *
 * A [KEY_SESSION_ID] kulcs **nem** `private`: ez lesz majd a jövőbeli `quiz/run/{sessionId}`
 * route path-argumentumának a neve is (ugyanaz a minta, mint a
 * [hu.kyokushin.trainer.feature.library.detail.LibraryDetailViewModel.KEY_TECHNIQUE_ID]-nél).
 * A [KEY_SEED] és a [KEY_PLAN] nem lehet route-argumentum (lásd fent), ezért ezek a
 * `quiz.run.*` prefixet kapják, mint a `QuizSetupViewModel` saját, nem route-eredetű kulcsai.
 *
 * ## Az `answers` map az egyetlen igazság (ARCHITECTURE 6.2)
 *
 * A [savedStateHandle]-ben kódolt [QuizRunState.answers] map (lásd [QuizRunStateCodec]) dönti el, mely
 * kérdésre érkezett már válasz – **nem** egy, a `ProgressRepository`-ból visszaolvasott
 * lekérdezés. Az [onAnswer] ezt ellenőrzi **először**, mielőtt a `ProgressRepository`-t
 * egyáltalán elérné – ez az első védelmi vonal (ADR-005); a DB `UNIQUE(sessionId, questionId)`
 * kényszere (a [ProgressRepository.recordAttempt] `false` visszatérése) a második; egy
 * jövőbeli UI gombletiltás a harmadik, csak másodlagos/vizuális védelem (T-406 „Figyelj rá").
 *
 * ## Nincs újragenerálás forgatáskor
 *
 * A [QuizRunState.seed] és a [QuizRunState.questions] is a mentett állapot része: a
 * [buildUiState] a kapott tervet olvassa vissza, nem futtatja újra a `QuizGenerator`-t. Ha
 * egy jövőbeli hívási út
 * (folyamat-újraindítás a mentett `SavedStateHandle` nélkül) mégis újragenerálná a tervet
 * ugyanabból a seedből és kérésből, a motor determinizmusa (ADR-007) bitre ugyanazt adná.
 *
 * ## Ami NEM ennek a csoportnak (G-07) a tárgya
 *
 * A kérdés-UI (opciógombok/képrács, T-407/T-408) és a válasz-visszajelzés (T-409) nem
 * része ennek a ViewModelnek – azok a [QuizRunUiState.Content.question] IDjeit oldják fel
 * a tudástárból. A kvíz **normál** befejezése (az utolsó kérdés utáni „Következő" →
 * eredményképernyő, `ProgressRepository.finishSession`) a G-09 csoport (T-411) tárgya: az
 * [onNext] ezért az utolsó kérdésen szándékosan **no-op** marad – lásd ott a KDoc-ot.
 *
 * @property progressRepository a válasz rögzítésének írási felülete (FR-18).
 * @property savedStateHandle a teljes [QuizRunState] hordozója.
 */
@HiltViewModel
class QuizRunViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** A bemenet (sessionId, seed, terv) – egyszer olvasva, a menet alatt nem változik. */
    private val initial: InitialInput? = readInitialInput(savedStateHandle)

    private val currentIndexFlow: Flow<Int> = savedStateHandle.getStateFlow(KEY_CURRENT_INDEX, 0)

    private val answersFlow: Flow<Map<QuestionId, TechniqueId>> =
        savedStateHandle.getStateFlow(KEY_ANSWERS, "").map(::decodeAnswersOrEmpty)

    private val closeConfirmationFlow: Flow<Boolean> =
        savedStateHandle.getStateFlow(KEY_CLOSE_CONFIRMATION, false)

    private val _closeEvent = Channel<Unit>(Channel.BUFFERED)

    /** Egyszeri esemény: a megszakítás megerősítve – a hívó dönt a navigációról (L5). */
    val closeEvent: Flow<Unit> = _closeEvent.receiveAsFlow()

    /**
     * A teljes [QuizRunState], a `SavedStateHandle`-ből visszaépítve – `null`, ha a
     * bemenet hiányzik vagy sérült (lásd [readInitialInput]).
     */
    private val runStateFlow: Flow<QuizRunState?> = combine(currentIndexFlow, answersFlow) { index, answers ->
        val input = initial ?: return@combine null
        val lastIndex = (input.plan.questions.size - 1).coerceAtLeast(0)
        QuizRunState(
            sessionId = input.sessionId,
            seed = input.seed,
            questions = input.plan.questions,
            currentIndex = index.coerceIn(0, lastIndex),
            answers = answers,
            shortfall = input.plan.shortfall,
        )
    }

    val uiState: StateFlow<QuizRunUiState> = combine(
        runStateFlow,
        closeConfirmationFlow,
    ) { state, closeConfirmationVisible ->
        buildUiState(state, closeConfirmationVisible)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = QuizRunUiState.Loading,
        )

    /**
     * Egy válasz rögzítése (T-406, FR-18, FR-19).
     *
     * No-op, ha [questionId]-ra már van válasz [answersFlow]-ban (lásd az osztály
     * KDoc-ja: ez az **első** védelmi vonal – a gyors dupla koppintás itt elakad, mielőtt
     * a [progressRepository] egyáltalán meghívásra kerülne).
     *
     * A helyesség **azonosító alapú** (T-406 „Kész ha" 2., DR-03): `selected ==
     * question.correctTechniqueId`, soha nem szöveges egyezés.
     */
    fun onAnswer(questionId: QuestionId, selectedTechniqueId: TechniqueId) {
        val plan = initial?.plan ?: return
        val sessionId = initial.sessionId
        val answers = decodeAnswersOrEmpty(savedStateHandle.get<String>(KEY_ANSWERS).orEmpty())
        if (questionId in answers) return
        val question = plan.questions.firstOrNull { it.id == questionId } ?: return
        val correct = selectedTechniqueId == question.correctTechniqueId

        viewModelScope.launch {
            val recorded = progressRepository.recordAttempt(
                NewAttempt(
                    sessionId = sessionId,
                    questionId = questionId,
                    techniqueId = question.correctTechniqueId,
                    questionType = question.type,
                    selectedTechniqueId = selectedTechniqueId,
                    correct = correct,
                ),
            )
            // T-406 "Teendők" 2.: a `false` (már volt válasz ERRE a kérdésre a DB
            // szerint – a második védelmi vonal) nem módosítja az állapotot; a korábban
            // rögzített válasz marad érvényben.
            if (recorded) {
                savedStateHandle[KEY_ANSWERS] =
                    QuizRunStateCodec.encodeAnswers(answers + (questionId to selectedTechniqueId))
            }
        }
    }

    /**
     * A következő kérdésre lépés (T-410 „Teendők": „a kérdések között nincs
     * visszalépés" – nincs `onPrevious`).
     *
     * Az utolsó kérdésen no-op: lásd az osztály KDoc-ja – a kvíz befejezése a G-09
     * csoport (T-411) tárgya.
     */
    fun onNext() {
        val plan = initial?.plan ?: return
        if (plan.questions.isEmpty()) return
        val index = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0
        savedStateHandle[KEY_CURRENT_INDEX] = (index + 1).coerceAtMost(plan.questions.lastIndex)
    }

    /**
     * A bezárás (×) gomb **és** a rendszer vissza-gombja (FUNCTIONAL_SPEC 5.2/5.,
     * T-410 „Teendők" 2–3.) – mindkettő ezt a metódust hívja, ezért a két belépési pont
     * garantáltan ugyanazt váltja ki: a megerősítő párbeszéd megnyitását. A tényleges
     * `BackHandler`/ikongomb bekötése a kérdés-UI-t adó feladat tárgya (lásd az osztály
     * KDoc-ja: ez a csoport csak az állapotgépet adja).
     */
    fun onRequestClose() {
        savedStateHandle[KEY_CLOSE_CONFIRMATION] = true
    }

    /** A megerősítő párbeszéd „Folytatás" gombja (FUNCTIONAL_SPEC 5.2/5.). */
    fun onCancelClose() {
        savedStateHandle[KEY_CLOSE_CONFIRMATION] = false
    }

    /**
     * A megerősítő párbeszéd „Megszakítás" gombja (FR-19).
     *
     * Szándékosan **nem** hívja a [ProgressRepository.finishSession]-t: a megszakított
     * munkamenet `finishedAt`-ja marad `null`, az `answeredCount` a tényleges érték
     * (FUNCTIONAL_SPEC 5.2 „Viselkedési szabályok") – ez automatikusan igaz, mert az
     * eddig rögzített [NewAttempt]-ek ([onAnswer]) már megvannak a `quiz_attempt`
     * táblában, és ide nincs mit visszavonni. A navigáció kifelé adott, egyszeri esemény
     * ([closeEvent]) – a hívó dönt a tényleges kilépésről (L5).
     */
    fun onConfirmClose() {
        savedStateHandle[KEY_CLOSE_CONFIRMATION] = false
        viewModelScope.launch { _closeEvent.send(Unit) }
    }

    private fun decodeAnswersOrEmpty(raw: String): Map<QuestionId, TechniqueId> =
        if (raw.isBlank()) emptyMap() else QuizRunStateCodec.decodeAnswers(raw)

    /**
     * A [QuizRunState] (a mentett igazság) leképezése a képernyő [QuizRunUiState]-jére.
     *
     * `null` bemenetre vagy üres kérdéslistára [QuizRunUiState.Empty] (NFR-06 védelmi
     * háló) – lásd [readInitialInput] KDoc-ja.
     */
    private fun buildUiState(state: QuizRunState?, closeConfirmationVisible: Boolean): QuizRunUiState {
        if (state == null || state.questions.isEmpty()) return QuizRunUiState.Empty

        val question = state.questions[state.currentIndex]
        return QuizRunUiState.Content(
            sessionId = state.sessionId,
            question = question,
            position = state.currentIndex,
            total = state.questions.size,
            answeredCount = state.answers.size,
            selectedTechniqueId = state.answers[question.id],
            isLastQuestion = state.currentIndex == state.questions.lastIndex,
            closeConfirmationVisible = closeConfirmationVisible,
        )
    }

    /** A bemenet, egy csomagban – lásd az osztály KDoc-ja. */
    private data class InitialInput(
        val sessionId: SessionId,
        val seed: Long,
        val plan: QuizPlan,
    )

    companion object {
        /**
         * A kvízmenet azonosítója. **Nem** `private` – lásd az osztály KDoc-ja: ez lesz
         * majd a `quiz/run/{sessionId}` route path-argumentumának a neve is.
         */
        const val KEY_SESSION_ID = "sessionId"

        /** A terv generálásához sorsolt RNG-mag – nem route-argumentum, lásd az osztály KDoc-ja. */
        const val KEY_SEED = "quiz.run.seed"

        /** A `QuizRunStateCodec.encodePlan` kimenete – nem route-argumentum, lásd az osztály KDoc-ja. */
        const val KEY_PLAN = "quiz.run.plan"

        private const val KEY_CURRENT_INDEX = "quiz.run.currentIndex"
        private const val KEY_ANSWERS = "quiz.run.answers"
        private const val KEY_CLOSE_CONFIRMATION = "quiz.run.closeConfirmationVisible"

        /** A konfigurációváltás alatt életben tartja a feliratkozást. */
        private const val STOP_TIMEOUT_MS = 5_000L

        /**
         * A bemenet kiolvasása – hiányzó vagy sérült adatra `null` (NFR-06 védelmi háló),
         * nem kivétel: egy ilyen `savedStateHandle` csak egy még nem létező hívó (lásd az
         * osztály KDoc-ja) hiányából fakadhat, és a [buildUiState] ezt [QuizRunUiState.Empty]-re
         * képezi, nem omlik össze.
         */
        private fun readInitialInput(savedStateHandle: SavedStateHandle): InitialInput? {
            val sessionIdRaw = savedStateHandle.get<String>(KEY_SESSION_ID) ?: return null
            val seed = savedStateHandle.get<Long>(KEY_SEED) ?: return null
            val planRaw = savedStateHandle.get<String>(KEY_PLAN) ?: return null
            val sessionId = runCatching { SessionId.of(sessionIdRaw) }.getOrNull() ?: return null
            val plan = runCatching { QuizRunStateCodec.decodePlan(planRaw) }.getOrNull() ?: return null
            return InitialInput(sessionId, seed, plan)
        }
    }
}
