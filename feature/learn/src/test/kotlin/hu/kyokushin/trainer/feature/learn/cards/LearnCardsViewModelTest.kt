package hu.kyokushin.trainer.feature.learn.cards

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.feature.learn.ALL_LEARN_TECHNIQUES
import hu.kyokushin.trainer.feature.learn.CATEGORY_DACHI
import hu.kyokushin.trainer.feature.learn.CATEGORY_TSUKI
import hu.kyokushin.trainer.feature.learn.FakeLearnProgressRepository
import hu.kyokushin.trainer.feature.learn.FakeLearnTechniqueRepository
import hu.kyokushin.trainer.feature.learn.HideMode
import hu.kyokushin.trainer.feature.learn.TECHNIQUE_CHUDAN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * A tanulókártya ViewModel viselkedése – emulátor nélkül (T-303, T-307, FR-06, FR-09,
 * NFR-07).
 *
 * Robolectric kell hozzá, mert a [SavedStateHandle] Android `Bundle` fölött validálja a
 * tárolt értékeket (CONVENTIONS 3.). `@Config(sdk = [34])` – a `compileSdk = 37`-hez nincs
 * `android-all` csomag.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearnCardsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------- seedelt sorrend / NFR-07

    @Test
    fun learnCards_seedComesFromInjectedRandom_notSystemDefault_NFR07() = runTest {
        // Két FÜGGETLEN `Random(1)` példány: az egyik a ViewModelbe megy, a másikból a
        // teszt maga számolja ki a várt seedet és a várt sorrendet – ha a ViewModel
        // `Random.Default`-ot vagy rendszerórát használna, a két eredmény nem egyezne.
        val expectedSeed = Random(1).nextLong()
        val categories = setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))
        val expectedOrder = ALL_LEARN_TECHNIQUES
            .sortedBy { it.id.value }
            .shuffled(Random(expectedSeed))
            .map { it.id }

        val viewModel = viewModel(
            savedStateHandle = handleFor(categories),
            random = Random(1),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(fullOrder(viewModel)).isEqualTo(expectedOrder)
        job.cancel()
    }

    @Test
    fun learnCards_sameSeed_producesIdenticalOrderAfterRecreate_FR09() = runTest {
        val handle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki")))

        val first = viewModel(savedStateHandle = handle, random = Random(42))
        val firstJob = collectState(first)
        advanceUntilIdle()
        val firstOrder = fullOrder(first)
        firstJob.cancel()

        // Ugyanaz a `SavedStateHandle` – a seed már benne van, tehát egy MÁSIK `Random`
        // példány átadása sem változtat az eredményen (ADR-007 mintája: a `Random(seed)`
        // determinisztikus, a konstruktor-paraméter RNG csak a seed EGYSZERI sorsolására
        // kell, ha még nincs mentve).
        val restored = viewModel(savedStateHandle = handle, random = Random(777))
        val restoredJob = collectState(restored)
        advanceUntilIdle()
        val restoredOrder = fullOrder(restored)
        restoredJob.cancel()

        assertThat(restoredOrder).isEqualTo(firstOrder)
    }

    // ------------------------------------------------------------------ pozíció / FR-09

    @Test
    fun learnCards_positionSurvivesRecreateWithSameSavedStateHandle_FR09() = runTest {
        val handle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki")))

        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()
        first.onNext()
        advanceUntilIdle()
        val movedPosition = (first.uiState.value as LearnCardsUiState.Content).position
        val movedTechniqueId = (first.uiState.value as LearnCardsUiState.Content).card.techniqueId
        assertThat(movedPosition).isEqualTo(1)
        firstJob.cancel()

        // Forgatás / mentett állapotból visszaállított folyamat: ugyanaz a handle.
        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()
        val restoredContent = restored.uiState.value as LearnCardsUiState.Content
        assertThat(restoredContent.position).isEqualTo(movedPosition)
        assertThat(restoredContent.card.techniqueId).isEqualTo(movedTechniqueId)
        restoredJob.cancel()
    }

    @Test
    fun learnCards_previousAtStart_isNoOp_FR09() = runTest {
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val start = viewModel.uiState.value as LearnCardsUiState.Content
        assertThat(start.position).isEqualTo(0)
        assertThat(start.hasPrevious).isFalse()

        // Az elején az „Előző" no-op, nem megy negatívba.
        viewModel.onPrevious()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).position).isEqualTo(0)
        job.cancel()
    }

    /**
     * Az utolsó kártya utáni „Következő" a záróképernyőre visz (T-307,
     * FUNCTIONAL_SPEC 4.2: „Az utolsó kártya után záróképernyő"), **nem** marad az
     * utolsó kártyán – ez a T-303 eredeti elvárásához képest szándékos változás, amit a
     * T-304/T-307 pár vezetett be (lásd `LearnCardsViewModel` osztály KDoc-ja).
     */
    @Test
    fun learnCards_nextAfterLastCard_showsFinishedScreen_FR06() = runTest {
        val categories = setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))
        val viewModel = viewModel(savedStateHandle = handleFor(categories))
        val job = collectState(viewModel)
        advanceUntilIdle()

        val start = viewModel.uiState.value as LearnCardsUiState.Content
        val total = start.total

        repeat(total - 1) {
            viewModel.onNext()
            advanceUntilIdle()
        }
        val end = viewModel.uiState.value as LearnCardsUiState.Content
        assertThat(end.position).isEqualTo(total - 1)
        assertThat(end.hasNext).isFalse()

        // A „Következő" az utolsó kártyán is engedélyezett (T-304 UI), és a
        // záróképernyőre visz, nem marad az utolsó kártyán.
        viewModel.onNext()
        advanceUntilIdle()
        val finished = viewModel.uiState.value as LearnCardsUiState.Finished
        assertThat(finished.total).isEqualTo(total)
        assertThat(finished.categoryIds).isEqualTo(categories)
        job.cancel()
    }

    @Test
    fun learnCards_restartAfterFinished_returnsToFirstCardWithoutReveal_FR06() = runTest {
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onReveal()
        val total = (viewModel.uiState.value as LearnCardsUiState.Content).total
        repeat(total) {
            viewModel.onNext()
            advanceUntilIdle()
        }
        assertThat(viewModel.uiState.value).isInstanceOf(LearnCardsUiState.Finished::class.java)

        viewModel.onRestart()
        advanceUntilIdle()
        val restarted = viewModel.uiState.value as LearnCardsUiState.Content
        assertThat(restarted.position).isEqualTo(0)
        assertThat(restarted.card.revealed).isFalse()
        job.cancel()
    }

    // --------------------------------------------------------------- felfedettség / FR-08

    @Test
    fun learnCards_revealIsPerCard_notGlobal_FR09() = runTest {
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).card.revealed).isFalse()
        viewModel.onReveal()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).card.revealed).isTrue()

        viewModel.onNext()
        advanceUntilIdle()
        // A KÖVETKEZŐ kártya alapállapotban nincs felfedve – a felfedettség kártyánként
        // tárolt, nem globális kapcsoló (FUNCTIONAL_SPEC 4.2 „Viselkedési szabályok").
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).card.revealed).isFalse()

        viewModel.onPrevious()
        advanceUntilIdle()
        // Visszalépve az első, már felfedett kártya felfedve marad (US-10 4. kritérium).
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).card.revealed).isTrue()
        job.cancel()
    }

    // ----------------------------------------------------------- önértékelés / FR-10, FR-28

    @Test
    fun learnCards_onKnew_whenRevealed_recordsStudyReviewAndAdvances_FR10() = runTest {
        val progressRepository = FakeLearnProgressRepository()
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
            progressRepository = progressRepository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val firstTechniqueId = (viewModel.uiState.value as LearnCardsUiState.Content).card.techniqueId
        viewModel.onReveal()
        advanceUntilIdle()

        viewModel.onKnew()
        advanceUntilIdle()

        assertThat(progressRepository.recordedReviews)
            .containsExactly(firstTechniqueId to SelfRating.KNEW)
        // Automatikus továbblépés (FUNCTIONAL_SPEC 4.2/4.).
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).position).isEqualTo(1)
        job.cancel()
    }

    @Test
    fun learnCards_onNeedsPractice_whenRevealed_recordsStudyReviewAndAdvances_FR10() = runTest {
        val progressRepository = FakeLearnProgressRepository()
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
            progressRepository = progressRepository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val firstTechniqueId = (viewModel.uiState.value as LearnCardsUiState.Content).card.techniqueId
        viewModel.onReveal()
        advanceUntilIdle()

        viewModel.onNeedsPractice()
        advanceUntilIdle()

        assertThat(progressRepository.recordedReviews)
            .containsExactly(firstTechniqueId to SelfRating.NEEDS_PRACTICE)
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).position).isEqualTo(1)
        job.cancel()
    }

    /**
     * Védelmi háló (ADR-005 mintája): a UI (T-304) felfedésig letiltva tartja a két
     * gombot, de a ViewModel maga is ellenőrzi az állapotot – egy fel nem fedett
     * kártyán érkező hívás nem ír `StudyReview`-t és nem lép tovább.
     */
    @Test
    fun learnCards_onKnew_whenNotRevealed_isNoOp_FR10() = runTest {
        val progressRepository = FakeLearnProgressRepository()
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
            progressRepository = progressRepository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).card.revealed).isFalse()

        viewModel.onKnew()
        advanceUntilIdle()

        assertThat(progressRepository.recordedReviews).isEmpty()
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).position).isEqualTo(0)
        job.cancel()
    }

    /**
     * Önértékelés nélküli továbblépés is megengedett (T-306 Teendők 3. pontja): a sima
     * „Következő" gomb (nem az önértékelés) nem ír `StudyReview`-t.
     */
    @Test
    fun learnCards_onNext_withoutSelfRating_doesNotRecordStudyReview_FR10() = runTest {
        val progressRepository = FakeLearnProgressRepository()
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
            progressRepository = progressRepository,
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        viewModel.onReveal()
        viewModel.onNext()
        advanceUntilIdle()

        assertThat(progressRepository.recordedReviews).isEmpty()
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).position).isEqualTo(1)
        job.cancel()
    }

    @Test
    fun learnCards_onKnew_onLastCard_advancesToFinishedScreen_FR10() = runTest {
        val categories = setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))
        val progressRepository = FakeLearnProgressRepository()
        val viewModel = viewModel(savedStateHandle = handleFor(categories), progressRepository = progressRepository)
        val job = collectState(viewModel)
        advanceUntilIdle()

        val total = (viewModel.uiState.value as LearnCardsUiState.Content).total
        repeat(total - 1) {
            viewModel.onNext()
            advanceUntilIdle()
        }
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).hasNext).isFalse()

        viewModel.onReveal()
        advanceUntilIdle()
        viewModel.onKnew()
        advanceUntilIdle()

        assertThat(progressRepository.recordedReviews).hasSize(1)
        assertThat(viewModel.uiState.value).isInstanceOf(LearnCardsUiState.Finished::class.java)
        job.cancel()
    }

    // ------------------------------------------------------------- kategórianév / FR-06

    @Test
    fun learnCards_cardCarriesItsOwnCategoryName_FR06() = runTest {
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki"))),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        val content = viewModel.uiState.value as LearnCardsUiState.Content
        val expectedCategoryName = if (content.card.techniqueId == TECHNIQUE_CHUDAN.id) {
            CATEGORY_TSUKI.nameHu
        } else {
            CATEGORY_DACHI.nameHu
        }
        assertThat(content.card.categoryName).isEqualTo(expectedCategoryName)
        job.cancel()
    }

    @Test
    fun learnCards_revealedStateSurvivesRecreateWithSameSavedStateHandle_FR09() = runTest {
        val handle = handleFor(setOf(CategoryId.of("dachi"), CategoryId.of("tsuki")))

        val first = viewModel(savedStateHandle = handle)
        val firstJob = collectState(first)
        advanceUntilIdle()
        first.onReveal()
        advanceUntilIdle()
        firstJob.cancel()

        val restored = viewModel(savedStateHandle = handle)
        val restoredJob = collectState(restored)
        advanceUntilIdle()
        assertThat((restored.uiState.value as LearnCardsUiState.Content).card.revealed).isTrue()
        restoredJob.cancel()
    }

    // -------------------------------------------------------------------- üres kijelölés

    @Test
    fun learnCards_selectionWithoutTechniques_showsEmpty_FR09() = runTest {
        val viewModel = viewModel(
            savedStateHandle = handleFor(setOf(CategoryId.of("keri"))),
        )
        val job = collectState(viewModel)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(LearnCardsUiState.Empty)
        job.cancel()
    }

    // ----------------------------------------------------------------------- segédek

    private fun handleFor(
        categories: Set<CategoryId>,
        hideMode: HideMode = HideMode.HIDE_JAPANESE,
    ): SavedStateHandle = SavedStateHandle(
        mapOf(
            LearnCardsViewModel.KEY_CATEGORY_IDS to categories.joinToString(
                LearnCardsViewModel.CATEGORY_ID_SEPARATOR,
            ) { it.value },
            LearnCardsViewModel.KEY_HIDE_MODE to hideMode.name,
        ),
    )

    private fun TestScope.viewModel(
        repository: FakeLearnTechniqueRepository = FakeLearnTechniqueRepository(),
        progressRepository: FakeLearnProgressRepository = FakeLearnProgressRepository(),
        random: Random = Random(0),
        savedStateHandle: SavedStateHandle,
    ): LearnCardsViewModel = LearnCardsViewModel(
        repository = repository,
        progressRepository = progressRepository,
        random = random,
        defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
        savedStateHandle = savedStateHandle,
    )

    /** Aktív feliratkozó: a `WhileSubscribed` nélküle nem számol állapotot. */
    private fun TestScope.collectState(viewModel: LearnCardsViewModel): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    /**
     * Az összes kártya azonosítója, elejétől a végéig, kizárólag a publikus
     * `onPrevious()`/`onNext()`-tel bejárva.
     *
     * Először a legelejéig lépked vissza – a kapott [viewModel] pozíciója a megosztott
     * `SavedStateHandle` miatt nem feltétlenül 0, ha egy korábbi lépés már mozdított rajta
     * (pl. egy korábbi [fullOrder] hívás vagy egy „pozíció túléli az újraépülést" teszt).
     */
    private suspend fun TestScope.fullOrder(viewModel: LearnCardsViewModel): List<TechniqueId> {
        while ((viewModel.uiState.value as LearnCardsUiState.Content).hasPrevious) {
            viewModel.onPrevious()
            advanceUntilIdle()
        }

        val order = mutableListOf<TechniqueId>()
        while (true) {
            val content = viewModel.uiState.value as LearnCardsUiState.Content
            order += content.card.techniqueId
            if (!content.hasNext) break
            viewModel.onNext()
            advanceUntilIdle()
        }
        return order
    }
}
