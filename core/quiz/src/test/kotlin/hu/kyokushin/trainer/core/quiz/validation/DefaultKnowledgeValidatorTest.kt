package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Test

/**
 * A V001…V009 szabályok végrehajtásának tesztjei (TR-01, DR-05, FR-31, CR-02, CR-07).
 *
 * Itt minden szabály legalább egy, a hatását bizonyító **egyedi** esettel szerepel. A
 * TR-01 szabályonkénti, nevesített táblázata a [KnowledgeValidationRuleTableTest]-ben él,
 * a szerződési garanciák (nem dob kivételt, nem emel státuszt) a
 * [KnowledgeValidatorGuaranteesTest]-ben, a valódi 161 rekordos tudástár pedig a
 * [RealKnowledgeBaseValidationTest]-ben. A közös bemenetépítők: `ValidationFixtures.kt`.
 */
class DefaultKnowledgeValidatorTest {
    private val validator = DefaultKnowledgeValidator()

    // ----------------------------------------------------------------------------
    // V001 – sémaverzió
    // ----------------------------------------------------------------------------

    @Test
    fun validate_unsupportedSchemaMajor_rejectsWholeDataset_V001_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(schemaVersion = "2.0.0"),
                entries = listOf(validEntry()),
            ),
        )

        assertThat(report.datasetRejected).isTrue()
        assertThat(report.accepted).isEmpty()
        assertThat(report.issuesOf(IssueCode.V001)).hasSize(1)
        assertThat(report.issuesOf(IssueCode.V001).single().severity).isEqualTo(Severity.BLOCKING)
        assertThat(report.schemaVersion).isEqualTo("2.0.0")
    }

    @Test
    fun validate_missingSchemaVersion_rejectsWholeDataset_V001_TR01() {
        val report = validator.validate(
            KnowledgeInput(manifest = manifest(schemaVersion = null), entries = listOf(validEntry())),
        )

        assertThat(report.datasetRejected).isTrue()
        assertThat(report.issuesOf(IssueCode.V001)).hasSize(1)
        assertThat(report.schemaVersion).isNull()
    }

    @Test
    fun validate_supportedSchemaMajorWithHigherMinor_isAccepted_V001_TR01() {
        val report = validator.validate(
            KnowledgeInput(manifest = manifest(schemaVersion = "1.2.3"), entries = listOf(validEntry())),
        )

        assertThat(report.datasetRejected).isFalse()
        assertThat(report.accepted).hasSize(1)
    }

    // ----------------------------------------------------------------------------
    // V002 – kötelező mezők
    // ----------------------------------------------------------------------------

    @Test
    fun validate_blankMandatoryFields_rejectsRecordWithoutThrowing_V002_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(
                    TechniqueInput(id = "  ", categoryId = null, romaji = "   ", hungarian = null),
                    validEntry(),
                ),
            ),
        )

        assertThat(report.accepted.map { it.id.value }).containsExactly("KY-0001")
        assertThat(report.rejected).hasSize(1)
        val rejection = report.rejected.single()
        assertThat(rejection.index).isEqualTo(0)
        assertThat(rejection.issues.map { it.field })
            .containsExactly("id", "categoryId", "romaji", "hungarian")
        // A hiányzó id miatt az entryId null – a rekordot az entryIndex azonosítja.
        assertThat(rejection.issues.map { it.entryId }.distinct()).containsExactly(null)
        assertThat(rejection.issues.map { it.entryIndex }.distinct()).containsExactly(0)
    }

    @Test
    fun validate_missingContentVersion_rejectsEveryRecord_V002_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(contentVersion = "   "),
                entries = listOf(validEntry()),
            ),
        )

        assertThat(report.accepted).isEmpty()
        assertThat(report.rejected).hasSize(1)
        assertThat(report.issuesOf(IssueCode.V002).map { it.field }).contains("datasetVersion")
        assertThat(report.datasetVersion).isNull()
    }

    // ----------------------------------------------------------------------------
    // V003 – egyedi id
    // ----------------------------------------------------------------------------

    @Test
    fun validate_duplicateId_rejectsTheLaterRecordOnly_V003_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(
                    validEntry(id = "KY-0001", hungarian = "első"),
                    validEntry(id = "KY-0001", hungarian = "második"),
                ),
            ),
        )

        assertThat(report.accepted.map { it.hungarian }).containsExactly("első")
        assertThat(report.rejected.single().index).isEqualTo(1)
        assertThat(report.issuesOf(IssueCode.V003).single().entryId).isEqualTo("KY-0001")
    }

    // ----------------------------------------------------------------------------
    // V004 – létező kategória
    // ----------------------------------------------------------------------------

    @Test
    fun validate_unknownCategoryId_rejectsRecord_V004_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(validEntry(categoryId = "nincs-ilyen")),
            ),
        )

        assertThat(report.accepted).isEmpty()
        assertThat(report.issuesOf(IssueCode.V004).single().field).isEqualTo("categoryId")
        assertThat(report.rejected.single().entry.categoryId).isEqualTo("nincs-ilyen")
    }

    // ----------------------------------------------------------------------------
    // V005 – képhivatkozás
    // ----------------------------------------------------------------------------

    @Test
    fun validate_illustrationFileMissingFromBundle_degradesToPlaceholderAndKeepsRecord_V005_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(
                    illustrations = listOf(
                        IllustrationInput(
                            ref = "KY-0044",
                            file = "illustrations/KY-0044.jpg",
                            license = "project-internal",
                        ),
                    ),
                ),
                entries = listOf(validEntry(illustration = "KY-0044")),
                availableIllustrationFiles = emptySet(),
            ),
        )

        val technique = report.accepted.single()
        assertThat(technique.illustration).isEqualTo(IllustrationRef.Placeholder)
        // A hiányzó fájl csak a képes kvíztípusokat zárja ki, a rekord nem lesz hiányos.
        assertThat(technique.status).isEqualTo(VerificationStatus.AWAITING_REVIEW)
        assertThat(report.issuesOf(IssueCode.V005).single().severity).isEqualTo(Severity.WARNING)
    }

    @Test
    fun validate_illustrationPresentInBundle_becomesAssetWithStricterGates_V005_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(
                    illustrations = listOf(
                        IllustrationInput(
                            ref = "KY-0044",
                            file = "illustrations/KY-0044.jpg",
                            license = "project-internal",
                            quizUsable = true,
                            hideableOnStudyCard = null,
                        ),
                    ),
                ),
                entries = listOf(validEntry(illustration = "KY-0044")),
                availableIllustrationFiles = setOf("KY-0044.jpg"),
            ),
        )

        val asset = report.accepted.single().illustration as IllustrationRef.Asset
        assertThat(asset.ref).isEqualTo("KY-0044")
        assertThat(asset.fileName).isEqualTo("KY-0044.jpg")
        assertThat(asset.quizUsable).isTrue()
        // Hiányzó `hideableOnStudyCard` → a szigorúbb olvasat: csak felfedés után látszik.
        assertThat(asset.hideableOnStudyCard).isTrue()
        assertThat(report.issuesOf(IssueCode.V005)).isEmpty()
    }

    @Test
    fun validate_danglingIllustrationRef_marksRecordIncomplete_V005_CR02() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(validEntry(illustration = "nincs-nyilvantartva", verified = true)),
            ),
        )

        val technique = report.accepted.single()
        assertThat(technique.illustration).isEqualTo(IllustrationRef.Placeholder)
        // Lefokozás: a tartalom `verified = true`, de a hivatkozás lógó – INCOMPLETE.
        assertThat(technique.status).isEqualTo(VerificationStatus.INCOMPLETE)
        assertThat(report.issuesOf(IssueCode.V005).single().severity).isEqualTo(Severity.BLOCKING)
    }

    @Test
    fun validate_registryFileIsNull_isIntentionalPlaceholderWithoutIssue_V005_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(
                    illustrations = listOf(IllustrationInput(ref = "placeholder", file = null, license = "N/A")),
                ),
                entries = listOf(validEntry(illustration = "placeholder")),
            ),
        )

        assertThat(report.accepted.single().illustration).isEqualTo(IllustrationRef.Placeholder)
        assertThat(report.accepted.single().status).isEqualTo(VerificationStatus.AWAITING_REVIEW)
        assertThat(report.issuesOf(IssueCode.V005)).isEmpty()
    }

    @Test
    fun validate_illustrationWithoutLicense_degradesToPlaceholder_V005_CR03() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(
                    illustrations = listOf(IllustrationInput(ref = "KY-0044", file = "KY-0044.jpg", license = null)),
                ),
                entries = listOf(validEntry(illustration = "KY-0044")),
                availableIllustrationFiles = setOf("KY-0044.jpg"),
            ),
        )

        assertThat(report.accepted.single().illustration).isEqualTo(IllustrationRef.Placeholder)
        assertThat(report.issuesOf(IssueCode.V005)).hasSize(1)
    }

    // ----------------------------------------------------------------------------
    // V006 – szinonimacsoportok
    // ----------------------------------------------------------------------------

    @Test
    fun validate_namesEqualAfterNormalization_formMutualExclusionGroup_V006_CR07() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(
                    validEntry(id = "KY-0001", romaji = "Mae geri", hungarian = "Előre rúgás"),
                    validEntry(id = "KY-0002", romaji = "MAE  GERI", hungarian = "Elülső rúgás"),
                    validEntry(id = "KY-0003", romaji = "Ushiro geri", hungarian = "Hátra rúgás"),
                ),
            ),
        )

        assertThat(report.accepted).hasSize(3)
        assertThat(report.synonymGroups).hasSize(1)
        assertThat(report.synonymGroups.single().map { it.value }).containsExactly("KY-0001", "KY-0002")
        assertThat(report.issuesOf(IssueCode.V006)).hasSize(2)
        assertThat(report.issuesOf(IssueCode.V006).map { it.severity }.distinct())
            .containsExactly(Severity.WARNING)
    }

    @Test
    fun validate_aliasMatchesAnotherRecordsName_formsGroup_V006_CR07() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(
                    validEntry(id = "KY-0001", romaji = "Mae geri", hungarian = "Előre rúgás"),
                    validEntry(
                        id = "KY-0002",
                        romaji = "Mae geri chudan",
                        hungarian = "Előre rúgás középre",
                        aliases = listOf("mae-geri", "MAE GERI"),
                    ),
                ),
            ),
        )

        assertThat(report.synonymGroups.single().map { it.value }).containsExactly("KY-0001", "KY-0002")
    }

    @Test
    fun validate_distinctNames_produceNoSynonymGroup_V006_CR07() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(
                    validEntry(id = "KY-0001", romaji = "Mae geri"),
                    validEntry(id = "KY-0002", romaji = "Ushiro geri", hungarian = "Hátra rúgás"),
                ),
            ),
        )

        assertThat(report.synonymGroups).isEmpty()
        assertThat(report.issuesOf(IssueCode.V006)).isEmpty()
    }

    // ----------------------------------------------------------------------------
    // V007 / V008 – kvízképesség (ADR-010)
    // ----------------------------------------------------------------------------

    @Test
    fun validate_quizEligibleRequestedOnIncompleteRecord_isDowngraded_V007_CR02() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(validEntry(illustration = "lógó-ref", quizEligible = true)),
            ),
        )

        val technique = report.accepted.single()
        assertThat(technique.status).isEqualTo(VerificationStatus.INCOMPLETE)
        assertThat(report.issuesOf(IssueCode.V007)).hasSize(1)
        assertThat(report.issuesOf(IssueCode.V007).single().field).isEqualTo("quizEligible")
        // ADR-010: a hiányos rekord egyetlen kérdéstípusban sem kvízképes.
        assertThat(QuizEligibility.eligibleTypes(technique, DatasetTrust.PRACTICE_ON_UNREVIEWED)).isEmpty()
    }

    @Test
    fun validate_quizEligibleRequestedWithUnusableImage_downgradesImageTypes_V008_ADR010() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(
                    illustrations = listOf(
                        IllustrationInput(
                            ref = "KY-0045",
                            file = "KY-0045.jpg",
                            license = "project-internal",
                            // Feliratos kép: hiteles tananyag, de kvízben elárulná a megoldást.
                            quizUsable = false,
                        ),
                    ),
                ),
                entries = listOf(validEntry(illustration = "KY-0045", quizEligible = true)),
                availableIllustrationFiles = setOf("KY-0045.jpg"),
            ),
        )

        val technique = report.accepted.single()
        assertThat(report.issuesOf(IssueCode.V008)).hasSize(1)
        assertThat(QuizEligibility.eligibleTypes(technique, DatasetTrust.PRACTICE_ON_UNREVIEWED))
            .containsExactly(QuestionType.JP_TO_HU, QuestionType.HU_TO_JP)
    }

    @Test
    fun validate_unreviewedButCompleteRecord_staysQuizEligibleForTextTypes_ADR010() {
        val report = validator.validate(
            KnowledgeInput(manifest = manifest(), entries = listOf(validEntry(quizEligible = true))),
        )

        val technique = report.accepted.single()
        assertThat(technique.status).isEqualTo(VerificationStatus.AWAITING_REVIEW)
        assertThat(report.issuesOf(IssueCode.V007)).isEmpty()
        assertThat(report.issuesOf(IssueCode.V008)).isEmpty()
        assertThat(QuizEligibility.isEligible(technique, QuestionType.JP_TO_HU, DatasetTrust.PRACTICE_ON_UNREVIEWED))
            .isTrue()
    }

    // ----------------------------------------------------------------------------
    // V009 – aliasok
    // ----------------------------------------------------------------------------

    @Test
    fun validate_blankSelfCollidingAndDuplicateAliases_areDropped_V009_TR01() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(),
                entries = listOf(
                    validEntry(
                        romaji = "Mae geri",
                        hungarian = "Előre rúgás",
                        aliases =
                            listOf("", null, "   ", "mae  geri", "ELŐRE RÚGÁS", "Zenkutsu", "zenkutsu", "Kokutsu"),
                    ),
                ),
            ),
        )

        val technique = report.accepted.single()
        assertThat(technique.aliases).containsExactly("Zenkutsu", "Kokutsu").inOrder()
        // 3 üres + 1 romaji-ütközés + 1 hungarian-ütközés + 1 duplikátum
        assertThat(report.issuesOf(IssueCode.V009)).hasSize(6)
    }

    // ----------------------------------------------------------------------------
    // Státusz és bizalmi szint – a „soha nem emel" garancia (CR-02)
    // ----------------------------------------------------------------------------

    @Test
    fun validate_verifiedFalseOrMissing_neverBecomesVerified_CR02() {
        val report = validator.validate(
            KnowledgeInput(
                manifest = manifest(datasetKind = "draft_review_required"),
                entries = listOf(
                    validEntry(id = "KY-0001", verified = null),
                    validEntry(id = "KY-0002", verified = false, hungarian = "másik"),
                ),
            ),
        )

        assertThat(report.accepted.map { it.status }.distinct())
            .containsExactly(VerificationStatus.AWAITING_REVIEW)
    }

    @Test
    fun validate_unknownDatasetKind_fallsBackToStricterTrust_CR02() {
        val report = validator.validate(
            KnowledgeInput(manifest = manifest(datasetKind = "valami-ismeretlen"), entries = emptyList()),
        )

        assertThat(report.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
    }

    @Test
    fun validate_reviewedDatasetKind_isTheOnlyPathToReviewedTrust_CR02() {
        val report = validator.validate(
            KnowledgeInput(manifest = manifest(datasetKind = "Reviewed"), entries = emptyList()),
        )

        assertThat(report.datasetTrust).isEqualTo(DatasetTrust.REVIEWED)
    }

    @Test
    fun validate_derivesRomajiAsciiWithoutDiacritics_FR03() {
        val report = validator.validate(
            KnowledgeInput(manifest = manifest(), entries = listOf(validEntry(romaji = "Chūdan Mae Geri"))),
        )

        assertThat(report.accepted.single().romajiAscii).isEqualTo("Chudan Mae Geri")
    }

    // ----------------------------------------------------------------------------
    // A szerződés lelke: semmilyen bemenet nem dob kivételt (FR-31)
    // ----------------------------------------------------------------------------

    @Test
    fun validate_hostileInput_neverThrows_FR31() {
        val input = KnowledgeInput(
            manifest = ManifestInput(
                schemaVersion = "1.0.0",
                contentVersion = "0.1.0",
                datasetKind = null,
                categories =
                    listOf(CategoryInput(id = null, nameHu = null, sortOrder = null), CategoryInput(id = "   ")),
                illustrations = listOf(IllustrationInput(), IllustrationInput(ref = "  ")),
            ),
            entries = listOf(
                TechniqueInput(),
                TechniqueInput(id = "", categoryId = "", romaji = "", hungarian = "", aliases = listOf(null, "")),
                TechniqueInput(id = "KY-0001", categoryId = "etiquette", romaji = "Osu", hungarian = "Köszönés"),
                TechniqueInput(id = "KY-0001", categoryId = "etiquette", romaji = "Osu", hungarian = "Köszönés"),
            ),
        )

        val outcome = runCatching { validator.validate(input) }

        assertThat(outcome.exceptionOrNull()).isNull()
        val report = outcome.getOrThrow()
        // Egyetlen kategória sem érvényes a manifestben, ezért minden rekord elvetve.
        assertThat(report.accepted).isEmpty()
        assertThat(report.rejected).hasSize(4)
        assertThat(report.issues).isNotEmpty()
    }

    @Test
    fun validate_sameInputTwice_producesIdenticalReport_NFR07() {
        val input = KnowledgeInput(
            manifest = manifest(),
            entries = listOf(
                validEntry(id = "KY-0001", romaji = "Mae geri"),
                validEntry(id = "KY-0002", romaji = "mae geri", hungarian = "ugyanaz"),
                validEntry(id = "KY-0003", categoryId = "nincs-ilyen"),
            ),
        )

        assertThat(validator.validate(input)).isEqualTo(validator.validate(input))
    }
}
