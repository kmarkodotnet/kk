package hu.kyokushin.trainer.feature.progress.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.kyokushin.trainer.core.data.repository.CategoryProgress
import hu.kyokushin.trainer.core.data.repository.OverallProgress
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.StudyReviewSummary
import hu.kyokushin.trainer.core.data.repository.TechniqueProgress
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.localizedName
import hu.kyokushin.trainer.core.quiz.scoring.Score
import hu.kyokushin.trainer.core.ui.locale.currentLanguageTag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A Haladás áttekintő képernyő állapotkezelése (FUNCTIONAL_SPEC 6.1, FR-23, FR-24, FR-26,
 * FR-27, FR-28, US-17, US-11, T-503/T-504/T-505).
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
 * ## A „Gyakorlást igénylő" lista névfeloldása csak az aktív tananyagra néz (T-504)
 *
 * A [ProgressRepository.observeNeedsPractice] kizárólag [TechniqueId]/[Score]/
 * `lastPracticedAt` hármast ad (a `quiz_attempt` táblából, lásd a repository KDoc-ja) –
 * a romaji/magyar név és az ábra a [TechniqueRepository]`.observeTechniques()`-ből jön,
 * ugyanazzal az `associateBy { it.id }` + `map` mintával, mint a kategórianeveknél. Az
 * [TechniqueRepository.observeTechniques] **csak az aktív** (nem visszavont) technikákat
 * adja (lásd a repository KDoc-ja) – egy visszavont, de korábban gyakorolt technika ezért
 * **kiesik** ebből a listából ([buildNeedsPracticeItems] `mapNotNull`-ja), nem jelenik meg
 * hibásan név nélkül. A teljes, visszavontakat is tartalmazó statisztika a
 * technika-részletezőn (T-506) továbbra elérhető marad (DR-07) – ez a két képernyő
 * szándékosan eltérő hatóköre, nem hiányosság.
 *
 * ## Az önértékelési blokk sosem keveredik a kvízstatisztikával (T-505, FR-28)
 *
 * A [ProgressRepository.observeStudyReviewSummary] a `study_review` tábla összegzése,
 * teljesen független forrásból jön, mint a kvíz `Score`-ok – a [ProgressOverviewUiState]
 * ezért két, egymástól független mezőt ad ki ([ProgressOverviewUiState.Content.overallScore]
 * és [ProgressOverviewUiState.Content.studyReviewSummary]), nem egy közös típust.
 *
 * @property progressRepository az összesített, kategóriánkénti és technikánkénti
 *   kvízstatisztika, valamint az önértékelés-összegzés forrása.
 * @property techniqueRepository a kategória- és technikanevek, az ábrák, és a
 *   lektorálatlanság-szalag (ADR-010) bemenete.
 */
@HiltViewModel
class ProgressOverviewViewModel
    @Inject
    constructor(
        private val progressRepository: ProgressRepository,
        private val techniqueRepository: TechniqueRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ProgressOverviewUiState> = combine(
            combine(
                progressRepository.observeOverallProgress(),
                progressRepository.observeCategoryProgress(),
                techniqueRepository.observeCategories(),
                techniqueRepository.observeContentState(),
            ) { overall, categoryProgress, categories, contentState ->
                OverviewAndCategorySources(overall, categoryProgress, categories, contentState.datasetTrust)
            },
            progressRepository.observeNeedsPractice(NEEDS_PRACTICE_LIMIT),
            techniqueRepository.observeTechniques(),
            progressRepository.observeStudyReviewSummary(),
        ) { sources, needsPractice, techniques, studyReviewSummary ->
            buildState(sources, needsPractice, techniques, studyReviewSummary)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ProgressOverviewUiState.Loading,
            )

        private fun buildState(
            sources: OverviewAndCategorySources,
            needsPractice: List<TechniqueProgress>,
            techniques: List<Technique>,
            studyReviewSummary: StudyReviewSummary,
        ): ProgressOverviewUiState.Content {
            val languageTag = currentLanguageTag()
            val scoreByCategory = sources.categoryProgress.associateBy { it.categoryId }
            val categoryItems = sources.categories.map { category ->
                ProgressCategoryItem(
                    categoryId = category.id,
                    nameHu = category.localizedName(languageTag),
                    score = scoreByCategory[category.id]?.score ?: NO_ATTEMPTS_SCORE,
                )
            }
            return ProgressOverviewUiState.Content(
                overallScore = sources.overall.score,
                categories = categoryItems,
                needsPractice = buildNeedsPracticeItems(needsPractice, techniques, languageTag),
                studyReviewSummary = studyReviewSummary,
                datasetTrust = sources.datasetTrust,
            )
        }

        private fun buildNeedsPracticeItems(
            needsPractice: List<TechniqueProgress>,
            techniques: List<Technique>,
            languageTag: String,
        ): List<ProgressNeedsPracticeItem> {
            val techniqueById = techniques.associateBy { it.id }
            // `mapNotNull`: lásd az osztály KDoc-ja – egy visszavont technika korábbi
            // próbálkozása nincs az aktív `techniques` listában, ezért kimarad innen.
            return needsPractice.mapNotNull { progress ->
                val technique = techniqueById[progress.techniqueId] ?: return@mapNotNull null
                ProgressNeedsPracticeItem(
                    techniqueId = progress.techniqueId,
                    romaji = technique.romaji,
                    hungarian = technique.localizedName(languageTag),
                    illustration = technique.illustration,
                    score = progress.score,
                    // A `TechniqueProgress` szerződése szerint `answered >= 1` esetén soha
                    // nem `null` (lásd `observeNeedsPractice` KDoc-ja) – ez a lista eleve csak
                    // ilyen tételeket ad.
                    lastPracticedAt = requireNotNull(progress.lastPracticedAt) {
                        "observeNeedsPractice csak legalább egy próbálkozással rendelkező " +
                            "technikát ad, tehát lastPracticedAt itt sosem null."
                    },
                )
            }
        }

        /** A kategóriakerethez tartozó négy forrás egy pillanatra összefogva (lásd fent). */
        private data class OverviewAndCategorySources(
            val overall: OverallProgress,
            val categoryProgress: List<CategoryProgress>,
            val categories: List<Category>,
            val datasetTrust: DatasetTrust,
        )

        companion object {
            /** A konfigurációváltás alatt életben tartja a feliratkozást. */
            private const val STOP_TIMEOUT_MS = 5_000L

            /** A „Gyakorlást igénylő" lista legnagyobb mérete (FUNCTIONAL_SPEC 6.1/4., FR-26). */
            private const val NEEDS_PRACTICE_LIMIT = 10

            /**
             * Védelmi háló: ha egy kategóriára véletlenül nem érkezne sor a
             * [ProgressRepository.observeCategoryProgress]-ból (a T-501 szerződése szerint ez
             * nem fordulhat elő, lásd az osztály KDoc-ja), a hívó így is „nincs adat"-ot kap,
             * nem `NullPointerException`-t vagy hamis 0%-ot.
             */
            private val NO_ATTEMPTS_SCORE = Score(answered = 0, correct = 0, ratio = null)
        }
    }
