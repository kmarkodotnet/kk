package hu.kyokushin.trainer.core.quiz.generator

/**
 * A kvízgenerátor szerződése (`docs/ARCHITECTURE.md` 4.2).
 *
 * Tiszta Kotlin: nincs `android.*`, nincs `Context`, nincs Room, nincs Compose (ADR-001).
 * A bemenet és a kimenet immutable adatstruktúra, így a kvízhelyesség teljes egészében
 * JVM unit teszttel lefedhető, emulátor nélkül (TR-02, TR-03).
 *
 * **Garanciák, amelyek minden implementációra kötelezőek:**
 *
 * 1. Egyik metódus sem dob kivételt elégtelen adat miatt. Az „nem jött össze" eset
 *    strukturált kimenet: [QuizPlan.skippedTypes], [QuizPlan.shortfall] (FR-22).
 * 2. A motor soha nem **gyárt** opciót: minden opció létező technikarekordra hivatkozik,
 *    és minden kérdésben pontosan négy, páronként különböző opció van, köztük pontosan
 *    egy helyes (FR-14). Ha ez nem teljesíthető, a kérdés nem jön létre.
 * 3. Determinizmus: ugyanaz a [QuizRequest] – ugyanaz a seed – ugyanazt a [QuizPlan]-t
 *    adja, az opciók sorrendjével együtt. Kizárólag a seedelt RNG használható; nincs
 *    `Random.Default`, nincs rendszeróra, és nincs `Set`-iterációs sorrendre épülő
 *    logika (NFR-07, ADR-007).
 * 4. A [dryRun] és a [generate] **ugyanazon** a szűrő- és jogosultsági logikán osztozik:
 *    `dryRun(r).expectedQuestionCount == generate(r).questionCount` minden `r`-re (T-111).
 */
interface QuizGenerator {
    /**
     * Összeállítja a kvíz tervét.
     *
     * @param request a teljes, önhordó bemenet.
     * @return a terv; üres kérdéslistával is érvényes válasz, ha a kérésből nem állítható
     *   össze helyes kérdés – ilyenkor a [QuizPlan.canStart] hamis, és a kihagyás okait a
     *   terv hordozza.
     */
    fun generate(request: QuizRequest): QuizPlan

    /**
     * Megmondja, mi állna össze ugyanebből a kérésből – a kvízbeállító képernyő élő
     * összegzéséhez (FR-13, FR-22).
     *
     * Nem kell teljes tervet építenie, de a [generate]-tel megegyező eredményt kell
     * ígérnie: ha a kettő eltér, a beállító képernyő félrevezeti a felhasználót.
     */
    fun dryRun(request: QuizRequest): QuizReadiness
}
