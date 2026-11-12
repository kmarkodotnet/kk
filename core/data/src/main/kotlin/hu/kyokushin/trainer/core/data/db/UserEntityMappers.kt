package hu.kyokushin.trainer.core.data.db

import hu.kyokushin.trainer.core.model.QuizAttempt
import hu.kyokushin.trainer.core.model.QuizSession

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
