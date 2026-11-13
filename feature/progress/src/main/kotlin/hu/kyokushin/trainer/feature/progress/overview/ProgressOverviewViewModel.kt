package hu.kyokushin.trainer.feature.progress.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.CategoryProgress
import hu.kyokushin.trainer.core.data.repository.OverallProgress
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.quiz.scoring.Score
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * A Haladás áttekintő képernyő állapotkezelése (FUNCTIONAL_SPEC 6.1, FR-23, FR-24, FR-27,
 * US-17, T-503).
 *
 * ## A `category` tábla a kategóriák egyetlen hiteles forrása
 *
 * A [ProgressRepository.observeCategoryProgress] minden kategóriára pontosan egy sort ad
 * (lásd `docs/CONVENTIONS.md` 1. szakasz „Kategóriánkénti összegzésnél a `FROM` a
 * `category`" sora, T-501) – ez a lista önmagában **nem** hordozza a kategória magyar
 * nevét. A megjelenítéshez ezért a [techniqueRepository]`.observeCategories()`-szel
 * kombinálva épül fel a [ProgressCategoryItem] lista: a kategória nevét és sorrendjét a
 * [TechniqueRepository] adja, a pontszámot a [ProgressRepository] – a két forrás az
 * azonosító ([hu.kyokushin.trainer.core.model.CategoryId]) alapján párosodik, nem index
 * szerint, hogy egy esetleges sorrend-eltérés se okozzon rossz párosítást.
 *
 * ## Miért nincs itt a „Gyakorlást igénylő" lista és az önértékelési blokk
 *
 * A [ProgressOverviewUiState] KDoc-ja részletezi: ez a ViewModel a képernyő **kerete**
 * (T-503), a T-504/T-505 ezt bővíti majd további forrásokkal (`observeNeedsPractice`,
 * `observeStudyReviewSummary`) és UI-state mezőkkel.
 *
 * @property progressRepository az összesített és kategóriánkénti kvízstatisztika forrása.
 * @property techniqueRepository a kategórianevek és a lektorálatlanság-szalag
 *   (ADR-010) bemenete.
 */
@HiltViewModel
class ProgressOverviewViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val techniqueRepository: TechniqueRepository,
) : ViewModel() {

    val uiState: StateFlow<ProgressOverviewUiState> = combine(
        progressRepository.observeOverallProgress(),
        progressRepository.observeCategoryProgress(),
        techniqueRepository.observeCategories(),
        techniqueRepository.observeContentState(),
    ) { overall, categoryProgress, categories, contentState ->
        buildState(overall, categoryProgress, categories, contentState.datasetTrust)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ProgressOverviewUiState.Loading,
        )

    private fun buildState(
        overall: OverallProgress,
        categoryProgress: List<CategoryProgress>,
        categories: List<Category>,
        datasetTrust: DatasetTrust,
    ): ProgressOverviewUiState.Content {
        val scoreByCategory = categoryProgress.associateBy { it.categoryId }
        val items = categories.map { category ->
            ProgressCategoryItem(
                categoryId = category.id,
                nameHu = category.nameHu,
                score = scoreByCategory[category.id]?.score ?: NO_ATTEMPTS_SCORE,
            )
        }
        return ProgressOverviewUiState.Content(
            overallScore = overall.score,
            categories = items,
            datasetTrust = datasetTrust,
        )
    }

    companion object {
        /** A konfigurációváltás alatt életben tartja a feliratkozást. */
        private const val STOP_TIMEOUT_MS = 5_000L

        /**
         * Védelmi háló: ha egy kategóriára véletlenül nem érkezne sor a
         * [ProgressRepository.observeCategoryProgress]-ból (a T-501 szerződése szerint ez
         * nem fordulhat elő, lásd az osztály KDoc-ja), a hívó így is „nincs adat"-ot kap,
         * nem `NullPointerException`-t vagy hamis 0%-ot.
         */
        private val NO_ATTEMPTS_SCORE = Score(answered = 0, correct = 0, ratio = null)
    }
}
