package hu.kyokushin.trainer.feature.progress.overview

import hu.kyokushin.trainer.core.data.repository.StudyReviewSummary
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import java.time.Instant

/**
 * A Haladás áttekintő képernyő állapota (FUNCTIONAL_SPEC 6.1, FR-23, FR-24, FR-26, FR-27,
 * FR-28, T-503/T-504/T-505).
 *
 * Ez a típus a képernyő mindhárom alszakaszát hordozza: az összesítő kártya és a
 * kategóriánkénti bontás (T-503), a „Gyakorlást igénylő" lista (T-504, FR-26,
 * [needsPractice]) és az önértékelési blokk (T-505, FR-28, [studyReviewSummary]) – a
 * `docs/backlog/GROUPS.md` G-10 csoportja ezt a három alszakaszt egy menetben viszi.
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
     * @property needsPractice a legfeljebb 10, legtöbb gyakorlást igénylő technika,
     *   rendezve (FR-26, lásd
     *   [hu.kyokushin.trainer.core.data.repository.ProgressRepository.observeNeedsPractice]).
     *   Üres lista – soha nem `null` –, ha még egyetlen kvízpróbálkozás sincs; ilyenkor a
     *   képernyő a „Még nincs elég adat a javaslathoz." üzenetet mutatja a lista helyén.
     *   Egy olyan technika, amelyre volt próbálkozás, de a legfrissebb tartalom már nem
     *   tartalmazza (visszavont), **nem** szerepel ebben a listában – a nevét/ábráját adó
     *   [hu.kyokushin.trainer.core.data.repository.TechniqueRepository.observeTechniques]
     *   csak az aktív technikákat adja (lásd [ProgressOverviewViewModel] KDoc-ja); a
     *   visszavont technika korábbi statisztikája a technika-részletezőn (T-506) továbbra
     *   is elérhető marad (DR-07).
     * @property studyReviewSummary a tanulókártya-önértékelések összegzése besorolás
     *   szerint (FR-28, FUNCTIONAL_SPEC 6.1/5.) – **külön, elhatárolt** szakaszban jelenik
     *   meg, és soha nem keveredik a kvízsikerarányba.
     * @property datasetTrust a tudástár bizalmi szintje (ADR-010): a Haladás képernyő
     *   fejlécében a lektorálatlanság-szalag bemenete, ugyanaz a minta, mint a
     *   kvíz képernyőin (T-415).
     */
    data class Content(
        val overallScore: Score,
        val categories: List<ProgressCategoryItem>,
        val needsPractice: List<ProgressNeedsPracticeItem>,
        val studyReviewSummary: StudyReviewSummary,
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

/**
 * Egy tétel a „Gyakorlást igénylő" listában (FUNCTIONAL_SPEC 6.1/4., FR-26, T-504).
 *
 * @property score legalább egy próbálkozással rendelkezik ([Score.answered]`>= 1`, lásd
 *   [ProgressOverviewUiState.Content.needsPractice] KDoc-ja) – a „nincs adat" ág ide
 *   soha nem jut el, mert a sosem gyakorolt technika nem kerül be a listába (FR-26
 *   „Figyelj rá").
 * @property lastPracticedAt az utolsó rögzített próbálkozás időpontja – ugyanazon okból
 *   soha nem `null` itt.
 */
data class ProgressNeedsPracticeItem(
    val techniqueId: TechniqueId,
    val romaji: String,
    val hungarian: String,
    val illustration: IllustrationRef?,
    val score: Score,
    val lastPracticedAt: Instant,
)
