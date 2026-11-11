package hu.kyokushin.trainer.core.quiz.validation

/**
 * A csomagolt tudástár validátora (DR-05, FR-31).
 *
 * Három, szándékosan szűk szerződési szabály:
 *
 * 1. **Nem dob kivételt.** Bármilyen érvénytelen tartalomra is a [ValidationReport] a
 *    válasz – a hiba adat, nem vezérlésátadás. Ez az a garancia, ami miatt az app
 *    hibás JSON-tól sem omlik össze (FR-31, US-01 3. elfogadási kritérium).
 * 2. **Nem ismeri a fájlrendszert, az Androidot és az órát.** A képfájlok létezése
 *    bemenet ([KnowledgeInput.availableIllustrationFiles]), nincs I/O, nincs
 *    `Instant.now()` és nincs `Random.Default` (NFR-07). Ezért tisztán JVM-en
 *    tesztelhető (TR-01), és ezért Android-mentes a `core/quiz` modul (ADR-001).
 * 3. **Soha nem emel státuszt.** A hitelesítési szint és a kvízjogosultság csak
 *    *lefelé* mozdulhat: `quizEligible` → `false`, státusz → `AWAITING_REVIEW` vagy
 *    `INCOMPLETE`, `Asset` → `Placeholder` (CR-02, ADR-002, ADR-010). A felemeléshez
 *    nincs és nem is lehet kódút.
 *
 * Az implementáció **determinisztikus és tiszta**: ugyanaz a [KnowledgeInput] mindig
 * ugyanazt a jelentést adja, és a hívás semmit nem módosít a bemeneten.
 */
fun interface KnowledgeValidator {

    /**
     * Validálja a tudástárat, és strukturált jelentést ad.
     *
     * @param input a manifest, a technikarekordok és a csomagban elérhető képfájlnevek.
     * @return a jelentés; a hiba soha nem kivétel, hanem a jelentés része.
     */
    fun validate(input: KnowledgeInput): ValidationReport
}
