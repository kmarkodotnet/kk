package hu.kyokushin.trainer.feature.progress.technique

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import java.time.Instant

/**
 * A technika-részletező (`progress/technique/{techniqueId}`) állapota (FUNCTIONAL_SPEC
 * 6.2, FR-25, FR-28, DR-07, T-506).
 *
 * Ugyanaz a döntés, mint a `feature/library` `LibraryDetailUiState`-nél: a
 * lektorálatlanság-szalag minden állapotban – betöltés, tartalom, hiba – látszik, tehát a
 * [datasetTrust] a sealed interfész **közös** tulajdonsága (ADR-010, AC-08).
 */
sealed interface ProgressTechniqueUiState {

    val datasetTrust: DatasetTrust

    /** A technika és a statisztika lekérdezése fut. */
    data class Loading(
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ) : ProgressTechniqueUiState

    /**
     * @property retired `true`, ha a technikát a legfrissebb tartalom már nem
     *   tartalmazza (visszavont, DR-07) – ekkor a képernyő a „Ez a technika már nem része
     *   a tananyagnak, de a korábbi eredményeid megmaradtak." üzenetet mutatja, a
     *   statisztika viszont **továbbra is látható** marad (T-506 „Kész ha").
     * @property score az összes kvízpróbálkozás összesítése (FR-25). `score.answered == 0`
     *   esetén „nincs adat" jár, nem 0% (FR-27, ugyanaz a minta, mint az áttekintőn).
     * @property lastPracticedAt `null`, ha a technikára még sosem érkezett kvízválasz.
     * @property byQuestionType kérdéstípus szerinti bontás (FUNCTIONAL_SPEC 6.2: „melyik
     *   típusban hibázik") – csak az a [QuestionType] szerepel kulcsként, amelyben
     *   legalább egy próbálkozás történt (lásd
     *   [hu.kyokushin.trainer.core.data.repository.TechniqueProgress.byQuestionType]
     *   KDoc-ja).
     * @property selfRatingHistory az önértékelési előzmény, a legfrissebb elöl (FR-28) –
     *   **külön blokkban** jelenik meg, és nem hat a [score]-ra (FR-28).
     */
    data class Content(
        val id: TechniqueId,
        val romaji: String,
        val hungarian: String,
        val illustration: IllustrationRef?,
        val retired: Boolean,
        val score: Score,
        val lastPracticedAt: Instant?,
        val byQuestionType: Map<QuestionType, Score>,
        val selfRatingHistory: List<StudyReview>,
        override val datasetTrust: DatasetTrust,
    ) : ProgressTechniqueUiState

    /**
     * A betöltés kivétellel elhasalt, vagy a technikaazonosító ismeretlen ebben a
     * tudástárban (FUNCTIONAL_SPEC 10.) – ez utóbbi más, mint a [Content.retired]: a
     * [TechniqueRepository.getTechnique] `null`-ja azt jelenti, hogy a tudástár **soha**
     * nem is tartalmazta ezt az azonosítót (lásd
     * [hu.kyokushin.trainer.core.data.repository.TechniqueRepository] KDoc-ja).
     *
     * `@StringRes`, nem `String`: a kivétel angol `message`-e nem juthat a felhasználó
     * elé (NFR-02).
     */
    data class Error(
        @StringRes val messageRes: Int,
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ) : ProgressTechniqueUiState
}
