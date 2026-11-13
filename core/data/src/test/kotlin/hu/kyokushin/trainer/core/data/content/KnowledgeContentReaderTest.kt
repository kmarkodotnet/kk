package hu.kyokushin.trainer.core.data.content

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import hu.kyokushin.trainer.core.quiz.validation.IssueScope
import hu.kyokushin.trainer.core.quiz.validation.Severity
import org.junit.Test

/**
 * A kanonikus séma beolvasása szintetikus tartalmon (FR-29, DR-05, ADR-011).
 *
 * Négy garanciát rögzít, mert ez a négy az, ami a tartalom bővíthetőségét és a hibatűrést
 * eldönti:
 *
 * 1. **Ismeretlen extra mező nem hiba** – a tartalom bővíthető anélkül, hogy a már
 *    kiadott app elszállna (`ignoreUnknownKeys`).
 * 2. **Hiányzó kötelező mező nem parse-hiba** – a validátor jelenti rekordszinten (V002),
 *    nem a beolvasó adathalmaz-szinten; a többi rekord használható marad.
 * 3. **A `schema_version` semver string**, nem egész szám (ADR-011).
 * 4. **Olvashatatlan fájl → `V010`** – ezt a kódot az importréteg állítja elő, nem a
 *    validátor.
 */
class KnowledgeContentReaderTest {
    private val validator = DefaultKnowledgeValidator()

    @Test
    fun read_unknownExtraFields_areIgnoredAndDoNotFailTheLoad_FR29() {
        val source = FakeKnowledgeAssetSource(
            files = mapOf(
                KnowledgeAssetLayout.MANIFEST_FILE to MANIFEST_WITH_FUTURE_FIELDS,
                KnowledgeAssetLayout.ENTRIES_FILE to ENTRIES_WITH_FUTURE_FIELDS,
            ),
        )

        val result = source.read()

        // A jövőbeli mezők (`future_manifest_field`, `future_entry_field`, új kategória- és
        // illusztrációmező) átlépve; a séma ismert része sértetlen.
        assertThat(result.input.manifest.schemaVersion).isEqualTo("1.0.0")
        assertThat(result.input.manifest.contentVersion).isEqualTo("0.1.0")
        assertThat(result.input.manifest.categories.map { it.id }).containsExactly("stances")
        assertThat(result.input.entries.map { it.id }).containsExactly("KY-0001")
        assertThat(result.input.entries.single().romaji).isEqualTo("Zenkutsu dachi")
    }

    @Test
    fun read_schemaVersion_isSemverString_notInteger_FR29() {
        val result = canonicalSource().read()

        // ADR-011: a `schema_version` string. Ha Int-ként olvasnánk, a parse itt dőlne el.
        assertThat(result.input.manifest.schemaVersion).isEqualTo("1.0.0")
        assertThat(validator.validate(result.input).schemaVersion).isEqualTo("1.0.0")
        assertThat(validator.validate(result.input).datasetRejected).isFalse()
    }

    @Test
    fun read_mapsEveryCanonicalFieldToTheValidatorInput_FR29() {
        val result = canonicalSource().read()

        val manifest = result.input.manifest
        // `dataset_version` → contentVersion, `editorial_status` → datasetKind (CR-02).
        assertThat(manifest.contentVersion).isEqualTo("0.1.0")
        assertThat(manifest.datasetKind).isEqualTo("draft_review_required")
        assertThat(manifest.categories.single().nameHu).isEqualTo("Állások")
        assertThat(manifest.categories.single().sortOrder).isEqualTo(3)
        val illustration = manifest.illustrations.single()
        assertThat(illustration.ref).isEqualTo("KY-0044")
        assertThat(illustration.file).isEqualTo("illustrations/KY-0044.jpg")
        assertThat(illustration.license).isEqualTo("project-internal")
        assertThat(illustration.quizUsable).isTrue()
        assertThat(illustration.hideableOnStudyCard).isTrue()

        val entry = result.input.entries.single()
        assertThat(entry.id).isEqualTo("KY-0001")
        assertThat(entry.categoryId).isEqualTo("stances")
        assertThat(entry.romaji).isEqualTo("Zenkutsu dachi")
        assertThat(entry.hungarian).isEqualTo("Elöl álló állás")
        assertThat(entry.japaneseScript).isNull()
        assertThat(entry.illustration).isEqualTo("KY-0044")
        assertThat(entry.descriptionHu).isNotEmpty()
        assertThat(entry.quizExcluded).isFalse()
        assertThat(entry.retired).isFalse()
        // A `quiz_eligible` nincs a fájlban: számított érték (ADR-010).
        assertThat(entry.quizEligible).isNull()
        assertThat(result.input.availableIllustrationFiles).containsExactly("KY-0044.jpg")
    }

    @Test
    fun read_reviewStatus_onlyReviewedMeansVerified_CR01() {
        val reviewed = canonicalSource(reviewStatus = "reviewed").read()
        val awaiting = canonicalSource(reviewStatus = "editorial_review_required").read()
        val unknown = canonicalSource(reviewStatus = "valami_ismeretlen").read()

        assertThat(reviewed.input.entries.single().verified).isTrue()
        // Hitelesítési szintet kódból soha nem emelünk: minden más érték nem hiteles.
        assertThat(awaiting.input.entries.single().verified).isFalse()
        assertThat(unknown.input.entries.single().verified).isFalse()
    }

    @Test
    fun read_missingMandatoryField_isReportedByTheValidatorAsV002_notAsParseFailure_DR05() {
        val source = FakeKnowledgeAssetSource(
            files = mapOf(
                KnowledgeAssetLayout.MANIFEST_FILE to CANONICAL_MANIFEST,
                // A második rekordból hiányzik a `hungarian_name`.
                KnowledgeAssetLayout.ENTRIES_FILE to ENTRIES_WITH_A_MISSING_MANDATORY_FIELD,
            ),
        )

        val result = source.read()

        // 1. A beolvasás nem bukik el, és nem V010: a fájl szintaxisa rendben van.
        assertThat(result.input.entries).hasSize(2)
        // 2. A hibát a validátor jelenti, rekordszinten – a másik rekord megmarad.
        val report = validator.validate(result.input)
        assertThat(report.issuesOf(IssueCode.V010)).isEmpty()
        assertThat(report.datasetRejected).isFalse()
        assertThat(report.issuesOf(IssueCode.V002).map { it.entryId }).containsExactly("KY-0002")
        assertThat(report.rejected.map { it.entry.id }).containsExactly("KY-0002")
        assertThat(report.accepted.map { it.id.value }).containsExactly("KY-0001")
    }

    @Test
    fun read_malformedJson_producesV010AndRejectsTheDataset_DR05() {
        val source = FakeKnowledgeAssetSource(
            files = mapOf(
                KnowledgeAssetLayout.MANIFEST_FILE to CANONICAL_MANIFEST,
                KnowledgeAssetLayout.ENTRIES_FILE to """{ "entries": [ { "id": "KY-0001" """,
            ),
        )

        val result = source.readUnreadable()

        val issue = result.report.issues.single()
        assertThat(issue.code).isEqualTo(IssueCode.V010)
        assertThat(issue.severity).isEqualTo(Severity.BLOCKING)
        assertThat(issue.code.scope).isEqualTo(IssueScope.DATASET)
        // A diagnosztikának meg kell mondania, melyik fájl olvashatatlan.
        assertThat(issue.field).isEqualTo(KnowledgeAssetLayout.ENTRIES_FILE)
        assertThat(issue.detail).contains(KnowledgeAssetLayout.ENTRIES_FILE)
        assertThat(result.report.datasetRejected).isTrue()
        assertThat(result.report.accepted).isEmpty()
    }

    @Test
    fun read_structurallyWrongJson_producesV010_DR05() {
        // Szintaktikailag érvényes JSON, de a séma szerinti szerkezet sérül: az `entries`
        // objektum, nem tömb. Ez nem hiányzó mező, hanem értelmezhetetlen szerkezet.
        val source = FakeKnowledgeAssetSource(
            files = mapOf(
                KnowledgeAssetLayout.MANIFEST_FILE to CANONICAL_MANIFEST,
                KnowledgeAssetLayout.ENTRIES_FILE to """{ "entries": { "id": "KY-0001" } }""",
            ),
        )

        val result = source.readUnreadable()

        assertThat(result.report.issues.single().code).isEqualTo(IssueCode.V010)
        assertThat(result.report.datasetRejected).isTrue()
    }

    @Test
    fun read_missingContentFile_producesV010_DR05() {
        // Üres csomag: a `manifest.json` sincs meg. Nem crash, hanem elutasító jelentés (FR-31).
        val result = FakeKnowledgeAssetSource(files = emptyMap()).readUnreadable()

        assertThat(result.report.issues.single().code).isEqualTo(IssueCode.V010)
        assertThat(result.report.issues.single().field).isEqualTo(KnowledgeAssetLayout.MANIFEST_FILE)
        assertThat(result.report.datasetRejected).isTrue()
    }

    @Test
    fun read_missingIllustrationFile_isNotALoadFailureButAV005Warning_DR05() {
        // A manifest hivatkozik a képre, de a csomagban nincs fájl: a rekord megmarad,
        // csak a kép esik ki (V005, FIGYELMEZTETÉS).
        val result = canonicalSource(packagedIllustrations = emptySet()).read()

        assertThat(result.input.availableIllustrationFiles).isEmpty()
        val report = validator.validate(result.input)
        assertThat(report.datasetRejected).isFalse()
        assertThat(report.accepted).hasSize(1)
        assertThat(report.issuesOf(IssueCode.V005).map { it.severity }).containsExactly(Severity.WARNING)
    }

    @Test
    fun read_emptyEntriesList_isValidContentNotAParseFailure_DR05() {
        val source = FakeKnowledgeAssetSource(
            files = mapOf(
                KnowledgeAssetLayout.MANIFEST_FILE to CANONICAL_MANIFEST,
                KnowledgeAssetLayout.ENTRIES_FILE to """{ "schema_version": "1.0.0", "entries": [] }""",
            ),
        )

        val result = source.read()

        assertThat(result.input.entries).isEmpty()
        assertThat(validator.validate(result.input).datasetRejected).isFalse()
    }

    private fun KnowledgeAssetSource.read(): KnowledgeContentReadResult.Parsed {
        val result = KnowledgeContentReader(this).read()
        assertThat(result).isInstanceOf(KnowledgeContentReadResult.Parsed::class.java)
        return result as KnowledgeContentReadResult.Parsed
    }

    private fun KnowledgeAssetSource.readUnreadable(): KnowledgeContentReadResult.Unreadable {
        val result = KnowledgeContentReader(this).read()
        assertThat(result).isInstanceOf(KnowledgeContentReadResult.Unreadable::class.java)
        return result as KnowledgeContentReadResult.Unreadable
    }

    private fun canonicalSource(
        reviewStatus: String = "editorial_review_required",
        packagedIllustrations: Set<String> = setOf("KY-0044.jpg"),
    ) = FakeKnowledgeAssetSource(
        files = mapOf(
            KnowledgeAssetLayout.MANIFEST_FILE to CANONICAL_MANIFEST,
            KnowledgeAssetLayout.ENTRIES_FILE to canonicalEntries(reviewStatus),
        ),
        illustrationFiles = packagedIllustrations,
    )

    private companion object {
        /** A kanonikus manifest egy kategóriával és egy illusztrációval (ADR-011). */
        val CANONICAL_MANIFEST =
            """
            {
              "schema_version": "1.0.0",
              "dataset_version": "0.1.0",
              "generated_at": "2026-11-11",
              "language": "hu-HU",
              "martial_art": "Kyokushin karate",
              "editorial_status": "draft_review_required",
              "dataset_trust": "practice_on_unreviewed",
              "notes": ["A tartalom szakmai lektorálást igényel."],
              "categories": [ { "id": "stances", "name_hu": "Állások", "sort_order": 3 } ],
              "illustrations": [
                {
                  "ref": "KY-0044",
                  "file": "illustrations/KY-0044.jpg",
                  "content_type": "image/jpeg",
                  "width": 1122,
                  "height": 1402,
                  "sha256": "be0189a3",
                  "origin": "ai_generated",
                  "license": "project-internal",
                  "source": "knowledgebase/pics",
                  "author": null,
                  "review_status": "human_review_required",
                  "has_embedded_label": false,
                  "quiz_usable": true,
                  "hideable_on_study_card": true
                }
              ],
              "synonym_groups": []
            }
            """.trimIndent()

        fun canonicalEntries(reviewStatus: String) =
            """
            {
              "schema_version": "1.0.0",
              "dataset_version": "0.1.0",
              "entries": [
                {
                  "id": "KY-0001",
                  "category_id": "stances",
                  "japanese_romaji": "Zenkutsu dachi",
                  "japanese_romaji_ascii": "Zenkutsu dachi",
                  "japanese_script": null,
                  "hungarian_name": "Elöl álló állás",
                  "description_hu": "Hosszú, elöl súlyozott állás.",
                  "aliases": [],
                  "tags": [],
                  "illustration_ref": "KY-0044",
                  "review_status": "$reviewStatus",
                  "quiz_excluded": false,
                  "retired": false,
                  "search_romaji": "zenkutsu dachi",
                  "search_hungarian": "elol allo allas",
                  "incomplete": false
                }
              ]
            }
            """.trimIndent()

        /** Ugyanaz a manifest, kitalált jövőbeli mezőkkel minden szinten. */
        val MANIFEST_WITH_FUTURE_FIELDS =
            """
            {
              "schema_version": "1.0.0",
              "dataset_version": "0.1.0",
              "editorial_status": "draft_review_required",
              "future_manifest_field": { "nested": [1, 2, 3] },
              "categories": [
                { "id": "stances", "name_hu": "Állások", "sort_order": 3, "future_category_field": "x" }
              ],
              "illustrations": [
                {
                  "ref": "KY-0044",
                  "file": "illustrations/KY-0044.jpg",
                  "license": "project-internal",
                  "quiz_usable": true,
                  "hideable_on_study_card": true,
                  "future_illustration_field": 42
                }
              ],
              "synonym_groups": []
            }
            """.trimIndent()

        val ENTRIES_WITH_FUTURE_FIELDS =
            """
            {
              "schema_version": "1.0.0",
              "future_entries_field": "a konverter új kimenete",
              "entries": [
                {
                  "id": "KY-0001",
                  "category_id": "stances",
                  "japanese_romaji": "Zenkutsu dachi",
                  "hungarian_name": "Elöl álló állás",
                  "review_status": "editorial_review_required",
                  "future_entry_field": ["bármi"]
                }
              ]
            }
            """.trimIndent()

        val ENTRIES_WITH_A_MISSING_MANDATORY_FIELD =
            """
            {
              "schema_version": "1.0.0",
              "entries": [
                {
                  "id": "KY-0001",
                  "category_id": "stances",
                  "japanese_romaji": "Zenkutsu dachi",
                  "hungarian_name": "Elöl álló állás",
                  "review_status": "editorial_review_required"
                },
                {
                  "id": "KY-0002",
                  "category_id": "stances",
                  "japanese_romaji": "Kokutsu dachi",
                  "review_status": "editorial_review_required"
                }
              ]
            }
            """.trimIndent()
    }
}
