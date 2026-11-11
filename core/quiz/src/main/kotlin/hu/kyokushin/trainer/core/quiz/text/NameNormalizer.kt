package hu.kyokushin.trainer.core.quiz.text

import java.text.Normalizer

/**
 * A technikanevek **egyetlen** normalizálási szabálya (FR-03, V006, V009).
 *
 * Miért külön, publikus objektum, és nem a validátor privát segédfüggvénye?
 * Három, egymástól távoli helyen kell *bitre ugyanaz* az alak:
 *
 * 1. a validátor V006 szinonimaütközés-vizsgálata ([hu.kyokushin.trainer.core.quiz.validation]),
 * 2. a [hu.kyokushin.trainer.core.model.Technique.romajiAscii] származtatott mező, amit a
 *    validátor állít elő (V009),
 * 3. a tudástár szabad szöveges keresése (FR-03, T-202, `core/data`).
 *
 * Ha ez a szabály két helyen élne, a keresés megtalálná azt, amit a szinonimakizárás nem
 * tart azonosnak – vagyis a CR-07 garancia (azonos jelentésű kifejezés nem lehet egymás
 * distraktora) csendben kilyukadna. A `core/data` a `core/quiz`-re függ (ADR-001 L2 csak a
 * fordított irányt tiltja), ezért a keresés ugyanezt a függvényt hívhatja.
 *
 * Tiszta, Android-mentes és determinisztikus: nincs locale-érzékeny művelet (a kisbetűsítés
 * explicit [java.util.Locale.ROOT]), nincs I/O, nincs óra és nincs RNG (NFR-07, ADR-001).
 */
object NameNormalizer {

    /**
     * Összehasonlításra és keresésre alkalmas alakot készít: trimmel, kisbetűsít,
     * eltünteti a diakritikus jeleket, és az egybefüggő whitespace-t egyetlen szóközre húzza.
     *
     * A diakritikus jelek eltüntetése Unicode NFD dekompozícióval történik, nem
     * karaktertáblával: így a magyar `ő`/`ű` (kettős hosszú ékezet) és a romaji `ō`/`ū`
     * (makron) ugyanúgy kezelődik, és új nyelv felvétele nem kíván kódmódosítást.
     *
     * A kötőjelet és az írásjeleket **megtartja**: a `mae-geri` és a `mae geri` nem
     * ugyanaz a leírt alak, viszont a whitespace-összehúzás miatt a `mae  geri`
     * már igen. Ennél agresszívabb egyszerűsítés valódi, különböző technikákat vonna
     * össze egy szinonimacsoportba.
     *
     * @param raw a nyers név a tartalomból; `null` és blank is megengedett.
     * @return a normalizált alak, vagy üres string, ha a bemenet `null`/blank.
     */
    fun normalize(raw: String?): String {
        if (raw == null) return ""
        val collapsed = raw.trim().replace(WHITESPACE_RUN, " ")
        if (collapsed.isEmpty()) return ""
        val decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
        return decomposed.replace(COMBINING_MARKS, "").lowercase(java.util.Locale.ROOT)
    }

    /**
     * A diakritikus jelek nélküli, de a **szóalakot és a kis-/nagybetűt megtartó**
     * átírás – ez kerül a [hu.kyokushin.trainer.core.model.Technique.romajiAscii] mezőbe.
     *
     * Külön függvény a [normalize]-tól, mert a `romajiAscii` megjelenítésre is alkalmas
     * érték (a kanonikus sémában `japanese_romaji_ascii`, pl. `Mae Geri`), míg a [normalize]
     * kimenete tisztán összehasonlítási kulcs. A kettő keverése vagy csúnya, kisbetűs
     * neveket, vagy kis-/nagybetűre érzékeny keresést adna.
     */
    fun toAscii(raw: String?): String {
        if (raw == null) return ""
        val collapsed = raw.trim().replace(WHITESPACE_RUN, " ")
        if (collapsed.isEmpty()) return ""
        return Normalizer.normalize(collapsed, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
    }

    /** Két név akkor ütközik, ha a normalizált alakjuk azonos és nem üres (V006). */
    fun isSameName(first: String?, second: String?): Boolean {
        val a = normalize(first)
        return a.isNotEmpty() && a == normalize(second)
    }

    private val WHITESPACE_RUN = Regex("\\s+")

    /** Unicode kombináló diakritikus jelek – az NFD dekompozíció után ezek maradnak hátra. */
    private val COMBINING_MARKS = Regex("\\p{Mn}+")
}
