package hu.kyokushin.trainer.core.model

import java.time.Instant

/**
 * Egy kvízmenet – felhasználói adat (DR-04).
 *
 * A [finishedAt] **nullable**, és ez nem kényelmi döntés: a megszakított kvíz munkamenete
 * befejezetlen marad (FR-19). A félbehagyott menet addig megválaszolt kérdései
 * beszámítanak az eredménybe, de a menet nem kap lezárási időpontot – így a
 * „befejezetlen" és a „lezárt, de hibás" eset nem keverhető össze.
 *
 * A [seed] tárolása az ADR-007 döntése: a kvíz indításakor az **injektált** RNG-ből
 * sorsolt seed ide és a `SavedStateHandle`-be is bekerül, így egy lezárt vagy megszakított
 * kvíz utólag bitre reprodukálható (hibajelentés, „pontosan ezt a kvízt láttam"). A
 * `QuizGenerator` kizárólag ezt a seedet használja – nincs `Random.Default` és nincs
 * rendszeróra-alapú sorsolás (NFR-07).
 *
 * @property id a menet azonosítója. Itt – a [QuizAttempt]-tel és a [StudyReview]-val
 *   ellentétben – **nem** generált `Long`, hanem [SessionId]: a menetre a UI, a futásállapot
 *   és a próbálkozások már a beszúrás előtt hivatkoznak, ezért az azonosítót a domain adja,
 *   nem az adatbázis.
 * @property startedAt a menet indulása, **injektált** `Clock`-ból (NFR-07). `Instant`
 *   `minSdk 26`-tól natív, ezért nem kell desugaring (ADR-009).
 * @property finishedAt a lezárás időpontja, vagy `null`, ha a menet befejezetlen (FR-19).
 * @property requestedCount ahány kérdést a felhasználó kért a beállító képernyőn. Ennél
 *   kevesebb kérdés is generálódhat, ha nincs elég jelölt – az eltérést a kvízmotor
 *   készenléti összegzése jelzi.
 * @property answeredCount ahány kérdésre tényleg válaszolt. A sikerarány **csak** ebből
 *   számolódik (FR-20, DR-09): a meg nem válaszolt kérdések nem számítanak hibásnak.
 * @property seed a menet sorsolási seedje (ADR-007).
 */
data class QuizSession(
    val id: SessionId,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val requestedCount: Int,
    val answeredCount: Int = 0,
    val seed: Long,
) {
    init {
        require(requestedCount > 0) { "QuizSession.requestedCount pozitív kell legyen." }
        require(answeredCount >= 0) { "QuizSession.answeredCount nem lehet negatív." }
        require(answeredCount <= requestedCount) {
            "QuizSession.answeredCount ($answeredCount) nem lehet több a kért kérdésszámnál ($requestedCount)."
        }
        require(finishedAt == null || !finishedAt.isBefore(startedAt)) {
            "QuizSession.finishedAt nem lehet korábbi a startedAt-nál."
        }
    }

    /**
     * A menet le van-e zárva. Számított érték, nem tárolt jelző: két igazságforrás
     * (egy `Boolean` és a [finishedAt]) elcsúszhatna egymástól.
     */
    val isFinished: Boolean get() = finishedAt != null
}
