package hu.kyokushin.trainer.feature.progress.overview

import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.quiz.scoring.Score

/**
 * A Haladás áttekintő képernyő állapota (FUNCTIONAL_SPEC 6.1, FR-23, FR-24, FR-27, T-503).
 *
 * Ez a típus a képernyő **kerete**: az összesítő kártya és a kategóriánkénti bontás
 * (T-503 Teendők 1–4.). A „Gyakorlást igénylő" lista (T-504, FR-26) és az önértékelési
 * blokk (T-505, FR-28) **szándékosan nincs itt** – a `docs/backlog/GROUPS.md` G-10
 * csoportja ezt a képernyőt az alszakaszok kereteként jelöli ki, a két további
 * szakaszt külön feladat adja hozzá a [Content] alá, bővítve ezt a típust.
 */
sealed interface ProgressOverviewUiState {

    /** Az aggregált statisztika `Flow`-jai még nem adtak értéket. */
    data object Loading : ProgressOverviewUiState

    /**
     * @property overallScore az összes rögzített kvízpróbálkozás összesítése (FR-23,
     *   [hu.kyokushin.trainer.core.data.repository.ProgressRepository.observeOverallProgress]).
     *   [Score.answered]`== 0` esetén a képernyő a „Még nincs adat. Indíts egy kvízt!"
     *   üzenetet és gombot jeleníti meg a kártya helyén, **nem** 0%-ot (FR-27).
     * @property categories a kategóriánkénti bontás, a tartalom sorrendjében (FR-24) –
     *   **minden** kategóriára pontosan egy elem jut, próbálkozás nélkül is (lásd
     *   [ProgressCategoryItem]).
     * @property datasetTrust a tudástár bizalmi szintje (ADR-010): a Haladás képernyő
     *   fejlécében a lektorálatlanság-szalag bemenete, ugyanaz a minta, mint a
     *   kvíz képernyőin (T-415).
     */
    data class Content(
        val overallScore: Score,
        val categories: List<ProgressCategoryItem>,
        val datasetTrust: DatasetTrust,
    ) : ProgressOverviewUiState
}

/**
 * Egy kategória sora a kategóriánkénti bontásban (FUNCTIONAL_SPEC 6.1/3., FR-24).
 *
 * @property score ha a kategóriában még nincs próbálkozás, `score.answered == 0` és
 *   `score.ratio == null` – a sor ilyenkor „nincs adat"-ot jelenít meg, nem 0%-ot
 *   (FUNCTIONAL_SPEC 6.1/3.).
 */
data class ProgressCategoryItem(
    val categoryId: CategoryId,
    val nameHu: String,
    val score: Score,
)
