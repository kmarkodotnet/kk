package hu.kyokushin.trainer.core.model

/**
 * Egy Kyokushin technika a tudástárból – a tartalom domain modellje (DR-01).
 *
 * Minden mező `val`: a betöltött technika a validátoron átjutva többé nem változik, a
 * tartalomfrissítés új példányt állít elő (upsert), nem mutálja a meglévőt (ADR-004).
 *
 * **A `quizEligible` szándékosan nincs a modellben.** A kvízjogosultság származtatott
 * érték (ADR-010), nem tárolt adat: a [status], a [quizExcluded], a [retired], a
 * kérdéstípus mezőkövetelményei és a tudástár bizalmi szintje ([DatasetTrust]) együtt
 * döntik el, a kvízmotorban. Ha itt tárolnánk, két igazságforrás lenne, és egy elavult
 * `true` érték kvízbe engedne hiányos rekordot.
 *
 * @property id stabil és örök azonosító. Átnevezéskor nem változik, visszavonás után sem
 *   használható újra más fogalomra (DR-06) – erre épül a felhasználói előrehaladás
 *   megőrzése a tartalomfrissítésnél (FR-33, TR-05).
 * @property categoryId a [Category] hivatkozása. Külön típus, hogy a [TechniqueId]-val
 *   való felcserélése fordítási hiba legyen (DR-03).
 * @property romaji a technika eredeti neve latin átírásban – ez a hiteles forma, a
 *   diakritikus jelekkel együtt (pl. `gedan barai`, `tsuki` hosszú magánhangzói).
 * @property romajiAscii a [romaji] diakritikus jelek nélküli, kisbetűs alakja. Azért
 *   tárolt mező és nem futásidőben számolt, mert a keresés és a szinonimaütközés-vizsgálat
 *   (V006) minden összehasonlításnál ezt használja, és a normalizálás szabálya a
 *   tartalomimport felelőssége – nem a lekérdezésé.
 * @property hungarian a magyar jelentés. Kötelező mező: a tanulás és a kvíz A/B típusa
 *   erre épül.
 * @property japaneseScript az eredeti japán írásmód, ha ismert. Megjelenítésre szolgál,
 *   kvízben nem kérdezzük (nincs kana/kanji ismeret feltételezve).
 * @property aliases további elfogadott elnevezések (dialektus, iskolánkénti eltérés).
 *   Üres lista, soha nem `null`. A distraktorválasztás ezeket is nézi, hogy két
 *   egymás szinonimája ne kerüljön egyszerre ugyanabba a kérdésbe (V006).
 * @property descriptionHu rövid magyar leírás, ha van.
 * @property illustration a képhivatkozás három állapota (ADR-003); `null`, ha a rekord
 *   nem hivatkozik illusztrációra.
 * @property status háromállapotú hitelesítési státusz (ADR-002) – **nem** boolean, mert
 *   a UI-nak három különböző üzenetet kell adnia, és a státuszt kód soha nem emelheti
 *   (CR-02).
 * @property quizExcluded a tartalom szerkesztői döntése: ez a rekord hiteles és
 *   megjelenik a tudástárban, de a kvízből kimarad. Független a [status]-tól és az
 *   illusztráció [IllustrationRef.Asset.quizUsable] kapujától.
 * @property retired a technika kikerült az aktuális tananyagból. Nem töröljük, csak
 *   jelöljük (ADR-004), hogy a korábbi próbálkozások hivatkozása értelmes maradjon; a
 *   listákból, a tanulásból és a kvízjelöltek közül kiesik.
 * @property datasetVersion a `manifest.json` `contentVersion` értéke, amelyből ez a
 *   rekord származik. Diagnosztikához és a tartalomfrissítés idempotenciájához kell:
 *   látszik, melyik kiadásból jött a sor.
 */
data class Technique(
    val id: TechniqueId,
    val categoryId: CategoryId,
    val romaji: String,
    val romajiAscii: String,
    val hungarian: String,
    val japaneseScript: String? = null,
    val aliases: List<String> = emptyList(),
    val descriptionHu: String? = null,
    val illustration: IllustrationRef? = null,
    val status: VerificationStatus = VerificationStatus.AWAITING_REVIEW,
    val quizExcluded: Boolean = false,
    val retired: Boolean = false,
    val datasetVersion: String,
) {
    init {
        require(romaji.isNotBlank()) { "Technique.romaji kötelező mező (DR-01)." }
        require(romajiAscii.isNotBlank()) { "Technique.romajiAscii kötelező mező (DR-01)." }
        require(hungarian.isNotBlank()) { "Technique.hungarian kötelező mező (DR-01)." }
        require(datasetVersion.isNotBlank()) { "Technique.datasetVersion kötelező mező." }
        require(aliases.none { it.isBlank() }) { "Technique.aliases nem tartalmazhat üres elemet (V009)." }
    }
}
