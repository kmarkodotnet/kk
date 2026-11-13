package hu.kyokushin.trainer.core.quiz.validation

/**
 * Egy konkrét validációs észrevétel a tudástár betöltésekor (DR-05, FR-31).
 *
 * Ez a típus a validátor **egyetlen hibacsatornája**: érvénytelen tartalomra soha nem
 * keletkezik kivétel, hanem ilyen rekord kerül a [ValidationReport.issues] listájába.
 * Debug buildben a teljes lista megjelenik a fejlesztői diagnosztikán (FR-45).
 *
 * @property code a megsértett szabály azonosítója (V001…V010).
 * @property severity a tényleges súlyosság. Nem feltétlenül azonos a
 *   [IssueCode.defaultSeverity]-vel: ugyanaz a szabály szigorúbban is érvényesülhet, ha a
 *   rekord emiatt kvízképtelenné válik – a jelentésben mindig a tényleges hatás szerepel.
 * @property entryId az érintett rekord **nyers** `id`-ja, a fájlban szereplő alakban, vagy
 *   `null`, ha a hiba a tudástár egészét érinti ([IssueScope.DATASET]). Szándékosan
 *   `String`, nem [hu.kyokushin.trainer.core.model.TechniqueId]: az `id` maga is lehet
 *   üres vagy hiányzó (V002), és a `TechniqueId.of` ilyenkor kivételt dobna – azt a
 *   rekordot pedig éppen jelenteni kell, nem elveszíteni.
 * @property entryIndex az érintett rekord nulla alapú indexe a [KnowledgeInput.entries]
 *   listában, ha a hiba egy rekordhoz köthető. Enélkül a hiányzó `id`-jú rekordok
 *   jelentései megkülönböztethetetlenek lennének egymástól.
 * @property field az érintett mező neve a sémában (pl. `hungarian`, `categoryId`), vagy
 *   `null`, ha a szabály a rekord egészére vonatkozik.
 * @property detail magyar nyelvű, fejlesztőnek és tartalomszerkesztőnek szóló leírás.
 *   Diagnosztikai szöveg; nem végfelhasználói UI-felirat, ezért nincs `strings.xml`-ben.
 */
data class ValidationIssue(
    val code: IssueCode,
    val severity: Severity,
    val entryId: String? = null,
    val entryIndex: Int? = null,
    val field: String? = null,
    val detail: String = "",
)

/**
 * Egy validációs észrevétel súlyossága.
 *
 * Azt mondja meg, hogy a szabálysértés **elvet-e** valamit, vagy csak jelzi. Hogy *mit*
 * vet el, az [IssueScope] döntése.
 */
enum class Severity {
    /** A szabálysértés elvetéssel vagy lefokozással jár (lásd [IssueScope]). */
    BLOCKING,

    /** A szabálysértés nem vet el semmit, de a jelentésben és a diagnosztikán látszik. */
    WARNING,
}

/**
 * Egy [BLOCKING][Severity.BLOCKING] szabálysértés hatókörének a mértéke.
 *
 * A puszta [Severity] nem elegendő: a V001 („nem támogatott `schemaVersion`") a *teljes*
 * betöltést elutasítja, a V002 („hiányzó kötelező mező") egyetlen *rekordot* vet el, a
 * V007 pedig csak egy *mezőt* fokoz le – mindhárom BLOKKOLÓ. A hatókör nélkül a hívó nem
 * tudná eldönteni, szabad-e a [ValidationReport.accepted] listára támaszkodni.
 */
enum class IssueScope {
    /** A teljes tudástár elutasítva; a jelentés `accepted` listája üres. */
    DATASET,

    /** Az érintett rekord elvetve, a [ValidationReport.rejected] listára került. */
    RECORD,

    /** A rekord megmarad, csak az érintett mező fokozódik le (pl. `quizEligible` → `false`). */
    FIELD,
}

/**
 * A tudástár-validáció szabálykódjai (DR-05; a szabálytáblázat a
 * `.claude/skills/kyo-knowledge-base/SKILL.md` 4. pontjában él).
 *
 * A kódok **stabilak**: a jelentésben, a fejlesztői diagnosztikán és a TR-01 tesztekben is
 * ezek a hivatkozási pontok, ezért sem a nevük, sem a jelentésük nem változhat
 * utólag – új szabály új kódot kap.
 *
 * @property defaultSeverity a szabály szokásos súlyossága a szabálytáblázat szerint.
 * @property scope BLOKKOLÓ esetben mit vet el a szabály; WARNING esetén csak azt írja le,
 *   mihez tartozik az észrevétel.
 */
enum class IssueCode(
    val defaultSeverity: Severity,
    val scope: IssueScope,
) {
    /** A `schemaVersion` nem támogatott vagy hiányzik → a teljes betöltés elutasítva. */
    V001(Severity.BLOCKING, IssueScope.DATASET),

    /** Kötelező mező üres, blank vagy hiányzik → a rekord elvetve. */
    V002(Severity.BLOCKING, IssueScope.RECORD),

    /** Az `id` nem egyedi a fájlon belül → a *későbbi* rekord elvetve. */
    V003(Severity.BLOCKING, IssueScope.RECORD),

    /** A `categoryId` nem szerepel a manifest kategóriái között → a rekord elvetve. */
    V004(Severity.BLOCKING, IssueScope.RECORD),

    /**
     * A képhivatkozás nem szerepel a manifest nyilvántartásában, vagy a fájl nincs meg a
     * csomagban → a rekord megmarad, de `Placeholder` képpel, és kiesik a képes kvízből.
     */
    V005(Severity.WARNING, IssueScope.FIELD),

    /**
     * Két rekord neve (`romaji`, `hungarian` vagy `aliases`) normalizálva azonos → a pár
     * kölcsönös distraktor-kizárási csoportba kerül ([ValidationReport.synonymGroups], CR-07).
     */
    V006(Severity.WARNING, IssueScope.FIELD),

    /** `quizEligible = true` nem hitelesített rekordon → `quizEligible` lefokozva `false`-ra. */
    V007(Severity.BLOCKING, IssueScope.FIELD),

    /** `quizEligible = true`, de a kérdéstípushoz szükséges mezők hiányosak → lefokozva. */
    V008(Severity.BLOCKING, IssueScope.FIELD),

    /** Üres vagy a saját `romaji`/`hungarian` értékkel ütköző alias → az alias eldobva. */
    V009(Severity.WARNING, IssueScope.FIELD),

    /**
     * A JSON egyáltalán nem értelmezhető → a betöltés elutasítva, diagnosztika készül.
     * Ezt a kódot nem a validátor állítja elő, hanem a bemenetet összeállító importréteg
     * (`core/data`), amikor a deszerializálás meghiúsul, és a validátor sosem fut le.
     */
    V010(Severity.BLOCKING, IssueScope.DATASET),
}
