package hu.kyokushin.trainer.core.model

/**
 * Egy technika illusztrációjára mutató hivatkozás (ADR-003).
 *
 * Miért sealed típus, és nem `String?`?
 * Három, egymástól élesen eltérő eset van, és a kép-alapú kvíztípusok (C, D) csak az
 * egyikben aktiválhatók:
 *
 * | Érték | Jelentés |
 * |---|---|
 * | `null` (a [Technique.illustration] mezőben) | a rekord nem hivatkozik illusztrációra |
 * | [Placeholder] | hivatkozik, de nincs valódi kép (szándékos helyőrző, vagy a fájl hiányzik – V005) |
 * | [Asset] | validált, létező képfájl |
 *
 * Nullable stringnél a kvízjogosultság rejtett szöveg-összehasonlítás lenne
 * (`!= "placeholder"`), külön `hasRealImage: Boolean` flagnél pedig két mező csúszhatna
 * ki egymásból. Sealed típusnál a fordító kényszeríti ki a kimerítő `when`-t, és a
 * jogosultság **egyetlen típusvizsgálat** (CR-05, FR-13).
 */
sealed interface IllustrationRef {

    /**
     * Validált, a csomagban tényleg létező képfájl.
     *
     * @property ref a `manifest.json` → `illustrations` nyilvántartásában szereplő
     *   licenckulcs. Nem fájlnév: a licencadatokat ez kapcsolja a képhez.
     * @property fileName a tényleges fájl neve az `assets/knowledge/illustrations/`
     *   könyvtárban.
     * @property quizUsable a kép használható-e kvízkérdésben. Külön kapu a hitelesítési
     *   státusztól: egy szakmailag hiteles, de **feliratos** (a technika nevét kiíró)
     *   kép hiteles tananyag, kvízben viszont elárulja a megoldást, ezért sem helyes
     *   válaszként, sem distraktorként nem használható (ADR-011 kiegészítés, ADR-010).
     * @property hideableOnStudyCard a kép megjelenhet-e azonnal a tanulókártyán.
     *   `false` esetén a kártya csak **felfedés után** mutatja meg – ugyanaz a feliratos
     *   kép a tanuláshoz hasznos, de előre megmutatva elveszi az önellenőrzés értelmét.
     */
    data class Asset(
        val ref: String,
        val fileName: String,
        val quizUsable: Boolean,
        val hideableOnStudyCard: Boolean,
    ) : IllustrationRef {
        init {
            require(ref.isNotBlank()) { "IllustrationRef.Asset.ref nem lehet üres." }
            require(fileName.isNotBlank()) { "IllustrationRef.Asset.fileName nem lehet üres." }
        }
    }

    /**
     * A rekord hivatkozik illusztrációra, de valódi kép nincs mögötte.
     *
     * A V005 validációs szabály nem elveti az ilyen rekordot, hanem **lefokozza**
     * [Asset] → [Placeholder] átalakítással: a technika a tudástárban látható marad,
     * csak a kép-alapú kvíztípusokból esik ki. Állapotot nem hordoz, ezért `data object`.
     */
    data object Placeholder : IllustrationRef
}
