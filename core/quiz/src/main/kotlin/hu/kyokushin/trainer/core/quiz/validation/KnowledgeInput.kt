package hu.kyokushin.trainer.core.quiz.validation

/**
 * A validátor teljes bemenete (DR-05, FR-31).
 *
 * **A validátor nem ismeri a fájlrendszert.** A csomagolt képfájlok tényleges
 * jelenléte [availableIllustrationFiles] formájában *bemenet*, nem mellékhatás: így a
 * V005 szabály tisztán JVM-en, emulátor és assets nélkül tesztelhető (TR-01).
 *
 * **Minden mező nyers, a JSON-ból érkező alakban.** A bemeneti típusok szándékosan
 * `null`-tűrők és nem validálnak semmit a konstruktorukban: a hibás tartalomnak
 * *reprezentálhatónak* kell lennie, különben a hiba már a bemenet összeállításánál
 * kivételt dobna, és nem kerülhetne be a [ValidationReport]-ba. A domain modellek
 * ([hu.kyokushin.trainer.core.model.Technique]) ezzel szemben `require()`-rel
 * validálnak – ezért a validátor csak a már elfogadott rekordokból készít domain
 * objektumot, soha nem előbb.
 *
 * @property manifest a `manifest.json` nyers tartalma.
 * @property entries a `techniques.json` rekordjai, a fájlban szereplő sorrendben. A
 *   sorrend jelentéshordozó: a V003 (duplikált `id`) szabály a *későbbi* rekordot veti el.
 * @property availableIllustrationFiles az `assets/knowledge/illustrations/` könyvtárban
 *   tényleg megtalálható fájlnevek halmaza. Üres halmaz azt jelenti: nincs csomagolt kép,
 *   nem azt, hogy a vizsgálat kimarad.
 */
data class KnowledgeInput(
    val manifest: ManifestInput,
    val entries: List<TechniqueInput>,
    val availableIllustrationFiles: Set<String> = emptySet(),
)

/**
 * A `manifest.json` nyers alakja.
 *
 * @property schemaVersion a séma szerkezeti verziója **semver stringként**, a kanonikus
 *   sémában szereplő nyers alakban (pl. `1.0.0`). ADR-011 szerint a `schema_version`
 *   string, nem egész szám, és a támogatottság a **major** verzión dől el; `null`, ha
 *   hiányzik – ez önmagában V001 (BLOKKOLÓ, a teljes betöltés elutasítva).
 * @property contentVersion a tartalomkiadás semver-szerű verziója (pl. `0.1.0-demo`).
 * @property datasetKind a tudástár szerkesztői állapota a tartalomban – a kanonikus
 *   sémában az `editorial_status` / `dataset_trust` érték (pl. `reviewed`,
 *   `draft_review_required`). Nyers `String`, mert az ismeretlen értéket is jelenteni kell tudni;
 *   a [hu.kyokushin.trainer.core.model.DatasetTrust]-ra fordítás a validátor dolga, és
 *   bizalmi szintet soha nem *emel* (CR-02).
 * @property categories az egyetlen hiteles kategórialista (ADR-011). A V004 szabály
 *   ehhez hasonlítja a rekordok `categoryId`-ját.
 * @property illustrations a licencnyilvántartás. A V005 szabály ehhez *és*
 *   a [KnowledgeInput.availableIllustrationFiles]-hoz hasonlítja a képhivatkozást.
 */
data class ManifestInput(
    val schemaVersion: String? = null,
    val contentVersion: String? = null,
    val datasetKind: String? = null,
    val categories: List<CategoryInput> = emptyList(),
    val illustrations: List<IllustrationInput> = emptyList(),
)

/**
 * Egy kategória nyers alakja a manifestből.
 *
 * @property id a kategória stabil azonosítója.
 * @property nameHu a magyar megjelenítendő név – a tartalomból, nem a `strings.xml`-ből
 *   (ADR-011).
 * @property sortOrder a tartalom által diktált megjelenítési sorrend; `null`, ha hiányzik.
 */
data class CategoryInput(
    val id: String? = null,
    val nameHu: String? = null,
    val sortOrder: Int? = null,
    /**
     * A [nameHu] nyitott kulcsú fordítás-térképe (FR-43, ADR-013), nyers alakban – a
     * `null`/blank értékek és a `"hu"` kulcs szűrése a leképezés felelőssége, nem ennek
     * a bemeneti típusnak, ugyanazon elv szerint, mint a többi nyers mező.
     */
    val nameTranslations: Map<String, String?> = emptyMap(),
)

/**
 * Egy illusztráció-nyilvántartási bejegyzés nyers alakja a manifestből.
 *
 * @property ref a licenckulcs, amire a technikarekordok `illustration` mezője hivatkozik.
 * @property file a fájlnév az illusztrációkönyvtárban. `null` a szándékos helyőrzőnél –
 *   ilyenkor a rekord [hu.kyokushin.trainer.core.model.IllustrationRef.Placeholder]-t kap,
 *   és nem keletkezik V005 figyelmeztetés hiányzó fájlra.
 * @property license a licenc megnevezése. Hiányzó licenc tartalmi integritási hiba: a
 *   kép nem használható (CR-03).
 * @property source a kép forrása (URL vagy hivatkozás).
 * @property author a szerző.
 * @property quizUsable a kép használható-e kvízkérdésben. `null` esetén a validátor a
 *   szigorúbb (nem kvízképes) olvasatot veszi: feliratos kép elárulná a megoldást.
 * @property hideableOnStudyCard a kép csak felfedés után mutatható-e a tanulókártyán.
 */
data class IllustrationInput(
    val ref: String? = null,
    val file: String? = null,
    val license: String? = null,
    val source: String? = null,
    val author: String? = null,
    val quizUsable: Boolean? = null,
    val hideableOnStudyCard: Boolean? = null,
)

/**
 * Egy technikarekord nyers alakja a `techniques.json`-ból.
 *
 * A `romajiAscii` szándékosan **nincs** a bemenetben: a diakritikus jelek nélküli alak a
 * tartalomimport származtatott értéke, nem szerkesztett adat – a normalizálás szabálya
 * egyetlen helyen, a validátorban él, hogy a keresés és a V006 szinonimavizsgálat
 * ugyanazt az alakot lássa.
 *
 * A `quizEligible` a sémában szerepel, és a V007/V008 szabály miatt be kell olvasni, de a
 * domain modellbe nem kerül át: a kvízjogosultság származtatott érték (ADR-010).
 *
 * @property id stabil és örök azonosító (DR-06).
 * @property categoryId hivatkozás a manifest kategóriáira (V004).
 * @property romaji a technika eredeti neve latin átírásban – ez a hiteles forma.
 * @property hungarian a magyar jelentés. Kötelező mező (V002).
 * @property japaneseScript az eredeti japán írásmód, ha ismert.
 * @property aliases további elfogadott elnevezések. Az elemek `null`-tűrők, mert a V009
 *   szabálynak az üres és a `null` aliast is jelentenie kell tudni.
 * @property illustration a manifest `illustrations` nyilvántartásának `ref` kulcsa, vagy
 *   `null`, ha a rekord nem hivatkozik illusztrációra.
 * @property descriptionHu rövid magyar leírás.
 * @property difficulty nehézségi szint a tartalomból, ha van.
 * @property beltRank övfok a tartalomból, ha van. Kódból **soha** nem pótolható (CR-01).
 * @property verified a szerkesztői hitelesítés a tartalomban. Ebből és a hiányzó mezőkből
 *   származtatja a validátor a háromállapotú
 *   [hu.kyokushin.trainer.core.model.VerificationStatus]-t (ADR-002).
 * @property quizEligible a tartalom kérése; a V007/V008 kapu lefokozhatja.
 * @property quizExcluded szerkesztői döntés: hiteles rekord, de a kvízből kimarad.
 * @property retired a technika kikerült a tananyagból. Nem töröljük, csak jelöljük
 *   (ADR-004), hogy a korábbi próbálkozások hivatkozása értelmes maradjon (FR-33).
 * @property source a rekord forrása.
 */
data class TechniqueInput(
    val id: String? = null,
    val categoryId: String? = null,
    val romaji: String? = null,
    val hungarian: String? = null,
    val japaneseScript: String? = null,
    val aliases: List<String?> = emptyList(),
    val illustration: String? = null,
    val descriptionHu: String? = null,
    val difficulty: Int? = null,
    val beltRank: String? = null,
    val verified: Boolean? = null,
    val quizEligible: Boolean? = null,
    val quizExcluded: Boolean? = null,
    val retired: Boolean? = null,
    val source: String? = null,
    /** A [hungarian] fordítás-térképe, nyers alakban – lásd [CategoryInput.nameTranslations]. */
    val nameTranslations: Map<String, String?> = emptyMap(),
    /** A [descriptionHu] fordítás-térképe, nyers alakban – lásd [nameTranslations]. */
    val descriptionTranslations: Map<String, String?> = emptyMap(),
)
