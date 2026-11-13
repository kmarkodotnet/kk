package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * A **valódi** tudástár (`knowledgebase_transfer/`) beolvasása a tesztekhez – egy helyen.
 *
 * Miért közös helyen? Mert több teszt futtat a valódi tartalmon: a validátor TR-01
 * készlete és a jelöltszűrés TR-03 készlete is. Ha a leképezés két példányban élne, az
 * egyik csendben elavulna, és a két teszt már nem ugyanarról a tartalomról beszélne.
 *
 * A JSON → [KnowledgeInput] leképezés **teszt-oldali** és szándékosan minimális: a
 * tartalomimport igazi helye a `core/data` (T-115). Ez csak a validátor bemenetét állítja
 * elő a kanonikus séma szerint (ADR-011).
 */
internal object RealKnowledgeBase {
    /** `knowledgebase_transfer/README.md`: 161 rekord, 9 kategória, 10 illusztráció. */
    const val EXPECTED_ENTRY_COUNT = 161
    const val EXPECTED_ILLUSTRATION_COUNT = 10

    /** A 10 illusztrációból ennyi kvízben használható (3 feliratos kép kiesik – ADR-011). */
    const val EXPECTED_QUIZ_USABLE_ILLUSTRATION_COUNT = 7

    /** A kvízképes képek mindegyike ebben a kategóriában van. */
    const val ILLUSTRATED_CATEGORY_ID = "stances"

    private val JSON = Json { ignoreUnknownKeys = true }

    fun load(): KnowledgeInput {
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
        // Hiányzó tartalom nem „üres tudástár", hanem elbukó teszt: a garancia éppen az,
        // hogy a szabályok a *valódi* tartalmon futnak le.
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
}
