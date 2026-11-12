package hu.kyokushin.trainer.feature.quiz.run

import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizQuestion
import hu.kyokushin.trainer.core.quiz.generator.Shortfall

/**
 * A futó kvíz teljes, forgatást túlélő állapota (`docs/ARCHITECTURE.md` 6.2 – „a
 * legkritikusabb pont"; T-405, FR-19, NFR-07, US-15).
 *
 * ## Miért plain Kotlin osztály, nem `@Serializable`
 *
 * A `feature/quiz` modul `build.gradle.kts`-e tiltja a `kotlinx.serialization` típusokat
 * (ADR-001, L3 – lásd a fájl fejkommentjét). Ez a típus ezért **nem** az, amit a
 * [androidx.lifecycle.SavedStateHandle] közvetlenül tárol: a [QuizRunViewModel] a
 * [sessionId]-t, a [seed]-et és a [currentIndex]-et primitívként (`String`/`Long`/`Int`),
 * a [questions]+[shortfall] páros és az [answers] map pedig a `core/quiz`
 * `QuizRunStateCodec` által kódolt `String`-ként kerül a `SavedStateHandle`-be – ott a
 * kódolás/visszafejtés tényleges `Json`-t igényel, ami csak ott engedett (lásd
 * `QuizRunStateCodec` KDoc-ja). Ez az osztály a ViewModel **olvasási oldalon**
 * visszaállított, egyben tartott nézete ezekre a darabokra – ugyanaz a szerep, mint a
 * `QuizSetupViewModel.RequestInputs`-é.
 *
 * ## Miért nincs újragenerálás forgatáskor
 *
 * A [seed] és a [questions] is a mentett állapot része: forgatás után a [QuizRunViewModel]
 * egyszerűen visszaolvassa mindkettőt, nem futtatja újra a `QuizGenerator`-t. Ha egy
 * jövőbeli hívási út (pl. folyamat-újraindítás a mentett `SavedStateHandle` nélkül) mégis
 * újragenerálná a tervet ugyanabból a [seed]-ből és kérésből, a motor determinizmusa
 * (ADR-007) garantálja, hogy **bitre** ugyanazt a [questions] listát adja – a [seed]
 * mentése ezért a fallback eset bitre-azonosságának a biztosítéka, nem az elsődleges
 * visszaállítási út.
 *
 * @property sessionId a kvízmenet azonosítója
 *   ([hu.kyokushin.trainer.core.data.repository.ProgressRepository.startSession]).
 * @property seed a terv generálásához sorsolt RNG-mag (ADR-007) – lásd a fenti KDoc-ot.
 * @property questions a terv kérdései a lejátszás sorrendjében, **kizárólag
 *   azonosítókkal** (ADR-006): a megjelenítendő nevet/képet a UI oldja fel a tudástárból.
 * @property currentIndex az éppen megjelenített kérdés 0-alapú pozíciója. Csak előre
 *   mozog (T-410 „Teendők": „a kérdések között nincs visszalépés"), és nem léphet a
 *   kérdéslista utolsó indexén túl ebben a csoportban (G-07) – a kvíz befejezése
 *   (eredményképernyőre lépés) a G-09 csoport (T-411) tárgya, lásd [QuizRunViewModel].
 * @property answers a megválaszoltság **egyetlen igazsága** (ARCHITECTURE 6.2): melyik
 *   kérdésre melyik technikát választotta a felhasználó. Egy kérdés `questions`-beli
 *   azonosítója legfeljebb egyszer szerepelhet kulcsként – ezt a [QuizRunViewModel]
 *   `onAnswer`-je kényszeríti ki (FR-18, T-406), nem ez a típus.
 * @property shortfall a terv esetleges hiánya (FR-22) – csak megjelenítési adat, a
 *   lejátszást nem befolyásolja.
 */
internal data class QuizRunState(
    val sessionId: SessionId,
    val seed: Long,
    val questions: List<QuizQuestion>,
    val currentIndex: Int,
    val answers: Map<QuestionId, TechniqueId>,
    val shortfall: Shortfall?,
)
