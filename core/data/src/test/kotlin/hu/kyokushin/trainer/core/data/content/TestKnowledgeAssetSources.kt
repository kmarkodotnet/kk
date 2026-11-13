package hu.kyokushin.trainer.core.data.content

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Memóriában tartott tudástár a beolvasó tesztjeihez.
 *
 * Azért van rá szükség, mert a [KnowledgeContentReader] szerződése a *tartalomról* szól,
 * nem az Android assetkezeléséről: így a sémaleképezés és a `V010` viselkedés tiszta JVM
 * unit tesztben ellenőrizhető, emulátor és csomagolt APK nélkül.
 *
 * @property files útvonal → fájltartalom. Ami nincs benne, arra a valódi assetkezelőhöz
 *   hasonlóan [IOException] jön.
 * @property illustrationFiles a „csomagolt" képfájlok csupasz nevei.
 */
internal class FakeKnowledgeAssetSource(
    private val files: Map<String, String>,
    private val illustrationFiles: Set<String> = emptySet(),
) : KnowledgeAssetSource {
    override fun readText(relativePath: String): String =
        files[relativePath] ?: throw FileNotFoundException(relativePath)

    override fun listFileNames(relativeDirectory: String): Set<String> =
        if (relativeDirectory == KnowledgeAssetLayout.ILLUSTRATIONS_DIRECTORY) illustrationFiles else emptySet()
}

/**
 * A **valódi** átadott tudástár (`knowledgebase_transfer/`) mint tudástárforrás.
 *
 * Az útvonal a `kyo.repoRoot` rendszertulajdonságból jön (a `core/data/build.gradle.kts`
 * állítja be), nem a teszt munkakönyvtárából: így a teszt akkor is megtalálja a tartalmat,
 * ha a Gradle más könyvtárból indítja a JVM-et.
 *
 * A csomagolt assets (`app/src/main/assets/knowledge/`) T-601 óta ugyanennek a mappának a
 * bitre azonos másolata (lásd `app` `hu.kyokushin.trainer.di.KnowledgeAssetImportTest`, amely
 * a valódi `AssetManager`-en át, a teljes `ContentImporter`-rel ellenőrzi) – ez a `core/data`
 * oldali osztály a beolvasó leképezését továbbra is ezen, fájlrendszeri elérésű forráson
 * teszteli, hogy a modul ne függjön Android assetkezeléstől.
 */
internal class RealContentAssetSource : KnowledgeAssetSource {
    override fun readText(relativePath: String): String = contentFile(relativePath).readText(Charsets.UTF_8)

    override fun listFileNames(relativeDirectory: String): Set<String> =
        (contentFile(relativeDirectory).list() ?: emptyArray()).toSet()

    private fun contentFile(name: String): File {
        val repoRoot = requireNotNull(System.getProperty("kyo.repoRoot")) {
            "A kyo.repoRoot rendszertulajdonság nincs beállítva – lásd core/data/build.gradle.kts."
        }
        val file = File(File(repoRoot, "knowledgebase_transfer"), name)
        // Hiányzó tartalom nem „üres tudástár", hanem elbukó teszt: a garancia éppen az,
        // hogy a leképezés a *valódi* 161 rekordon fut le.
        assertThat(file.exists()).isTrue()
        return file
    }
}

/**
 * Csomagolt tudástár-kiadások összeállítása a tesztekhez.
 *
 * Azért külön, közös helyen, mert a TR-05 garanciát (FR-33: a frissítés nem viszi el az
 * előrehaladást) **több** tesztosztály méri: ugyanazt a tartalmat kell két kiadásban,
 * csak az eltérésben különbözően előállítani. Ha a JSON-összeállítás tesztosztályonként
 * duplikálódna, a „mi változott a két kiadás között" kérdés lenne olvashatatlan – pont az,
 * amiről ezek a tesztek szólnak.
 *
 * A tartalom szándékosan **hiteles** Kyokushin terminológia (`kyokushin-content`): a
 * rekordok `review_status` szerint ellenőrzésre várnak, a hitelesítési szintet a teszt
 * sem emeli.
 */
internal object KnowledgeSourceFixtures {
    /** Az alap kiadás technikái: három valódi állás, a `stances` kategóriában. */
    val defaultEntries: List<String> = listOf(
        entry(id = "KY-0001"),
        entry(
            id = "KY-0002",
            romaji = "Kokutsu dachi",
            hungarian = "Hátul álló állás",
            aliases = emptyList(),
        ),
    )

    /**
     * Egy teljes, betölthető kiadás: manifest + rekordok + a hivatkozott képfájl.
     *
     * @param version a `dataset_version`; ez a verziókapu bemenete (FR-30).
     * @param schemaVersion a `schema_version`; nem támogatott major érték → V001.
     * @param entries a rekordok JSON töredékei, lásd [entry].
     */
    fun source(
        version: String,
        schemaVersion: String = DEFAULT_SCHEMA_VERSION,
        entries: List<String> = defaultEntries,
    ): FakeKnowledgeAssetSource = FakeKnowledgeAssetSource(
        files = mapOf(
            KnowledgeAssetLayout.MANIFEST_FILE to manifest(version, schemaVersion),
            KnowledgeAssetLayout.ENTRIES_FILE to
                """
                { "schema_version": "$schemaVersion", "dataset_version": "$version",
                  "entries": [ ${entries.joinToString(",")} ] }
                """.trimIndent(),
        ),
        illustrationFiles = setOf("KY-0044.jpg"),
    )

    fun manifest(version: String, schemaVersion: String = DEFAULT_SCHEMA_VERSION): String =
        """
        {
          "schema_version": "$schemaVersion",
          "dataset_version": "$version",
          "editorial_status": "draft_review_required",
          "categories": [ { "id": "stances", "name_hu": "Állások", "sort_order": 3 } ],
          "illustrations": [
            {
              "ref": "KY-0044",
              "file": "illustrations/KY-0044.jpg",
              "license": "project-internal",
              "quiz_usable": true,
              "hideable_on_study_card": true
            }
          ]
        }
        """.trimIndent()

    /**
     * Egy rekord JSON töredéke.
     *
     * A `null` érték a kötelező mezőkön szándékos: így állítható elő rekordszintű V002
     * elvetés anélkül, hogy a teszt kézzel írt, hibás JSON-t tartalmazna.
     */
    fun entry(
        id: String,
        romaji: String? = "Zenkutsu dachi",
        hungarian: String? = "Elöl álló állás",
        aliases: List<String> = listOf("Zenkucu dachi"),
        retired: Boolean = false,
    ): String =
        """
        {
          "id": "$id",
          "category_id": "stances",
          "japanese_romaji": ${romaji.jsonOrNull()},
          "hungarian_name": ${hungarian.jsonOrNull()},
          "aliases": [ ${aliases.joinToString(",") { "\"$it\"" }} ],
          "illustration_ref": "KY-0044",
          "review_status": "editorial_review_required",
          "quiz_excluded": false,
          "retired": $retired
        }
        """.trimIndent()

    private fun String?.jsonOrNull(): String = if (this == null) "null" else "\"$this\""

    private const val DEFAULT_SCHEMA_VERSION: String = "1.0.0"
}
