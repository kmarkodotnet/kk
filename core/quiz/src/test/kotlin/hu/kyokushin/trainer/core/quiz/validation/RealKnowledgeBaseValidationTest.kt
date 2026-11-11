package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.VerificationStatus
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import org.junit.Test

/**
 * A validátor a **valódi tartalmon** (TR-01, DR-05, FR-31).
 *
 * Miért kell ez a teszt a szabálytáblázat mellé? Mert a táblázat szintetikus: minden
 * bemenetét a teszt írta, és pontosan azt a hibát tartalmazza, amit a teszt keres. Egy
 * ilyen készlet zöld tud lenni akkor is, ha a validátor a tényleges tartalom egyetlen
 * alakját sem tudja feldolgozni – például mert a kanonikus séma `snake_case` mezőneveit
 * félreolvassuk, vagy mert a 161 rekord között olyan név van, amin a normalizálás elhasal.
 * Ez a teszt a `knowledgebase_transfer/` tényleges 161 rekordját futtatja át, és **0
 * BLOKKOLÓ hibát** követel meg.
 *
 * ## Amit ez a teszt nem helyettesít
 *
 * A JSON → [KnowledgeInput] leképezés itt **teszt-oldali**, és szándékosan minimális: a
 * tartalomimport igazi helye a `core/data` (T-115), ez a leképezés csak a
 * validátor bemenetét állítja elő a kanonikus séma szerint (ADR-011). Ha T-115 elkészül,
 * ez a teszt marad a validátor-oldali garancia, és a két leképezés eltérése maga is hiba.
 *
 * A `V010` (olvashatatlan JSON) itt sem keletkezhet: azt a kódot az importréteg állítja
 * elő, nem a validátor – ha a `Json.parseToJsonElement` elbukna, ez a teszt a parse-nál
 * dőlne el, és éppen ezt jelzi.
 */
class RealKnowledgeBaseValidationTest {

    private val validator = DefaultKnowledgeValidator()

    @Test
    fun validate_realKnowledgeBase_hasNoBlockingIssue_TR01() {
        val report = validator.validate(loadRealKnowledgeBase())

        val blocking = report.issues.filter { it.severity == Severity.BLOCKING }
        // A hibalista a hibaüzenet része: ha elbukik, látni kell, melyik rekordon.
        assertThat(blocking.map { "${it.code}/${it.entryId ?: "-"}/${it.field ?: "-"}" }).isEmpty()
        assertThat(report.datasetRejected).isFalse()
        assertThat(report.rejected).isEmpty()
    }

    @Test
    fun validate_realKnowledgeBase_acceptsEveryRecord_TR01() {
        val input = loadRealKnowledgeBase()

        val report = validator.validate(input)

        // A pontos szám szándékosan rögzített: ha a tartalom változik, ez a teszt
        // kikényszeríti, hogy a változást valaki tudomásul vegye (FR-32).
        assertThat(input.entries).hasSize(EXPECTED_ENTRY_COUNT)
        assertThat(report.accepted).hasSize(EXPECTED_ENTRY_COUNT)
        assertThat(report.schemaVersion).isEqualTo("1.0.0")
        assertThat(report.datasetVersion).isEqualTo("0.1.0")
        // `editorial_status = draft_review_required` → a szigorúbb olvasat (CR-02, ADR-010).
        assertThat(report.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
        // Egyetlen rekord sincs `review_status = reviewed`-en, tehát egyik sem hiteles.
        assertThat(report.accepted.map { it.status }.toSet())
            .containsExactly(VerificationStatus.AWAITING_REVIEW)
        assertThat(report.accepted.map { it.id.value }.toSet()).hasSize(EXPECTED_ENTRY_COUNT)
    }

    @Test
    fun validate_realKnowledgeBase_resolvesEveryIllustrationToAnAsset_TR01() {
        val report = validator.validate(loadRealKnowledgeBase())

        val assets = report.accepted.mapNotNull { it.illustration as? IllustrationRef.Asset }
        assertThat(assets).hasSize(EXPECTED_ILLUSTRATION_COUNT)
        assertThat(report.accepted.none { it.illustration == IllustrationRef.Placeholder }).isTrue()
        // A három feliratos kép (KY-0045, KY-0050, KY-0052) kvízben nem használható (ADR-011).
        assertThat(assets.filterNot { it.quizUsable }.map { it.ref })
            .containsExactly("KY-0045", "KY-0050", "KY-0052")
    }

    @Test
    fun validate_realKnowledgeBase_findsNoSynonymCollision_TR01_CR07() {
        val report = validator.validate(loadRealKnowledgeBase())

        // A konverter ugyanerre jutott: a manifest `synonym_groups` listája is üres.
        assertThat(report.issuesOf(IssueCode.V006)).isEmpty()
        assertThat(report.synonymGroups).isEmpty()
    }

    // ----------------------------------------------------------------------------
    // A kanonikus séma beolvasása (ADR-011)
    // ----------------------------------------------------------------------------

    private fun loadRealKnowledgeBase(): KnowledgeInput {
        val manifest = readJsonObject(contentFile("manifest.json"))
        val entries = readJsonObject(contentFile("entries.json"))

        return KnowledgeInput(
            manifest = ManifestInput(
                schemaVersion = manifest.string("schema_version"),
                contentVersion = manifest.string("dataset_version"),
                // A bizalmi szint a szerkesztői állapotból jön, nem a `dataset_trust`
                // önbevallásából: a tartalom nem emelheti a saját szintjét (CR-02).
                datasetKind = manifest.string("editorial_status"),
                categories = manifest.array("categories").map { element ->
                    val category = element.jsonObject
                    CategoryInput(
                        id = category.string("id"),
                        nameHu = category.string("name_hu"),
                        sortOrder = category.int("sort_order"),
                    )
                },
                illustrations = manifest.array("illustrations").map { element ->
                    val illustration = element.jsonObject
                    IllustrationInput(
                        ref = illustration.string("ref"),
                        file = illustration.string("file"),
                        license = illustration.string("license"),
                        source = illustration.string("source"),
                        author = illustration.string("author"),
                        quizUsable = illustration.boolean("quiz_usable"),
                        hideableOnStudyCard = illustration.boolean("hideable_on_study_card"),
                    )
                },
            ),
            entries = entries.array("entries").map { element ->
                val entry = element.jsonObject
                TechniqueInput(
                    id = entry.string("id"),
                    categoryId = entry.string("category_id"),
                    romaji = entry.string("japanese_romaji"),
                    hungarian = entry.string("hungarian_name"),
                    japaneseScript = entry.string("japanese_script"),
                    aliases = entry.array("aliases").map { (it as? JsonPrimitive)?.contentOrNullSafe() },
                    illustration = entry.string("illustration_ref"),
                    descriptionHu = entry.string("description_hu"),
                    // ADR-011: a `verified` mező nincs a fájlban, a `review_status`
                    // helyettesíti, és csak a `reviewed` érték jelent hitelesítést.
                    verified = entry.string("review_status") == "reviewed",
                    // A `quiz_eligible` szándékosan nincs a fájlban: számított érték (ADR-010).
                    quizEligible = null,
                    quizExcluded = entry.boolean("quiz_excluded"),
                    retired = entry.boolean("retired"),
                )
            },
            availableIllustrationFiles = (contentFile("illustrations").list() ?: emptyArray()).toSet(),
        )
    }

    /**
     * Egy fájl a tudástár átadott csomagjában.
     *
     * Az útvonal a `kyo.repoRoot` rendszertulajdonságból jön (a `core/quiz/build.gradle.kts`
     * állítja be), nem a teszt munkakönyvtárából: így a teszt akkor is megtalálja a
     * tartalmat, ha a Gradle más könyvtárból indítja a JVM-et.
     */
    private fun contentFile(name: String): File {
        val repoRoot = requireNotNull(System.getProperty("kyo.repoRoot")) {
            "A kyo.repoRoot rendszertulajdonság nincs beállítva – lásd core/quiz/build.gradle.kts."
        }
        val file = File(File(repoRoot, "knowledgebase_transfer"), name)
        // Hiányzó tartalom nem „üres tudástár", hanem elbukó teszt: a TR-01 garancia
        // éppen az, hogy a validátor a *valódi* tartalmon fut le.
        assertThat(file.exists()).isTrue()
        return file
    }

    private fun readJsonObject(file: File): JsonObject =
        JSON.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject

    private fun JsonObject.value(key: String): JsonElement? = this[key]?.takeIf { it !is JsonNull }

    private fun JsonObject.string(key: String): String? = (value(key) as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? = (value(key) as? JsonPrimitive)?.int

    private fun JsonObject.boolean(key: String): Boolean? = (value(key) as? JsonPrimitive)?.boolean

    private fun JsonObject.array(key: String): JsonArray = (value(key) as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonPrimitive.contentOrNullSafe(): String? = if (this is JsonNull) null else content

    private companion object {
        /** `knowledgebase_transfer/README.md`: 161 rekord, 9 kategória, 10 illusztráció. */
        const val EXPECTED_ENTRY_COUNT = 161
        const val EXPECTED_ILLUSTRATION_COUNT = 10

        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
