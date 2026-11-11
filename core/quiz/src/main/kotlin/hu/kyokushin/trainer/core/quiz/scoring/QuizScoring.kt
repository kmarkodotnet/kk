package hu.kyokushin.trainer.core.quiz.scoring

import hu.kyokushin.trainer.core.model.QuizAttempt

/**
 * Egy kvízmenet (vagy bármely próbálkozás-halmaz) összegzett eredménye (FR-20).
 *
 * A `ratio` **szándékosan nullable**: 0 megválaszolt kérdés esetén `null`, nem `0.0`
 * (FR-27). Ez a típusválasztás az, ami megakadályozza, hogy a UI véletlenül 0%-ot írjon
 * ki ott, ahol a „nincs adat" a helyes válasz – egy `0.0` alapérték csendben elfedné a
 * két eset különbségét, és egy hozzá sem kezdett kvíz úgy látszódna, mint egy végig
 * elhibázott.
 *
 * Az érték **levezetett**, nem tárolt számláló (DR-09): mindig a próbálkozásokból
 * számolódik, ezért nem tud a tárolt adattal ellentmondásba kerülni.
 *
 * @property answered a ténylegesen megválaszolt (és egyszer számolt) kérdések száma.
 *   A meg nem válaszolt kérdés nem szerepel benne – és **nem** hibásként (FR-19).
 * @property correct ezek közül a helyesek száma.
 * @property ratio a sikerarány `0.0`–`1.0` között, vagy `null`, ha [answered] `== 0`.
 *   Nem százalék: a formázás – és a „nincs adat" szöveg – a UI dolga, magyarul,
 *   `strings.xml`-ből.
 */
data class Score(
    val answered: Int,
    val correct: Int,
    val ratio: Double?,
) {
    init {
        require(answered >= 0) { "Score.answered nem lehet negatív: $answered" }
        require(correct in 0..answered) {
            "Score.correct a 0..answered tartományban kell lennie: correct=$correct, answered=$answered"
        }
        if (answered == 0) {
            // A „nincs adat" és a „0%" két külön állapot (FR-27): a típus szintjén is
            // kizárjuk, hogy egy üres összegzés 0.0 aránnyal keletkezzen.
            require(ratio == null) { "0 megválaszolt kérdés esetén a ratio null, nem $ratio" }
        } else {
            val value = requireNotNull(ratio) { "answered=$answered esetén a ratio nem lehet null" }
            require(value in 0.0..1.0) { "Score.ratio a 0.0..1.0 tartományba esik: $value" }
        }
    }
}

/**
 * Eredményszámítás **tiszta függvényként** (FR-20, FR-27, DR-09, TR-04).
 *
 * Nincs állapot, nincs mellékhatás, nincs óra- és nincs véletlenhívás (NFR-07): ugyanaz
 * a bemeneti lista mindig ugyanazt a [Score]-t adja. Emiatt a kvízmenet lezárása és a
 * haladás-képernyő aggregálása ugyanezt a függvényt hívhatja, két külön számolás helyett.
 *
 * **Nem számol újra helyességet.** A [QuizAttempt.correct] mezőt összegzi, nem hasonlítja
 * újra a két azonosítót: a szinonimacsoportos helyesség megítélése a kvízmotor dolga, és
 * azt az ítéletet kell megőrizni (DR-09, lásd a `QuizAttempt` dokumentációját).
 */
object QuizScoring {

    /**
     * Összegzi a megválaszolt kérdéseket.
     *
     * **Egy kérdés egyszer számít (FR-18).** Ugyanarra a kérdésre érkezett több
     * próbálkozásból az **első válasz** számít – a párhuzamos rekordokat a
     * `(sessionId, questionId)` páron csoportosítva szűrjük, és a legkorábbi
     * `answeredAt` nyer; azonos időbélyegnél a lista elsőként felsorolt eleme. A tárolás
     * szintjén ezt a `UNIQUE(sessionId, questionId)` index garantálja (ADR-005), de a
     * számítás nem támaszkodik rá: a UI dupla koppintása vagy egy újrajátszott
     * eseménysor sem duplázhatja az eredményt.
     *
     * Több munkamenet próbálkozásai is átadhatók egyszerre (a haladás-képernyő így
     * aggregál): a kérdésazonosító munkameneten belül egyedi, ezért a kulcs a pár, nem
     * csak a `questionId`.
     *
     * @param attempts a figyelembe vett próbálkozások; a sorrend nem befolyásolja az
     *   eredményt (csak a pontosan egyidejű ütközések feloldását).
     * @return a megválaszolt és helyes darabszám, valamint a sikerarány – üres bemenetre
     *   `Score(0, 0, null)`.
     */
    fun score(attempts: List<QuizAttempt>): Score {
        val distinct = attempts
            .withIndex()
            .groupBy { (_, attempt) -> attempt.sessionId to attempt.questionId }
            .values
            .map { group ->
                group.minWith(
                    compareBy({ (_, attempt) -> attempt.answeredAt }, { (index, _) -> index }),
                ).value
            }

        val answered = distinct.size
        val correct = distinct.count { it.correct }
        // A nulla osztás helyett nincs arány: a hívó a null-t „nincs adat"-ként jeleníti meg.
        val ratio = if (answered == 0) null else correct.toDouble() / answered.toDouble()
        return Score(answered = answered, correct = correct, ratio = ratio)
    }
}
