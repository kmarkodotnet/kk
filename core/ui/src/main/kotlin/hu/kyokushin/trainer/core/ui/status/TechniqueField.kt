package hu.kyokushin.trainer.core.ui.status

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.ui.R

/**
 * Egy technikarekord megnevezhető mezői – a „Hiányos" jelölő felsorolásához
 * (FR-05, FUNCTIONAL_SPEC 2.2).
 *
 * **Miért enum, és miért nem `List<String>`?** A jelölőnek magyar mezőneveket kell
 * kiírnia. Ha a komponens kész szöveget kapna, a mezőneveket a hívó – tehát egy
 * ViewModel vagy egy funkciómodul – írná bele a kódba, és a magyar szöveg kiszökne a
 * `strings.xml`-ből (NFR-02). Így a hívó csak azt mondja meg, *melyik* mező hiányzik;
 * a megnevezés erőforrásból jön, és egyetlen helyen fordítható.
 *
 * A felsorolás a tartalomséma megjelenített mezőit követi (ADR-011). A validátor
 * által kötelezőként kezelt `id` és `datasetVersion` szándékosan nincs itt: az ilyen
 * rekord be sem kerül a tudástárba, tehát a UI soha nem jelölheti meg.
 *
 * @property labelRes a mező magyar megnevezése a felsorolásban.
 */
enum class TechniqueField(@StringRes val labelRes: Int) {

    /** A technika romaji neve. */
    ROMAJI(R.string.content_field_romaji),

    /** A magyar jelentés. */
    HUNGARIAN(R.string.content_field_hungarian),

    /** A kategória hozzárendelése. */
    CATEGORY(R.string.content_field_category),

    /**
     * Az illusztráció. A gyakorlatban ez a leggyakoribb hiány: a lógó képhivatkozás az,
     * ami egy betöltött rekordot `INCOMPLETE`-re fokoz le (V005, ADR-002).
     */
    ILLUSTRATION(R.string.content_field_illustration),

    /** Az eredeti japán írásmód. */
    JAPANESE_SCRIPT(R.string.content_field_japanese_script),

    /** A rövid magyar leírás. */
    DESCRIPTION(R.string.content_field_description),
}
