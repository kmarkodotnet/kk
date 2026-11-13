package hu.kyokushin.trainer.core.data.content

import hu.kyokushin.trainer.core.quiz.validation.ValidationReport

/**
 * A csomagolt tudástár betöltése a Room tartalomtáblákba (FR-29, FR-31, FR-33,
 * ARCHITECTURE 5.3).
 *
 * Ez a `core/data` publikus szerződése az importra; a megvalósítás `internal`
 * (ADR-001 L4). A hívó (az app indulása, illetve a fejlesztői diagnosztika) nem tud és
 * nem is kell tudnia a DTO-król, a DAO-król és a Room tranzakcióról.
 *
 * ## A három garancia, amit a szerződés hordoz
 *
 * 1. **Nem omlik össze érvénytelen tartalomtól** (FR-31): a hiba [ImportResult.Rejected],
 *    nem kivétel. Elutasított adathalmaz esetén a **korábban betöltött tartalom érvényben
 *    marad** – az app használható, és a diagnosztika megmondja, mi a baj.
 * 2. **Nem veszít felhasználói adatot** (FR-33, TR-05): a betöltés upsert, és a csomagból
 *    kimaradt technika `retired = true` jelölést kap, nem törlést (ADR-004). A
 *    `quiz_attempt`, `study_review` és `quiz_session` sorokhoz az import soha nem nyúl.
 * 3. **Idempotens** (FR-30): ugyanaz a `dataset_version` kétszer betöltve nem duplikál és
 *    nem is fut le kétszer – a második hívás [ImportResult.UpToDate].
 *
 * Minden függvény `suspend`: a betöltés háttérszálon fut (injektált `@IoDispatcher`), így a
 * hívó UI a futás idejére `Loading` állapotot tud mutatni (NFR-01).
 */
interface ContentImporter {
    /**
     * Betölti a csomagolt tudástárat, **ha** a csomagolt `dataset_version` eltér a
     * tároltól.
     *
     * A verzióösszevetéshez a manifestet mindig be kell olvasni; a validáció és az
     * adatbázisírás viszont csak tényleges eltérésnél fut le.
     *
     * @return mi történt – mindhárom ág rendes működés, nem hibajelzés.
     */
    suspend fun importIfNeeded(): ImportResult

    /**
     * Az **ebben a processzben** lefutott utolsó betöltés teljes validációs jelentése, a
     * fejlesztői diagnosztikához (FR-32, T-508).
     *
     * `null`, ha ebben a processzben nem futott validáció – tipikusan azért, mert a
     * tartalomverzió nem változott ([ImportResult.UpToDate]), és a betöltés kimaradt.
     * A részletes észrevételek szándékosan **nem** perzisztálódnak: a `content_state`
     * tábla csak összegzést tárol, a jelentés pedig a futó folyamat diagnosztikája.
     */
    suspend fun lastValidationReport(): ValidationReport?
}

/**
 * A tartalombetöltés kimenete.
 *
 * Három ág, mert a hívónak három *különböző* dolgot kell tennie: semmit, új tartalmat
 * megjeleníteni, vagy diagnosztikát adni a korábbi tartalom mellett. Egyetlen,
 * `success: Boolean` mezős típus ezt a különbséget elrejtené.
 */
sealed interface ImportResult {
    /**
     * A tárolt tartalomverzió megegyezik a csomagolttal: nem futott se validáció, se
     * adatbázisírás (FR-30 idempotencia).
     *
     * @property contentVersion a közös verzió.
     */
    data class UpToDate(val contentVersion: String) : ImportResult

    /**
     * A tartalom betöltődött.
     *
     * @property contentVersion a betöltött `dataset_version`, vagy `null`, ha a manifestből
     *   hiányzott – ilyenkor egyetlen rekord sem fogadható el (a `datasetVersion` kötelező
     *   domain mező), de ez rekordszintű V002, nem adathalmaz-elutasítás.
     * @property acceptedCount a felvett vagy frissített technikák száma.
     * @property rejectedCount az elvetett rekordok száma; ezek nem kerülnek a tartalomba.
     * @property retiredCount ennyi, korábban tárolt technika kapott `retired = true`
     *   jelölést, mert az aktuális csomag nem tartalmazza elfogadott rekordként (ADR-004).
     *   A hozzájuk tartozó felhasználói sorok érintetlenek.
     */
    data class Imported(
        val contentVersion: String?,
        val acceptedCount: Int,
        val rejectedCount: Int,
        val retiredCount: Int,
    ) : ImportResult

    /**
     * A teljes adathalmaz elutasítva (V001 nem támogatott séma, V010 értelmezhetetlen
     * JSON): a tartalomtáblák **változatlanok**, az app a korábban betöltött tartalommal
     * működik tovább (FR-31).
     *
     * @property report a kész jelentés a diagnosztikához; ugyanaz, amit a
     *   [ContentImporter.lastValidationReport] ad vissza.
     */
    data class Rejected(val report: ValidationReport) : ImportResult
}
