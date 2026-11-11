package hu.kyokushin.trainer.core.data.repository

import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import kotlinx.coroutines.flow.Flow

/**
 * A tartalom **olvasási** felülete a feature modulok felé (ARCHITECTURE 5.3, NFR-03, DR-07).
 *
 * Ez a `core/data` publikus szerződése a tudástárra; a megvalósítás `internal`, és a Hilt
 * köti be (T-203). A szerződésben **egyetlen** Room típus sem szerepel – se entitás, se
 * DAO, se DTO –, csak `core/model` domain modell és a `core/quiz` jelöltkészlete
 * (ADR-001 L3/L4, TR-07). Írásra nincs metódus: a tartalmat kizárólag a
 * [hu.kyokushin.trainer.core.data.content.ContentImporter] írja.
 *
 * ## A `retired` szűrés a repository felelőssége (ADR-004, DR-07)
 *
 * A visszavont technika sora megmarad, hogy a rá hivatkozó `quiz_attempt` és
 * `study_review` rekordok feloldhatók maradjanak (FR-33, TR-05) – de a tananyagból
 * kiesik. Ezt a szűrést **nem a hívó** végzi: ha egy képernyő elfelejtené, visszavont
 * technikák jelennének meg a listában vagy a kvízben, hangos hiba nélkül. Ezért:
 *
 * | Metódus | `retired` sorok |
 * |---|---|
 * | [observeCategories] | — |
 * | [observeTechniques] | **kiszűrve** |
 * | [observeTechniqueMatches] | **kiszűrve** |
 * | [getQuizCandidates] | **kiszűrve** |
 * | [getTechnique] | **benne van**, lásd lent |
 *
 * A [getTechnique] a szándékos kivétel: a Haladás képernyő technika-részletezője éppen
 * azt a technikát nyitja meg, amelyre egy korábbi próbálkozás hivatkozik, és ki kell
 * írnia, hogy „ez a technika már nem része a tananyagnak, de a korábbi eredményeid
 * megmaradtak" (FUNCTIONAL_SPEC 6.2, DR-07). Ha itt is szűrnénk, ez a képernyő nem
 * tudná megmondani, mire hivatkozik az előrehaladás. A [Technique.retired] zászló a
 * domain modell része, tehát a hívó a *megjelenítésről* dönt, nem a szűrésről.
 *
 * ## `Flow` vs. `suspend`
 *
 * Megfigyelésre `Flow` (a lista és a tartalomállapot magától frissül, amikor a
 * tartalombetöltés upsertel), pontszerű lekérdezésre `suspend`. Blokkoló metódus nincs,
 * és minden adatbázis-művelet az injektált `@IoDispatcher`-en fut – a hívó UI szálát
 * egyetlen metódus sem terheli (NFR-01, NFR-10).
 */
interface TechniqueRepository {

    /**
     * A kategóriák a **tartalom által diktált** sorrendben (DR-02, ADR-011).
     *
     * Nem ábécé szerint: a kategóriák oktatási logikát követnek (állások → ütések → …),
     * és a magyar nevük tartalmi adat, nem `strings.xml` erőforrás.
     */
    fun observeCategories(): Flow<List<Category>>

    /**
     * Az aktív technikák a keresőszöveggel és a kategóriaszűrővel szűkítve.
     *
     * @param query szabad szöveges keresés; üres vagy csak whitespace érték esetén nincs
     *   szöveges szűrés. A keresés ékezet- és kisbetű-érzéketlen, részszóra illeszkedik, és
     *   a romaji névre, a magyar jelentésre és az aliasokra egyszerre nézi
     *   (FR-03) – a normalizálás ugyanaz a szabály, amit a validátor használ.
     * @param categories a megtartandó kategóriák. **Üres halmaz jelentése: minden
     *   kategória** – ez a szűrő kijelölésmentes állapota, nem „semmi". Ugyanaz a
     *   konvenció, mint a kvízkérés kategóriaszűrőjénél (FR-11).
     * @return a találatok a `romajiAscii` szerint rendezve; visszavont technika nincs
     *   közöttük.
     */
    fun observeTechniques(
        query: String = "",
        categories: Set<CategoryId> = emptySet(),
    ): Flow<List<Technique>>

    /**
     * Ugyanaz a keresés, mint az [observeTechniques], de a találat megmondja, **melyik
     * alias** egyezett (FR-03, FUNCTIONAL_SPEC 3.1, US-06 3. kritérium).
     *
     * A Tudástár listája ezt a metódust használja: alias alapján talált tétel alatt a
     * „Egyezés: `{alias}`" sor csak akkor jelenhet meg, ha a repository megmondja, mi
     * illeszkedett. Az [observeTechniques] ennek a vetülete – ott, ahol az egyezés
     * forrása nem érdekes (kategóriánkénti találatszám, tanulási és kvízlisták).
     *
     * A szűrés **az adatbázisban** fut, az előre normalizált `technique.search_romaji`,
     * `technique.search_hungarian` és `technique_alias.aliasNormalized` oszlopokon
     * `LIKE '%…%'` feltétellel (ADR-012) – a keresőszöveg joker karakterei
     * (`%`, `_`) literálként illeszkednek, nem jokerként.
     *
     * @param query lásd [observeTechniques]; üres érték esetén minden találat
     *   [TechniqueMatch.matchedAlias] értéke `null`.
     * @param categories lásd [observeTechniques]; **üres halmaz = minden kategória**.
     * @return a találatok a `romajiAscii` szerint rendezve; visszavont technika nincs
     *   közöttük.
     */
    fun observeTechniqueMatches(
        query: String = "",
        categories: Set<CategoryId> = emptySet(),
    ): Flow<List<TechniqueMatch>>

    /**
     * Egy technika az azonosítója alapján, az aliasaival együtt – vagy `null`, ha ilyen
     * azonosító nincs a tartalomtáblákban.
     *
     * **A visszavont technikát is visszaadja** (lásd az osztály leírását): a `null` azt
     * jelenti, hogy a tudástár soha nem is tartalmazta ezt az azonosítót, nem azt, hogy
     * kikerült a tananyagból. A kettőt a [Technique.retired] zászló különíti el.
     */
    suspend fun getTechnique(id: TechniqueId): Technique?

    /**
     * A kvízgenerátor bemenete: technikák + szinonimacsoportok + illusztráció-ujjlenyomatok
     * (FR-11, FR-13, CR-07).
     *
     * A készlet **nem** dönt kvízképességről – azt kérdéstípusonként a jogosultsági kapu
     * mondja meg a bizalmi szint ismeretében (`QuizEligibility`, ADR-010). Ez a metódus
     * annyit tesz, hogy a motor számára összeszedi a tartalmat, és közben alkalmazza azt
     * az egyetlen szűrést, ami a tartalom életciklusából következik: a visszavont
     * technikák kimaradnak.
     *
     * @param categories a szóba jöhető kategóriák; **üres halmaz = minden kategória**.
     * @return a jelöltkészlet. A szinonimacsoportok a szűrés után is érvényesek: a
     *   kiszűrt technikákra hivatkozó tagságok elmaradnak, és a kétnél kevesebb tagúra
     *   fogyott csoportok kiesnek (a V006 csoport legalább kételemű).
     */
    suspend fun getQuizCandidates(categories: Set<CategoryId> = emptySet()): QuizCandidateSet

    /**
     * A betöltött tartalom állapota, megfigyelhetően (FR-32, FUNCTIONAL_SPEC 6.3).
     *
     * A `Flow` soha nem ad `null`-t: a „még nem futott import" az első indítás rendes
     * állapota, és [ContentState.NotImported] alakban jelenik meg.
     */
    fun observeContentState(): Flow<ContentState>
}
