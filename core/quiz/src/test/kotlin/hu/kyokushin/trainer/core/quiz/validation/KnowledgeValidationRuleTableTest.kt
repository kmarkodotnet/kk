package hu.kyokushin.trainer.core.quiz.validation

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * A TR-01 **szabálytáblázat**: a V001…V010 kódok mindegyikére legalább egy explicit,
 * nevesített eset (DR-05, FR-31).
 *
 * Miért parametrizált, és miért nem 27 külön `@Test`? Mert a validátor szerződése maga is
 * táblázat (`.claude/skills/kyo-knowledge-base/SKILL.md` 4. pont): kód → szabály →
 * súlyosság → hatás. Táblázatos tesztként a lefedettség *leolvasható* – a
 * [ruleCoverage_everyIssueCodeHasANamedCase_TR01] teszt gépileg is ellenőrzi, hogy egyetlen
 * [IssueCode] se maradhasson eset nélkül, tehát egy új V-kód felvétele **bukó teszttel**
 * jár, nem csendben lefedetlen szabállyal.
 *
 * Minden eset neve a lefedett kóddal kezdődik, így a JUnit-jelentésben a
 * `validate_ruleTableCase_matchesTheRuleTable_TR01[V003_duplicateId_laterRecordRejected]`
 * alak önmagában megmondja, melyik szabály bukott el.
 *
 * A V010-nek szándékosan **tagadó** esete van: azt a kódot nem ez a réteg állítja elő
 * (lásd [IssueCode.V010] és a [V010_isNeverProducedByTheValidator] eset indoklását).
 */
@RunWith(Parameterized::class)
class KnowledgeValidationRuleTableTest(
    @Suppress("unused") private val caseName: String,
    private val case: RuleCase,
) {
    @Test
    fun validate_ruleTableCase_matchesTheRuleTable_TR01() {
        // A szerződés első szabálya minden esetre érvényes: a hiba adat, nem kivétel (FR-31).
        val outcome = runCatching { DefaultKnowledgeValidator().validate(case.input) }

        assertThat(outcome.exceptionOrNull()).isNull()
        case.assertions(outcome.getOrThrow())
    }

    /**
     * Egy táblázatsor: a bemenet és a hozzá tartozó elvárás.
     *
     * Az elvárás lambda, nem deklaratív mezőhalmaz: a szabályok hatása *kvalitatívan*
     * különbözik (teljes elutasítás, rekordelvetés, mezőlefokozás), és egy minden esetre
     * ráhúzott, részben kitöltött elvárásobjektum pont azt rejtené el, ami a V-kódok
     * közti lényegi különbség.
     */
    class RuleCase(
        val input: KnowledgeInput,
        val assertions: (ValidationReport) -> Unit,
    )

    companion object {
        /**
         * Minden [IssueCode] kódhoz tartozó esetek. A kulcs az eset neve, és a
         * konvenció kötelező: `V0nn_` előtaggal kezdődik, mert a lefedettségi teszt ebből
         * állapítja meg, melyik szabály van lefedve.
         */
        private val cases: List<Pair<String, RuleCase>> = buildList {

            // ---------------- V001 – a schemaVersion major verziója -------------------

            add(
                "V001_missingSchemaVersion_rejectsWholeDataset" to RuleCase(
                    input = KnowledgeInput(manifest(schemaVersion = null), listOf(validEntry())),
                ) { report ->
                    report.assertDatasetRejectedBy(IssueCode.V001, field = "schemaVersion")
                    assertThat(report.schemaVersion).isNull()
                },
            )
            add(
                "V001_unsupportedMajorVersion_rejectsWholeDataset" to RuleCase(
                    input = KnowledgeInput(manifest(schemaVersion = "2.0.0"), listOf(validEntry())),
                ) { report ->
                    report.assertDatasetRejectedBy(IssueCode.V001, field = "schemaVersion")
                    // A diagnosztikának meg kell tudnia mondani, *melyik* verziót kaptuk.
                    assertThat(report.schemaVersion).isEqualTo("2.0.0")
                },
            )
            add(
                "V001_nonNumericMajorVersion_rejectsWholeDataset" to RuleCase(
                    input = KnowledgeInput(manifest(schemaVersion = "egy.nulla.nulla"), listOf(validEntry())),
                ) { report -> report.assertDatasetRejectedBy(IssueCode.V001, field = "schemaVersion") },
            )
            add(
                "V001_supportedMajorWithHigherMinor_isAccepted" to RuleCase(
                    input = KnowledgeInput(manifest(schemaVersion = "1.7.3"), listOf(validEntry())),
                ) { report ->
                    assertThat(report.datasetRejected).isFalse()
                    assertThat(report.issuesOf(IssueCode.V001)).isEmpty()
                    assertThat(report.accepted).hasSize(1)
                },
            )

            // ---------------- V002 – kötelező mezők ----------------------------------

            add(
                "V002_blankId_rejectsRecord" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(id = "   "))),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V002, field = "id") },
            )
            add(
                "V002_missingCategoryId_rejectsRecord" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(categoryId = null))),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V002, field = "categoryId") },
            )
            add(
                "V002_blankRomaji_rejectsRecord" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(romaji = "  "))),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V002, field = "romaji") },
            )
            add(
                "V002_blankHungarian_rejectsRecord" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(hungarian = ""))),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V002, field = "hungarian") },
            )
            add(
                "V002_missingContentVersion_rejectsEveryRecord" to RuleCase(
                    input = KnowledgeInput(
                        manifest(contentVersion = null),
                        listOf(validEntry(id = "KY-0001"), validEntry(id = "KY-0002")),
                    ),
                ) { report ->
                    // Nem DATASET hatókör: a teljes elutasítás a V001/V010 fenntartott esete.
                    assertThat(report.datasetRejected).isFalse()
                    assertThat(report.accepted).isEmpty()
                    assertThat(report.rejected).hasSize(2)
                    assertThat(report.issuesOf(IssueCode.V002).map { it.field })
                        .containsExactly("datasetVersion", "datasetVersion")
                },
            )
            add(
                "V002_everyMandatoryFieldMissing_reportsAllOfThemAtOnce" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(TechniqueInput())),
                ) { report ->
                    // A tartalomszerkesztőnek egy körben kell látnia minden hiányt.
                    assertThat(report.issuesOf(IssueCode.V002).map { it.field })
                        .containsExactly("id", "categoryId", "romaji", "hungarian")
                    assertThat(report.rejected).hasSize(1)
                },
            )

            // ---------------- V003 – az id egyedisége --------------------------------

            add(
                "V003_duplicateId_rejectsTheLaterRecordOnly" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(
                            validEntry(id = "KY-0001", romaji = "Mae geri", hungarian = "Előre rúgás"),
                            validEntry(id = "KY-0001", romaji = "Mawashi geri", hungarian = "Körrúgás"),
                        ),
                    ),
                ) { report ->
                    val issue = report.assertRecordRejectedBy(IssueCode.V003, field = "id")
                    // A sorrend jelentéshordozó: a *későbbi* rekord esik ki.
                    assertThat(issue.entryIndex).isEqualTo(1)
                    assertThat(report.accepted.single().romaji).isEqualTo("Mae geri")
                },
            )
            add(
                "V003_duplicateIdDifferingOnlyInWhitespace_isStillADuplicate" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(
                            validEntry(id = "KY-0001"),
                            validEntry(id = "  KY-0001  ", romaji = "Mawashi geri", hungarian = "Körrúgás"),
                        ),
                    ),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V003, field = "id") },
            )

            // ---------------- V004 – létező kategóriaazonosító ------------------------

            add(
                "V004_unknownCategoryId_rejectsRecord" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(categoryId = "nincs-ilyen"))),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V004, field = "categoryId") },
            )
            add(
                "V004_manifestWithoutUsableCategories_rejectsEveryRecord" to RuleCase(
                    input = KnowledgeInput(
                        manifest(categories = listOf(CategoryInput(id = null), CategoryInput(id = "   "))),
                        listOf(validEntry()),
                    ),
                ) { report -> report.assertRecordRejectedBy(IssueCode.V004, field = "categoryId") },
            )
            add(
                "V004_knownCategoryId_producesNoIssue" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(categoryId = "etiquette"))),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V004)).isEmpty()
                    assertThat(report.accepted).hasSize(1)
                },
            )

            // ---------------- V005 – képhivatkozás ------------------------------------

            add(
                "V005_danglingIllustrationRef_keepsRecordButMarksItIncomplete" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(illustration = "KY-9999"))),
                ) { report ->
                    val issue = report.assertFieldIssue(IssueCode.V005, field = "illustration")
                    // Lógó hivatkozás = tartalmi hiba, ezért a szigorúbb olvasat.
                    assertThat(issue.severity).isEqualTo(Severity.BLOCKING)
                    val technique = report.accepted.single()
                    assertThat(technique.illustration).isEqualTo(IllustrationRef.Placeholder)
                    assertThat(technique.status).isEqualTo(VerificationStatus.INCOMPLETE)
                },
            )
            add(
                "V005_illustrationFileMissingFromBundle_degradesToPlaceholderOnly" to RuleCase(
                    input = KnowledgeInput(
                        manifest(
                            illustrations = listOf(
                                IllustrationInput(
                                    ref = "KY-0044",
                                    file = "illustrations/KY-0044.jpg",
                                    license = "project-internal",
                                ),
                            ),
                        ),
                        listOf(validEntry(illustration = "KY-0044", verified = true)),
                        availableIllustrationFiles = emptySet(),
                    ),
                ) { report ->
                    val issue = report.assertFieldIssue(IssueCode.V005, field = "illustration")
                    assertThat(issue.severity).isEqualTo(Severity.WARNING)
                    val technique = report.accepted.single()
                    assertThat(technique.illustration).isEqualTo(IllustrationRef.Placeholder)
                    // Csomagolási hiányosság: a szöveges kvízt és a tanulást nem érinti.
                    assertThat(technique.status).isEqualTo(VerificationStatus.VERIFIED)
                },
            )
            add(
                "V005_registryEntryWithoutLicense_degradesToPlaceholder" to RuleCase(
                    input = KnowledgeInput(
                        manifest(
                            illustrations =
                                listOf(IllustrationInput(ref = "KY-0044", file = "KY-0044.jpg", license = null)),
                        ),
                        listOf(validEntry(illustration = "KY-0044")),
                        availableIllustrationFiles = setOf("KY-0044.jpg"),
                    ),
                ) { report ->
                    report.assertFieldIssue(IssueCode.V005, field = "illustration")
                    assertThat(report.accepted.single().illustration).isEqualTo(IllustrationRef.Placeholder)
                },
            )
            add(
                "V005_intentionalPlaceholderWithNullFile_producesNoIssue" to RuleCase(
                    input = KnowledgeInput(
                        manifest(
                            illustrations =
                                listOf(IllustrationInput(ref = "placeholder", file = null, license = "N/A")),
                        ),
                        listOf(validEntry(illustration = "placeholder")),
                    ),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V005)).isEmpty()
                    assertThat(report.accepted.single().illustration).isEqualTo(IllustrationRef.Placeholder)
                },
            )
            add(
                "V005_licensedFilePresentInBundle_becomesAssetWithoutIssue" to RuleCase(
                    input = KnowledgeInput(
                        manifest(
                            illustrations = listOf(
                                IllustrationInput(
                                    ref = "KY-0044",
                                    file = "illustrations/KY-0044.jpg",
                                    license = "project-internal",
                                    quizUsable = true,
                                    hideableOnStudyCard = true,
                                ),
                            ),
                        ),
                        listOf(validEntry(illustration = "KY-0044")),
                        availableIllustrationFiles = setOf("KY-0044.jpg"),
                    ),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V005)).isEmpty()
                    assertThat(report.accepted.single().illustration)
                        .isEqualTo(
                            IllustrationRef.Asset(
                                "KY-0044",
                                "KY-0044.jpg",
                                quizUsable = true,
                                hideableOnStudyCard = true,
                            ),
                        )
                },
            )

            // ---------------- V006 – szinonimaütközés ---------------------------------

            add(
                "V006_namesEqualAfterNormalization_formMutualExclusionGroup" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(
                            validEntry(id = "KY-0001", romaji = "Mae geri", hungarian = "Előre rúgás"),
                            validEntry(id = "KY-0002", romaji = "mae  geri", hungarian = "Elülső rúgás"),
                        ),
                    ),
                ) { report ->
                    assertThat(report.accepted).hasSize(2)
                    assertThat(report.synonymGroups).hasSize(1)
                    assertThat(report.synonymGroups.single().map { it.value }).containsExactly("KY-0001", "KY-0002")
                    // Mindkét rekord kap észrevételt: a kizárás kölcsönös (CR-07).
                    assertThat(report.issuesOf(IssueCode.V006)).hasSize(2)
                },
            )
            add(
                "V006_aliasCollidingWithAnotherRecordsName_formsGroup" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(
                            validEntry(id = "KY-0001", romaji = "Mae geri", hungarian = "Előre rúgás"),
                            validEntry(
                                id = "KY-0002",
                                romaji = "Mawashi geri",
                                hungarian = "Körrúgás",
                                aliases = listOf("máe gerí"),
                            ),
                        ),
                    ),
                ) { report ->
                    assertThat(report.synonymGroups).hasSize(1)
                    assertThat(report.synonymGroups.single().map { it.value }).containsExactly("KY-0001", "KY-0002")
                },
            )
            add(
                "V006_distinctNames_produceNoGroup" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(
                            validEntry(id = "KY-0001", romaji = "Mae geri", hungarian = "Előre rúgás"),
                            validEntry(id = "KY-0002", romaji = "Mawashi geri", hungarian = "Körrúgás"),
                        ),
                    ),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V006)).isEmpty()
                    assertThat(report.synonymGroups).isEmpty()
                },
            )

            // ---------------- V007 / V008 – a kvízképesség kapuja ---------------------

            add(
                "V007_quizEligibleRequestedOnIncompleteRecord_isDowngraded" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(validEntry(illustration = "KY-9999", verified = true, quizEligible = true)),
                    ),
                ) { report ->
                    report.assertFieldIssue(IssueCode.V007, field = "quizEligible")
                    assertThat(report.accepted.single().status).isEqualTo(VerificationStatus.INCOMPLETE)
                },
            )
            add(
                "V007_quizEligibleNotRequested_producesNoDowngradeIssue" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(validEntry(illustration = "KY-9999", quizEligible = null)),
                    ),
                ) { report ->
                    // A kapu nem a *hiányzó* kérésre szól: lefokozni csak kért jogosultságot lehet.
                    assertThat(report.issuesOf(IssueCode.V007)).isEmpty()
                    assertThat(report.issuesOf(IssueCode.V008)).isEmpty()
                },
            )
            add(
                "V008_quizEligibleRequestedWithLabelledImage_downgradesImageTypes" to RuleCase(
                    input = KnowledgeInput(
                        manifest(
                            illustrations = listOf(
                                IllustrationInput(
                                    ref = "KY-0045",
                                    file = "illustrations/KY-0045.jpg",
                                    license = "project-internal",
                                    // Belesütött névfelirat: a kép elárulná a megoldást (ADR-011).
                                    quizUsable = false,
                                ),
                            ),
                        ),
                        listOf(validEntry(illustration = "KY-0045", verified = true, quizEligible = true)),
                        availableIllustrationFiles = setOf("KY-0045.jpg"),
                    ),
                ) { report ->
                    report.assertFieldIssue(IssueCode.V008, field = "quizEligible")
                    assertThat(report.accepted.single().status).isEqualTo(VerificationStatus.VERIFIED)
                },
            )

            // ---------------- V009 – aliastisztítás -----------------------------------

            add(
                "V009_blankAndNullAliases_areDropped" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(aliases = listOf(null, "", "   ")))),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V009)).hasSize(3)
                    assertThat(report.accepted.single().aliases).isEmpty()
                },
            )
            add(
                "V009_aliasEqualToOwnName_isDropped" to RuleCase(
                    input = KnowledgeInput(
                        manifest(),
                        listOf(
                            validEntry(
                                romaji = "Mae geri",
                                hungarian = "Előre rúgás",
                                aliases = listOf("mae geri", "előre rúgás"),
                            ),
                        ),
                    ),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V009)).hasSize(2)
                    assertThat(report.accepted.single().aliases).isEmpty()
                },
            )
            add(
                "V009_duplicateAliases_keepOnlyTheFirstOccurrence" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(aliases = listOf("Maegeri", "maegeri")))),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V009)).hasSize(1)
                    assertThat(report.accepted.single().aliases).containsExactly("Maegeri")
                },
            )
            add(
                "V009_distinctNonCollidingAlias_isKeptWithoutIssue" to RuleCase(
                    input = KnowledgeInput(manifest(), listOf(validEntry(aliases = listOf(" Maegeri ")))),
                ) { report ->
                    assertThat(report.issuesOf(IssueCode.V009)).isEmpty()
                    assertThat(report.accepted.single().aliases).containsExactly("Maegeri")
                },
            )

            // ---------------- V010 – nem ennek a rétegnek a kódja ---------------------

            add(
                "V010_isNeverProducedByTheValidator" to RuleCase(
                    // A legdurvább bemenet: nem támogatott séma, hiányzó mezők, duplikált id.
                    input = KnowledgeInput(
                        manifest(schemaVersion = "nem-semver", contentVersion = null),
                        listOf(TechniqueInput(), validEntry(id = "KY-0001"), validEntry(id = "KY-0001")),
                    ),
                ) { report ->
                    // A V010 („a JSON nem parse-olható") az importréteg kódja (core/data,
                    // T-115): ha a deszerializálás elbukik, ez a validátor le sem fut, és
                    // nincs olyan `KnowledgeInput`, amiből V010 keletkezhetne. Ezt a
                    // tagadó eset *rögzíti*, hogy senki ne várjon tőle viselkedést.
                    assertThat(report.issuesOf(IssueCode.V010)).isEmpty()
                },
            )
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> = cases.map { (name, case) -> arrayOf(name, case) }

        /** A táblázat által lefedett kódok az esetnevek `V0nn_` előtagjából. */
        internal fun coveredCodes(): Set<IssueCode> = cases
            .map { (name, _) -> IssueCode.valueOf(name.substringBefore('_')) }
            .toSet()

        private fun ValidationReport.assertDatasetRejectedBy(code: IssueCode, field: String?) {
            assertThat(datasetRejected).isTrue()
            assertThat(accepted).isEmpty()
            val issue = issuesOf(code).single()
            assertThat(issue.severity).isEqualTo(Severity.BLOCKING)
            assertThat(issue.code.scope).isEqualTo(IssueScope.DATASET)
            assertThat(issue.field).isEqualTo(field)
        }

        private fun ValidationReport.assertRecordRejectedBy(code: IssueCode, field: String?): ValidationIssue {
            assertThat(datasetRejected).isFalse()
            val issue = issuesOf(code).single()
            assertThat(issue.severity).isEqualTo(Severity.BLOCKING)
            assertThat(issue.code.scope).isEqualTo(IssueScope.RECORD)
            assertThat(issue.field).isEqualTo(field)
            // A rekord elvetve, de nyers alakban megmaradt a jelentésben. Az `id`-ra nem
            // lehet állítást fogalmazni: a V003 esetében éppen az a lényeg, hogy ugyanaz
            // az `id` egyszer elfogadva, egyszer elvetve szerepel – a rekordot az `index`
            // azonosítja, nem az `id` (az maga is lehet hiányzó).
            val rejectedRecord = rejected.single { it.index == issue.entryIndex }
            assertThat(rejectedRecord.issues).contains(issue)
            return issue
        }

        private fun ValidationReport.assertFieldIssue(code: IssueCode, field: String?): ValidationIssue {
            // Mezőszintű szabály: a rekord megmarad, csak lefokozódik.
            assertThat(rejected).isEmpty()
            assertThat(accepted).isNotEmpty()
            val issue = issuesOf(code).single()
            assertThat(issue.code.scope).isEqualTo(IssueScope.FIELD)
            assertThat(issue.field).isEqualTo(field)
            assertThat(issue.detail).isNotEmpty()
            return issue
        }
    }
}

/**
 * A táblázat **lefedettségének** őre (TR-01).
 *
 * Nem a parametrizált osztályban él, mert ott minden esetre újra lefutna, és a bukása nem
 * egy szabályról, hanem a készlet egészéről szól.
 */
class KnowledgeValidationRuleCoverageTest {
    @Test
    fun ruleCoverage_everyIssueCodeHasANamedCase_TR01() {
        assertThat(KnowledgeValidationRuleTableTest.coveredCodes())
            .containsExactlyElementsIn(IssueCode.entries)
    }
}
