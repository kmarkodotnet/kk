package hu.kyokushin.trainer.core.data.db

import hu.kyokushin.trainer.core.data.repository.TechniqueProgress
import hu.kyokushin.trainer.core.model.QuizAttempt
import hu.kyokushin.trainer.core.model.QuizSession
import hu.kyokushin.trainer.core.model.StudyReview
import hu.kyokushin.trainer.core.quiz.scoring.Score

/**
 * Felhasználói adat entitás → domain leképezés (T-411, G-09).
 *
 * Ugyanaz a minta, mint a `ContentEntityMappers.kt`-ban: a mapper a `core/data`-ban él,
 * mert a bemeneti entitások `internal`-ok, és a `ProgressRepository` publikus szerződése
 * (`getSessionResult`) kizárólag `core/model` domain típust adhat vissza (ADR-001 L4).
 *
 * Eddig nem volt rá szükség (a `ProgressRepository.recordAttempt`/`startSession` csak
 * **ír**, nem olvas vissza domain objektumot – lásd `docs/CONVENTIONS.md` 1. szakasz),
 * a [hu.kyokushin.trainer.core.data.repository.ProgressRepository.getSessionResult] az első
 * olvasó tag, amelynek tényleg entitás → domain fordításra van szüksége.
 */

internal fun QuizSessionEntity.toQuizSession(): QuizSession = QuizSession(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    requestedCount = requestedCount,
    answeredCount = answeredCount,
    seed = seed,
)

internal fun QuizAttemptEntity.toQuizAttempt(): QuizAttempt = QuizAttempt(
    id = id,
    sessionId = sessionId,
    questionId = questionId,
    techniqueId = techniqueId,
    questionType = questionType,
    selectedTechniqueId = selectedTechniqueId,
    correct = correct,
    answeredAt = answeredAt,
)

/**
 * Egy önértékelés sor → domain leképezés (FR-28, T-506) – a technika-részletező
 * önértékelési előzményéhez ([hu.kyokushin.trainer.core.data.repository.ProgressRepository.observeStudyReviewHistory]).
 */
internal fun StudyReviewEntity.toStudyReview(): StudyReview = StudyReview(
    id = id,
    techniqueId = techniqueId,
    selfRating = selfRating,
    reviewedAt = reviewedAt,
)

/**
 * Projekció → [Score] leképezés a Haladás aggregált statisztikáihoz (FR-23–FR-27, T-501).
 *
 * Mind a négy `toScore()` túlterhelés ugyanazt az egyetlen szabályt alkalmazza (FR-27,
 * DR-09): `ratio = null`, ha `answered == 0`, különben `correctCount / answered`. A
 * [Score] konstruktora ezt magától is kikényszerítené (lásd `core/quiz`
 * `Score.init`), de a `null`-ágat itt, a forrásnál kell eldönteni – egy `0.0`
 * alapértékkel hívott konstruktor kivételt dobna.
 */

private fun ratioOrNull(answered: Int, correct: Int): Double? =
    if (answered == 0) null else correct.toDouble() / answered.toDouble()

internal fun OverallScoreRow.toScore(): Score = Score(
    answered = answered,
    correct = correctCount,
    ratio = ratioOrNull(answered, correctCount),
)

internal fun CategoryScoreRow.toScore(): Score = Score(
    answered = answered,
    correct = correctCount,
    ratio = ratioOrNull(answered, correctCount),
)

internal fun TechniqueScoreRow.toScore(): Score = Score(
    answered = answered,
    correct = correctCount,
    ratio = ratioOrNull(answered, correctCount),
)

internal fun TechniqueQuestionTypeScoreRow.toScore(): Score = Score(
    answered = answered,
    correct = correctCount,
    ratio = ratioOrNull(answered, correctCount),
)

/**
 * [TechniqueScoreRow] + a hozzá tartozó kérdéstípus-bontás → [TechniqueProgress]
 * (FR-25, FR-26, T-501). Közös pont a `ProgressRepository.observeTechniqueProgress`
 * (egyetlen technika) és a `ProgressRepository.observeNeedsPractice` (legfeljebb 10
 * technika) között – mindkettő ugyanide fut össze, hogy a két `Flow` sosem térhet el
 * egymástól abban, hogyan lesz egy sorból [TechniqueProgress].
 */
internal fun TechniqueScoreRow.toTechniqueProgress(
    byQuestionType: List<TechniqueQuestionTypeScoreRow>,
): TechniqueProgress = TechniqueProgress(
    techniqueId = techniqueId,
    score = toScore(),
    lastPracticedAt = lastPracticedAt,
    byQuestionType = byQuestionType.associate { it.questionType to it.toScore() },
)
