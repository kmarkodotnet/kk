package hu.kyokushin.trainer.feature.quiz.setup

import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.quiz.generator.QuizPlan

/**
 * A „Kvíz indítása" gomb egyszeri eredménye (T-403 Teendők 4., ARCHITECTURE 6.1:
 * „Egyszeri eseményre … `Channel` → `Flow`, nem `StateFlow`").
 *
 * A [QuizSetupViewModel.startEvent]-en keresztül jut el a hívóhoz. A tényleges navigáció
 * (`quiz/run/{sessionId}`) és a [plan] átadása a `quiz/run` képernyőnek a UI-t adó
 * feladat (T-404) és a `QuizRunState` (T-405) felelőssége – ez a típus csak a már
 * elkészült menetet és tervet hordozza, útvonal-döntés nélkül.
 *
 * @property sessionId az újonnan nyitott kvízmenet azonosítója
 *   ([hu.kyokushin.trainer.core.data.repository.ProgressRepository.startSession]).
 * @property plan a menethez tartozó, a friss [sessionId]-vel egyidejűleg sorsolt seedből
 *   generált kvízterv.
 * @property seed a [plan] generálásához sorsolt, injektált RNG-ből jövő mag (ADR-007).
 *   A `QuizPlan` már hordozza a kész kérdéssorrendet, tehát ez önmagában nem kell a
 *   kvíz lejátszásához – a `QuizRunState` (T-405, ARCHITECTURE 6.2) tárgya mégis
 *   explicit módon tartalmazza, ezért innen is tovább kell adni, nem csak a
 *   `QuizSetupViewModel.onStartQuiz` lokális változójában elveszni.
 */
data class QuizStartResult(
    val sessionId: SessionId,
    val plan: QuizPlan,
    val seed: Long,
)
