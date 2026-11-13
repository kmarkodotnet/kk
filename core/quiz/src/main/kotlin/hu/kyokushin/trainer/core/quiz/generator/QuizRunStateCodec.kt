package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A futó kvíz mentett állapotának (`QuizRunState`, `docs/ARCHITECTURE.md` 6.2, T-405)
 * két cipelendő, nem-primitív része – a [QuizPlan] (kérdések + hiány) és a
 * megválaszoltság ([Map]) – szöveggé alakítása a `SavedStateHandle`-be íráshoz.
 *
 * ## Miért itt, és miért nem a `feature/quiz`-ben
 *
 * A `feature/quiz` modul `build.gradle.kts`-e szándékosan **tiltja** a
 * `kotlinx.serialization` típusokat (ADR-001, L3) – a Compose/ViewModel réteg nem
 * ismerheti a tartalom és az átviteli formátum részleteit. A [QuizPlan] és a
 * [QuizQuestion] viszont már itt, a `core/quiz`-ben `@Serializable` (lásd
 * `QuizIdSerializers.kt` KDoc-ja: „ADR-007, T-405"), mert a `core/quiz` **implementation**
 * (nem `api`) függőségként már látja a `kotlinx-serialization-json`-t (lásd a modul
 * `build.gradle.kts`-ének megjegyzése: „a `main` forráskészlet … csak az annotációt és a
 * `KSerializer`-t" ismerte eddig). Ez a fájl az első hely, ahol a `main` forráskészlet
 * tényleges `Json`-t használ.
 *
 * A `feature/quiz` ViewModelje ezért **sosem** lát `Json`-t vagy `@Serializable`
 * annotációt: csak ezt a négy függvényt hívja, és egy sima `String`-et kap/ad, amit a
 * `SavedStateHandle`-be ír – ugyanúgy, mint a `QuizSetupViewModel` a kategória- és
 * típushalmazokat `ArrayList<String>`-ként (`docs/CONVENTIONS.md` 4.1).
 *
 * @see QuizQuestion a kódolt tartalom – kizárólag azonosítók (ADR-006).
 */
object QuizRunStateCodec {
    private val json = Json { ignoreUnknownKeys = true }

    private val answersSerializer = MapSerializer(QuestionIdSerializer, TechniqueIdSerializer)

    /** A terv (kérdések + esetleges [Shortfall]) kódolása – a [QuizPlan.skippedTypes] is megmarad. */
    fun encodePlan(plan: QuizPlan): String = json.encodeToString(plan)

    /**
     * A terv visszafejtése.
     *
     * A [QuestionIdSerializer] és a [TechniqueIdSerializer] a visszafejtéskor is a
     * `TechniqueId.of(...)`/`QuestionId.of(...)` gyárakon megy át (lásd `QuizIdSerializers.kt`
     * KDoc-ja) – egy sérült vagy manipulált mentett állapot nem tud üres azonosítót
     * becsempészni a domainbe.
     *
     * @throws kotlinx.serialization.SerializationException sérült vagy nem a [encodePlan]
     *   által előállított tartalom esetén. A hívó (`QuizRunViewModel.readInitialInput`)
     *   ezt explicit `runCatching`-gal kezeli, és a kvíz-helyesség (DR-03) megőrzése
     *   érdekében **nem** próbál részleges/sérült tervvel folytatni – egy ilyen bemenetet
     *   a hiányzóval egyenértékűen, `QuizRunUiState.Empty`-ként kezel (NFR-06), nem omlik
     *   össze és nem fogad el manipulált azonosítót.
     */
    fun decodePlan(raw: String): QuizPlan = json.decodeFromString(raw)

    /** A megválaszoltság egyetlen igazsága (ARCHITECTURE 6.2) – kérdésazonosító → választott technika. */
    fun encodeAnswers(answers: Map<QuestionId, TechniqueId>): String =
        json.encodeToString(answersSerializer, answers)

    /** Lásd [decodePlan] – ugyanaz a visszafejtési garancia vonatkozik az azonosítókra. */
    fun decodeAnswers(raw: String): Map<QuestionId, TechniqueId> =
        json.decodeFromString(answersSerializer, raw)
}
