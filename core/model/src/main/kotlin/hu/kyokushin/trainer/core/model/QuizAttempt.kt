package hu.kyokushin.trainer.core.model

import java.time.Instant

/**
 * Egy rögzített kvízválasz – a felhasználói adat domain modellje (DR-04).
 *
 * A tartalomtól **külön életciklus** (DR-07): ez a rekord a tudástár frissítésekor nem
 * törlődik, és nincs róla `CASCADE DELETE` a tartalom felé (ADR-004). Emiatt hivatkozik
 * [TechniqueId]-ra, nem beágyazott [Technique] példányra: ha a technikát átnevezik vagy
 * visszavonják, a próbálkozás hivatkozása akkor is értelmes marad (FR-33, TR-05).
 *
 * **Miért tárolja a [questionId]-t is?** Az „egy kérdésre pontosan egy válasz" szabályt
 * (FR-18) a `quiz_attempt` tábla `UNIQUE(sessionId, questionId)` indexe garantálja
 * (ADR-005). Ugyanaz a technika egy munkameneten belül elvileg több kérdésben is
 * szerepelhet, ezért a `(sessionId, techniqueId)` pár **nem** lenne helyes kulcs – az
 * index csak a kérdésazonosítóval építhető meg.
 *
 * @property id a Room által generált elsődleges kulcs. `0`, amíg a rekord nincs
 *   beszúrva: a `@PrimaryKey(autoGenerate = true)` ezt az értéket cseréli ki a valódi
 *   kulcsra. Szándékosan `Long` és nem value class – a kulcsot a perzisztencia adja, nem
 *   a domain, és soha nem hordoz üzleti jelentést.
 * @property sessionId melyik kvízmenethez tartozik a válasz.
 * @property questionId a megválaszolt kérdés azonosítója a menetben. Az egyszeri válasz
 *   kikényszerítésének kulcsa (ADR-005).
 * @property techniqueId a kérdés **helyes** válaszának technikája.
 * @property questionType a kérdés típusa. Azért tárolt, mert a haladás-képernyő
 *   típusonként bontva is mutatja az eredményt, és utólag nem következtethető ki.
 * @property selectedTechniqueId amit a felhasználó **választott**. A helyesség
 *   összehasonlítása azonosítóval történik, soha nem szöveges névegyezéssel (DR-03).
 * @property correct a válasz helyes volt-e. Redundánsnak tűnik a két azonosító mellett,
 *   de nem az: a `JP_TO_IMAGE`/`IMAGE_TO_JP` típusoknál szinonimacsoport esetén a
 *   helyesség megítélése a kvízmotor dolga, és azt az ítéletet kell megőrizni –
 *   a statisztika ezt a mezőt összegzi, nem számol újra (DR-09).
 * @property answeredAt a válasz időpontja, **injektált** `Clock`-ból (NFR-07). A domain
 *   soha nem hívja az `Instant.now()`-t. `Instant` használható desugaring nélkül, mert
 *   `minSdk 26`-tól natív (ADR-009).
 */
data class QuizAttempt(
    val id: Long,
    val sessionId: SessionId,
    val questionId: QuestionId,
    val techniqueId: TechniqueId,
    val questionType: QuestionType,
    val selectedTechniqueId: TechniqueId,
    val correct: Boolean,
    val answeredAt: Instant,
) {
    init {
        require(id >= 0L) { "QuizAttempt.id nem lehet negatív (0 = még nem perzisztált rekord)." }
    }
}
