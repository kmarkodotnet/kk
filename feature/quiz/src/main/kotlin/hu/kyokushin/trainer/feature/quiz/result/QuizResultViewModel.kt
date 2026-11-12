package hu.kyokushin.trainer.feature.quiz.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.QuizSessionResult
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.core.quiz.generator.QuizRequest
import hu.kyokushin.trainer.core.quiz.scoring.QuizScoring
import hu.kyokushin.trainer.feature.quiz.setup.QuizStartResult
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A kvízeredmény képernyő állapotkezelése (FUNCTIONAL_SPEC 5.3, FR-20, FR-21, FR-27,
 * US-16, T-411, T-412, T-415, G-09 csoport).
 *
 * ## Bemenet: a `sessionId`, mint route-argumentum
 *
 * A `quiz/result/{sessionId}` route (FUNCTIONAL_SPEC 5.3) a [KEY_SESSION_ID] kulcs alatt
 * tölti fel a [savedStateHandle]-t – ugyanaz a technika, mint a
 * [hu.kyokushin.trainer.feature.quiz.run.QuizRunViewModel.KEY_SESSION_ID]-nél és a
 * [hu.kyokushin.trainer.feature.library.detail.LibraryDetailViewModel.KEY_TECHNIQUE_ID]-nél
 * (lásd `docs/CONVENTIONS.md` 5. szakasz): a kulcs **névben** egyezik a `navigation` modul
 * route-argumentumával, de ez a modul nem függ a `navigation`-re (L5) – a tényleges
 * route-bekötés nem ennek a csoportnak (G-09) a tárgya, lásd `docs/CONVENTIONS.md` 5.
 * szakasz "a `quiz/setup` UI (T-404) elkészült, de a `navigation` modul bekötése
 * szándékosan nincs meg" sorát: a `quiz/run` és a `quiz/result` bekötése ugyanígy nyitva
 * marad egy későbbi, a `navigation` modult érintő feladatig.
 *
 * ## Egyszeri, nem reaktív visszaolvasás
 *
 * A [ProgressRepository.getSessionResult] egy már **lezárt vagy megszakított** menetet ad
 * vissza – az attempts listája ettől a pillanattól nem bővül tovább (az egyetlen
 * módosítás, a megszakítás/befejezés, a `quiz/run`-on történt, mire ide a felhasználó
 * eljut). Ezért elegendő egyszeri `suspend` betöltés [loadResult]-ban, nem egy `Flow` –
 * ellentétben a tudástár-tartalommal (lásd [techniqueRepository].observeContentState()]),
 * amely a T-415 „maguktól eltűnő jelzés" (AC-08) miatt továbbra is reaktív marad.
 *
 * ## A hibás technikák feloldása `getTechnique`-tel, nem `observeTechniques()`-szel
 *
 * A [TechniqueRepository.observeTechniques] a visszavont (`retired`) technikákat kiszűri
 * (DR-07) – egy, a menet óta visszavont technika ezért a lekérdezésből eltűnne, és a
 * hibás kérdés tétele feloldatlan maradna. A [TechniqueRepository.getTechnique] ellenben
 * **szándékosan** visszaadja a visszavont sort is (lásd a metódus KDoc-ja), így a korábbi
 * eredmény a tartalomtörlés/visszavonás után is helyesen megjelenik.
 *
 * ## „Hibák újragyakorlása" (T-412): technikaszintű korlátozás, nem kategóriaszűrés
 *
 * A [QuizRequest] motorja csak kategória szerint szűr (`categoryFilter`) – nincs
 * technikaszintű allow-list. A [onRetryWrong] ezért egy **szűkített** [QuizCandidateSet]-et
 * épít, amely KIZÁRÓLAG a hibás kérdések helyes technikáit tartalmazza (társ-technika
 * hozzáadása nélkül): így a [QuizCandidateFilter.correctAnswerPool][hu.kyokushin.trainer.core.quiz.generator.QuizCandidateFilter.correctAnswerPool]
 * is, a distraktorpool is kizárólag ebből a halmazból áll össze – a motor emiatt
 * garantáltan **csak** a hibás technikákat kérdezi újra, semmi mást (US-16 AC-1:
 * „ugyanazokra a technikákra"). Ennek ára a motor már dokumentált, nem-optimista szabálya
 * (lásd `docs/CONVENTIONS.md` 1. szakasz „Egy négyopciós kérdéshez legalább 4 technika
 * kell"): 4-nél kevesebb **egyedi** hibás technikával egyetlen kérdés sem áll össze, és a
 * [onRetryWrong] ilyenkor – a `quiz/setup`-hoz hasonlóan egy előzetes `dryRun()`-nal
 * felismerve – nem indít új, üres munkamenetet (nincs mit duplikálatlanul rögzíteni).
 *
 * @property progressRepository a menet visszaolvasása és az újragyakorló menet nyitása.
 * @property techniqueRepository a hibás tételek és az újragyakorló jelöltkészlet
 *   feloldási forrása, és a lektorálatlanság-szalag (T-415) bemenete.
 * @property quizGenerator az újragyakorló terv generálása (T-412).
 * @property random az injektált RNG, kizárólag az újragyakorló menet seedjének
 *   sorsolására (NFR-07, ADR-007) – ugyanaz a kötés, mint a `QuizSetupViewModel`-nél.
 * @property savedStateHandle a route-argumentumból kapott `sessionId` hordozója.
 */
@HiltViewModel
class QuizResultViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val techniqueRepository: TechniqueRepository,
    private val quizGenerator: QuizGenerator,
    private val random: Random,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val resultPhase = MutableStateFlow<ResultPhase>(ResultPhase.Loading)

    private val _retryEvent = Channel<QuizStartResult>(Channel.BUFFERED)

    /**
     * Egyszeri esemény: a „Hibák újragyakorlása" eredménye (T-412) – ugyanaz a típus
     * ([QuizStartResult]), mint amit a `QuizSetupViewModel.startEvent` ad, mert a
     * hordozott adat (új `sessionId` + új terv + új seed) azonos; a hívó dönt a
     * `quiz/run/{sessionId}`-ra navigálásról (L5).
     */
    val retryEvent: Flow<QuizStartResult> = _retryEvent.receiveAsFlow()

    val uiState: StateFlow<QuizResultUiState> = combine(
        resultPhase,
        techniqueRepository.observeContentState(),
    ) { phase, contentState ->
        buildState(phase, contentState.datasetTrust)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = QuizResultUiState.Loading,
        )

    init {
        viewModelScope.launch { loadResult() }
    }

    /**
     * „Hibák újragyakorlása" (FUNCTIONAL_SPEC 5.3/3., FR-21, T-412).
     *
     * No-op, ha a menet még nincs betöltve, nincs hibás próbálkozás (a UI ilyenkor nem is
     * jelenítené meg a gombot – ez csak másodlagos védelem, ADR-005 mintája), vagy a
     * hibás technikák 4-nél kevesebbek (lásd az osztály KDoc-ja): ezekben az esetekben a
     * [quizGenerator] `dryRun()`-ja 0 várható kérdést adna, és új, üres menetet felesleges
     * lenne nyitni.
     */
    fun onRetryWrong() {
        val loaded = resultPhase.value as? ResultPhase.Loaded ?: return
        val wrongTechniqueIds = loaded.result.attempts.filter { !it.correct }
            .map { it.techniqueId }
            .distinct()
        if (wrongTechniqueIds.isEmpty()) return

        viewModelScope.launch {
            val wrongIdSet = wrongTechniqueIds.toSet()
            val allCandidates = techniqueRepository.getQuizCandidates()
            val retryCandidates = QuizCandidateSet(
                techniques = allCandidates.techniques.filter { it.id in wrongIdSet },
                synonymGroups = allCandidates.synonymGroups
                    .map { group -> group.filterTo(LinkedHashSet()) { it in wrongIdSet } }
                    .filter { it.size >= 2 },
                illustrationDigests = allCandidates.illustrationDigests.filterKeys { it in wrongIdSet },
            )
            val datasetTrust = techniqueRepository.observeContentState().first().datasetTrust
            val probeRequest = QuizRequest(
                candidates = retryCandidates,
                enabledTypes = QuestionType.entries.toSet(),
                requestedCount = wrongTechniqueIds.size,
                seed = PROBE_SEED,
                datasetTrust = datasetTrust,
            )
            // Előzetes `dryRun()`, ugyanúgy, mint a `quiz/setup`-nál: nem nyitunk üres
            // munkamenetet, ha a motor dokumentáltan 0 kérdést adna (lásd az osztály KDoc-ja).
            if (quizGenerator.dryRun(probeRequest).expectedQuestionCount == 0) return@launch

            val seed = random.nextLong()
            val sessionId = progressRepository.startSession(wrongTechniqueIds.size, seed)
            val plan = quizGenerator.generate(probeRequest.copy(seed = seed))
            _retryEvent.send(QuizStartResult(sessionId, plan, seed))
        }
    }

    private suspend fun loadResult() {
        val sessionId = savedStateHandle.get<String>(KEY_SESSION_ID)
            ?.let { raw -> runCatching { SessionId.of(raw) }.getOrNull() }
        val result = sessionId?.let { progressRepository.getSessionResult(it) }
        if (result == null) {
            resultPhase.value = ResultPhase.NotFound
            return
        }
        val neededIds = result.attempts.flatMapTo(LinkedHashSet()) { listOf(it.techniqueId, it.selectedTechniqueId) }
        val techniqueById = neededIds.mapNotNull { id ->
            techniqueRepository.getTechnique(id)?.let { technique -> id to technique }
        }.toMap()
        resultPhase.value = ResultPhase.Loaded(result, techniqueById)
    }

    private fun buildState(phase: ResultPhase, datasetTrust: DatasetTrust): QuizResultUiState = when (phase) {
        ResultPhase.Loading -> QuizResultUiState.Loading
        ResultPhase.NotFound -> QuizResultUiState.NotFound
        is ResultPhase.Loaded -> buildContent(phase, datasetTrust)
    }

    private fun buildContent(phase: ResultPhase.Loaded, datasetTrust: DatasetTrust): QuizResultUiState.Content {
        val attempts = phase.result.attempts
        val score = QuizScoring.score(attempts)
        val wrongItems = attempts.filter { !it.correct }.mapNotNull { attempt ->
            val correct = phase.techniqueById[attempt.techniqueId] ?: return@mapNotNull null
            val selected = phase.techniqueById[attempt.selectedTechniqueId] ?: return@mapNotNull null
            QuizResultWrongItem(
                questionType = attempt.questionType,
                correctTechniqueId = correct.id,
                correctRomaji = correct.romaji,
                correctHungarian = correct.hungarian,
                correctIllustration = correct.illustration,
                selectedTechniqueId = selected.id,
                selectedRomaji = selected.romaji,
                selectedHungarian = selected.hungarian,
            )
        }
        return QuizResultUiState.Content(
            sessionId = phase.result.session.id,
            score = score,
            requestedCount = phase.result.session.requestedCount,
            aborted = !phase.result.session.isFinished,
            wrongItems = wrongItems,
            // Szándékosan a NYERS próbálkozásokból, nem a `wrongItems`-ből (lásd
            // `QuizResultUiState.Content.canRetryWrong` KDoc-ja): egy feloldatlan technika
            // nem veheti el a gombot egy egyébként valódi hibás választól.
            canRetryWrong = attempts.any { !it.correct },
            datasetTrust = datasetTrust,
        )
    }

    /** A betöltés belső állapota – lásd az osztály KDoc-ja. */
    private sealed interface ResultPhase {
        data object Loading : ResultPhase
        data object NotFound : ResultPhase
        data class Loaded(
            val result: QuizSessionResult,
            val techniqueById: Map<TechniqueId, Technique>,
        ) : ResultPhase
    }

    companion object {
        /** A route-argumentum neve – lásd az osztály KDoc-ja. Nem `private`, ugyanazért az okért, mint a `QuizRunViewModel.KEY_SESSION_ID`. */
        const val KEY_SESSION_ID = "sessionId"

        /**
         * A [onRetryWrong] `dryRun()`-jának seedje – a `dryRun()` dokumentáltan nem
         * használ `Random`-ot (lásd `QuizSetupViewModel.DRY_RUN_SEED`), ez az érték soha
         * nem kerül felhasználásra kérdésgeneráláshoz.
         */
        private const val PROBE_SEED = 0L

        /** A konfigurációváltás alatt életben tartja a feliratkozást. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
