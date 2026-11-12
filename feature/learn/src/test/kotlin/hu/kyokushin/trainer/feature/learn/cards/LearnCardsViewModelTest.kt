package hu.kyokushin.trainer.feature.learn.cards

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.feature.learn.ALL_LEARN_TECHNIQUES
import hu.kyokushin.trainer.feature.learn.FakeLearnTechniqueRepository
import hu.kyokushin.trainer.feature.learn.HideMode
import kotlin.random.Random
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

/**
 * A tanulókártya ViewModel viselkedése – emulátor nélkül (T-303, FR-09, NFR-07).
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
    fun learnCards_nextAndPrevious_respectBounds_FR09() = runTest {
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

        // A végéig lépve a `hasNext` lezár, és további `onNext()` nem lép túl.
        val total = start.total
        repeat(total - 1) {
            viewModel.onNext()
            advanceUntilIdle()
        }
        val end = viewModel.uiState.value as LearnCardsUiState.Content
        assertThat(end.position).isEqualTo(total - 1)
        assertThat(end.hasNext).isFalse()

        viewModel.onNext()
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as LearnCardsUiState.Content).position).isEqualTo(total - 1)
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
        random: Random = Random(0),
        savedStateHandle: SavedStateHandle,
    ): LearnCardsViewModel = LearnCardsViewModel(
        repository = repository,
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
