package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionType

/**
 * A `dryRun()` kimenete: mire számíthat a felhasználó, **mielőtt** elindítja a kvízt
 * (FR-13, FR-22, T-111).
 *
 * A kvízbeállító képernyő élő összegzése ebből készül („10 kérdést kértél, 6 állítható
 * össze – a képes kérdésekhez nincs elég illusztráció"). Ezért a készenléti összegzésnek
 * **ugyanazt** a szűrő- és jogosultsági logikát kell használnia, mint a `generate()`-nek:
 * ha a kettő eltérne, a képernyő hazudna. Erre külön teszt van (T-112).
 *
 * Ez a típus szándékosan nem szerializálható: nem állapot, hanem egy pillanatnyi
 * származtatott összegzés, amit a beállítások változásakor újra kell számolni.
 *
 * @property requestedCount a kért kérdésszám, ahogy a kérésben szerepelt.
 * @property expectedQuestionCount ennyi kérdés áll össze. A `generate()` ugyanerre a
 *   kérésre pontosan ennyi kérdést ad.
 * @property availableByType kérdéstípusonként hány kérdés **állítható össze** – nem a
 *   jelöltek száma, hanem a teljes értékű, négyopciós kérdések száma. Csak azok a típusok
 *   szerepelnek benne, amelyek nem maradtak ki.
 * @property skippedTypes melyik engedélyezett típus maradt ki és miért – a magyar üzenet
 *   és a javaslat bemenete (FR-22).
 * @property expectedShortfall `null`, ha a kért kérdésszám kijön; egyébként ugyanaz a
 *   [Shortfall], amit a `generate()` is adni fog.
 */
data class QuizReadiness(
    val requestedCount: Int,
    val expectedQuestionCount: Int,
    val availableByType: Map<QuestionType, Int> = emptyMap(),
    val skippedTypes: Map<QuestionType, SkipReason> = emptyMap(),
    val expectedShortfall: Shortfall? = null,
) {
    init {
        require(requestedCount > 0) { "A kért kérdésszám pozitív (FR-13)." }
        require(expectedQuestionCount >= 0) { "A várható kérdésszám nem lehet negatív." }
        require(expectedQuestionCount <= requestedCount) {
            "A terv soha nem ad több kérdést a kértnél (FR-13)."
        }
        require((expectedShortfall != null) == (expectedQuestionCount < requestedCount)) {
            "Shortfall pontosan akkor van, ha a várható kérdésszám kisebb a kértnél (FR-22)."
        }
        require(expectedShortfall == null || expectedShortfall.actualCount == expectedQuestionCount) {
            "A shortfall tényleges kérdésszáma és a várható kérdésszám nem térhet el."
        }
    }

    /** Elindítható-e kvíz ezekkel a beállításokkal (FR-22). */
    val canStart: Boolean
        get() = expectedQuestionCount > 0
}
