package hu.kyokushin.trainer.core.quiz.validation

import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.text.NameNormalizer

/**
 * A [KnowledgeValidator] egyetlen implementációja: a V001…V009 szabályok végrehajtása a
 * kanonikus séma felett (DR-05, FR-31, CR-02, CR-07, TR-01).
 *
 * A `V010` szándékosan nincs itt: azt a kódot a bemenetet összeállító importréteg
 * (`core/data`, T-115) állítja elő, amikor a JSON deszerializálása meghiúsul – ilyenkor ez
 * a validátor le sem fut.
 *
 * ## A feldolgozás menete
 *
 * 1. **V001** – a `schemaVersion` major verziója. Nem támogatott vagy olvashatatlan érték
 *    esetén a feldolgozás *itt megáll*: a jelentés egyetlen, [IssueScope.DATASET] hatókörű
 *    BLOKKOLÓ észrevételt hordoz, és az `accepted` lista üres.
 * 2. **Rekordszintű elvetés** – V002 (kötelező mező), V003 (duplikált `id`), V004
 *    (ismeretlen `categoryId`). Minden indok összegyűlik, nem csak az első: a
 *    tartalomszerkesztőnek egy körben kell látnia, mi a hiba.
 * 3. **Lefokozások** – V005 (képhivatkozás), V009 (alias), V007/V008 (kvízképesség). Ezek
 *    nem vetnek el rekordot, csak mezőt fokoznak le.
 * 4. **Leképezés** – a [Technique] domain objektum *csak* az előző lépések után készül el.
 *    Ez nem stílus: a [Technique] konstruktora `require()`-rel validál, és blank kötelező
 *    mezőre kivételt dob, ezért a hibás rekordot a leképezés **előtt** kell elvetni.
 * 5. **V006** – a szinonimacsoportok az elfogadott rekordok normalizált nevei alapján.
 *
 * ## Amit ez az osztály soha nem tesz
 *
 * - **Nem dob kivételt.** A leképezés is `runCatching`-ben fut: ha a domain modell
 *   bármilyen, itt nem előre látott invariánst sért, az V002-ként *jelentésbe* kerül, nem
 *   a hívó nyakába (FR-31).
 * - **Nem emel státuszt.** [VerificationStatus.VERIFIED] kizárólag a tartalom
 *   `verified = true` értékéből keletkezhet; a hibás képhivatkozás *lefokozza*
 *   [VerificationStatus.INCOMPLETE]-re. Felfelé nincs kódút (CR-02).
 * - **Nem nyúl a fájlrendszerhez, az órához és az RNG-hez** (ADR-001, NFR-07).
 */
class DefaultKnowledgeValidator : KnowledgeValidator {

    override fun validate(input: KnowledgeInput): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val schemaVersion = input.manifest.schemaVersion?.trim()?.takeIf { it.isNotEmpty() }
        val datasetVersion = input.manifest.contentVersion?.trim()?.takeIf { it.isNotEmpty() }
        val datasetTrust = resolveDatasetTrust(input.manifest.datasetKind)

        // --- V001: nem támogatott séma → a teljes betöltés elutasítva. ---
        if (majorVersionOf(schemaVersion) != SUPPORTED_SCHEMA_MAJOR) {
            issues += ValidationIssue(
                code = IssueCode.V001,
                severity = IssueCode.V001.defaultSeverity,
                field = "schemaVersion",
                detail = "Nem támogatott schemaVersion: ${schemaVersion ?: "hiányzik"}. " +
                    "A támogatott major verzió: $SUPPORTED_SCHEMA_MAJOR.",
            )
            return ValidationReport(
                issues = issues,
                datasetTrust = datasetTrust,
                datasetVersion = datasetVersion,
                schemaVersion = schemaVersion,
            )
        }

        val categoryIds = input.manifest.categories
            .mapNotNull { it.id?.trim()?.takeIf { id -> id.isNotEmpty() } }
            .toSet()
        // Ismétlődő `ref` esetén az első bejegyzés nyer: a nyilvántartás determinisztikus
        // olvasata nem függhet a fájlon belüli sorrend véletlenétől.
        val illustrationRegistry = LinkedHashMap<String, IllustrationInput>()
        input.manifest.illustrations.forEach { entry ->
            val ref = entry.ref?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            illustrationRegistry.putIfAbsent(ref, entry)
        }

        val accepted = mutableListOf<Technique>()
        val rejected = mutableListOf<RejectedRecord>()
        val seenIds = mutableSetOf<String>()
        // Az elfogadott rekordok fájlbeli indexe: a V006 észrevételeknek is az
        // `entries` listán belüli indexre kell hivatkozniuk, nem az `accepted` listáéra.
        val entryIndexById = mutableMapOf<TechniqueId, Int>()

        input.entries.forEachIndexed { index, entry ->
            val rawId = entry.id
            val id = rawId?.trim().orEmpty()
            val reportedId = id.takeIf { it.isNotEmpty() }
            val recordIssues = mutableListOf<ValidationIssue>()

            fun issue(
                code: IssueCode,
                field: String?,
                detail: String,
                severity: Severity = code.defaultSeverity,
            ) = ValidationIssue(
                code = code,
                severity = severity,
                entryId = reportedId,
                entryIndex = index,
                field = field,
                detail = detail,
            )

            // --- V002: kötelező mezők. Minden hiányzó mező külön észrevételt kap. ---
            mandatoryFields(entry, datasetVersion).forEach { (field, label) ->
                recordIssues += issue(IssueCode.V002, field, "Kötelező mező hiányzik vagy üres: $label.")
            }

            // --- V003: az `id` egyedisége a fájlon belül; a *későbbi* rekord esik ki. ---
            if (id.isNotEmpty() && !seenIds.add(id)) {
                recordIssues += issue(
                    IssueCode.V003,
                    "id",
                    "Duplikált id: „$id\" már szerepelt korábbi rekordon; a későbbi rekord elvetve.",
                )
            }

            // --- V004: a `categoryId` létezik a manifest kategóriái között. ---
            val categoryId = entry.categoryId?.trim().orEmpty()
            if (categoryId.isNotEmpty() && categoryId !in categoryIds) {
                recordIssues += issue(
                    IssueCode.V004,
                    "categoryId",
                    "Ismeretlen categoryId: „$categoryId\" nem szerepel a manifest kategóriái között.",
                )
            }

            if (recordIssues.any { it.severity == Severity.BLOCKING && it.code.scope == IssueScope.RECORD }) {
                rejected += RejectedRecord(index = index, entry = entry, issues = recordIssues.toList())
                issues += recordIssues
                return@forEachIndexed
            }

            // --- V009: aliasok tisztítása (üres, önütköző, duplikált). ---
            val ownNames = setOf(
                NameNormalizer.normalize(entry.romaji),
                NameNormalizer.normalize(entry.hungarian),
            )
            val keptAliases = mutableListOf<String>()
            val seenAliases = mutableSetOf<String>()
            entry.aliases.forEach { alias ->
                val trimmed = alias?.trim()
                val normalized = NameNormalizer.normalize(trimmed)
                when {
                    normalized.isEmpty() ->
                        recordIssues += issue(IssueCode.V009, "aliases", "Üres vagy hiányzó alias eldobva.")

                    normalized in ownNames -> recordIssues += issue(
                        IssueCode.V009,
                        "aliases",
                        "A saját romaji/hungarian értékkel megegyező alias eldobva: „$trimmed\".",
                    )

                    !seenAliases.add(normalized) -> recordIssues += issue(
                        IssueCode.V009,
                        "aliases",
                        "Duplikált alias eldobva: „$trimmed\".",
                    )

                    else -> keptAliases += requireNotNull(trimmed)
                }
            }

            // --- V005: képhivatkozás feloldása, szükség esetén Asset → Placeholder. ---
            val resolution = resolveIllustration(entry.illustration, illustrationRegistry, input.availableIllustrationFiles)
            resolution.detail?.let { detail ->
                recordIssues += issue(IssueCode.V005, "illustration", detail, resolution.severity)
            }

            // Státusz: VERIFIED csak a tartalomból; a hibás hivatkozás lefokoz (CR-02, ADR-002).
            val status = when {
                resolution.brokenReference -> VerificationStatus.INCOMPLETE
                entry.verified == true -> VerificationStatus.VERIFIED
                else -> VerificationStatus.AWAITING_REVIEW
            }

            val mapped = runCatching {
                Technique(
                    id = TechniqueId.of(id),
                    categoryId = CategoryId.of(categoryId),
                    romaji = requireNotNull(entry.romaji).trim(),
                    romajiAscii = asciiFormOf(requireNotNull(entry.romaji)),
                    hungarian = requireNotNull(entry.hungarian).trim(),
                    japaneseScript = entry.japaneseScript?.trim()?.takeIf { it.isNotEmpty() },
                    aliases = keptAliases.toList(),
                    descriptionHu = entry.descriptionHu?.trim()?.takeIf { it.isNotEmpty() },
                    illustration = resolution.ref,
                    status = status,
                    quizExcluded = entry.quizExcluded == true,
                    retired = entry.retired == true,
                    datasetVersion = requireNotNull(datasetVersion),
                )
            }

            val technique = mapped.getOrElse { failure ->
                // Védőháló: ha a domain modell olyan invariánst sért, amit fentebb nem
                // fogtunk el, az is *jelentés* lesz, nem kivétel a hívónál (FR-31).
                recordIssues += issue(
                    IssueCode.V002,
                    null,
                    "A rekord nem képezhető le domain modellre: ${failure.message}",
                )
                rejected += RejectedRecord(index = index, entry = entry, issues = recordIssues.toList())
                issues += recordIssues
                return@forEachIndexed
            }

            // --- V007/V008: a kvízképesség kapuja (ADR-010). Csak lefokozás, soha emelés. ---
            if (entry.quizEligible == true) {
                if (status == VerificationStatus.INCOMPLETE) {
                    recordIssues += issue(
                        IssueCode.V007,
                        "quizEligible",
                        "A tartalom quizEligible = true-t kér, de a rekord hiányos (INCOMPLETE): " +
                            "a kvízképesség lefokozva false-ra (ADR-010).",
                    )
                } else if (resolution.ref != null && !QuizEligibility.hasQuizUsableIllustration(technique)) {
                    recordIssues += issue(
                        IssueCode.V008,
                        "quizEligible",
                        "A rekord hivatkozik illusztrációra, de nincs kvízben használható képe: " +
                            "a képes kérdéstípusok (IMAGE_TO_JP, JP_TO_IMAGE) lefokozva.",
                    )
                }
            }

            accepted += technique
            entryIndexById[technique.id] = index
            issues += recordIssues
        }

        // --- V006: szinonimacsoportok az elfogadott rekordok normalizált nevei alapján. ---
        val synonyms = detectSynonyms(accepted, entryIndexById)
        issues += synonyms.issues

        return ValidationReport(
            accepted = accepted.toList(),
            rejected = rejected.toList(),
            issues = issues.toList(),
            synonymGroups = synonyms.groups,
            datasetTrust = datasetTrust,
            datasetVersion = datasetVersion,
            schemaVersion = schemaVersion,
        )
    }

    /**
     * A hiányzó kötelező mezők (séma 3. pont): `id`, `categoryId`, `romaji`, `hungarian`.
     *
     * A `manifest.contentVersion` is itt szerepel: a [Technique.datasetVersion] kötelező
     * mező, tehát hiányzó tartalomverzió mellett **egyetlen** rekord sem képezhető le. Ez
     * rekordonkénti V002, nem teljes elutasítás: a V001/V010 hatókör a szerződés szerint a
     * nem támogatott sémára és a parse-hibára van fenntartva.
     */
    private fun mandatoryFields(entry: TechniqueInput, datasetVersion: String?): List<Pair<String, String>> =
        buildList {
            if (entry.id.isNullOrBlank()) add("id" to "id")
            if (entry.categoryId.isNullOrBlank()) add("categoryId" to "categoryId")
            if (entry.romaji.isNullOrBlank()) add("romaji" to "romaji (japanese_romaji)")
            if (entry.hungarian.isNullOrBlank()) add("hungarian" to "hungarian (hungarian_name)")
            if (datasetVersion == null) add("datasetVersion" to "manifest.contentVersion")
        }

    /**
     * A `romajiAscii` származtatott alakja (ADR-011: `japanese_romaji_ascii`).
     *
     * Ha az ékezettelenítés üres eredményt adna (csak kombináló jelekből álló név), a nyers
     * trimmelt alak marad – üres `romajiAscii` a [Technique] konstruktorát dobná meg.
     */
    private fun asciiFormOf(romaji: String): String =
        NameNormalizer.toAscii(romaji).takeIf { it.isNotBlank() } ?: romaji.trim()

    /**
     * A bizalmi szint a manifest szerkesztői állapotából (ADR-010).
     *
     * Ismeretlen, hiányzó vagy értelmezhetetlen érték esetén a **szigorúbb** olvasat jön:
     * bizalmi szintet kód soha nem emel (CR-02). A `reviewed` érték az egyetlen, amire
     * [DatasetTrust.REVIEWED] jár.
     */
    private fun resolveDatasetTrust(raw: String?): DatasetTrust =
        when (NameNormalizer.normalize(raw)) {
            "reviewed", "production" -> DatasetTrust.REVIEWED
            else -> DatasetTrust.PRACTICE_ON_UNREVIEWED
        }

    /** A `schemaVersion` semver string major része, vagy `null`, ha nem olvasható (V001). */
    private fun majorVersionOf(schemaVersion: String?): Int? =
        schemaVersion?.substringBefore('.')?.trim()?.toIntOrNull()

    /**
     * A V005 szabály kimenete egy rekordra.
     *
     * @property ref a domainbe kerülő képhivatkozás: [IllustrationRef.Asset],
     *   [IllustrationRef.Placeholder], vagy `null`, ha a rekord nem hivatkozik képre.
     * @property detail a V005 észrevétel szövege, vagy `null`, ha nincs észrevétel.
     * @property severity a tényleges súlyosság; a *lógó* hivatkozás szigorúbb a hiányzó
     *   fájlnál, mert az előbbi tartalmi hiba, az utóbbi csomagolási hiányosság.
     * @property brokenReference lógó hivatkozás-e, azaz lefokozza-e a rekord státuszát
     *   [VerificationStatus.INCOMPLETE]-re.
     */
    private data class IllustrationResolution(
        val ref: IllustrationRef?,
        val detail: String? = null,
        val severity: Severity = IssueCode.V005.defaultSeverity,
        val brokenReference: Boolean = false,
    )

    /**
     * Feloldja a rekord képhivatkozását a manifest nyilvántartása és a csomagban tényleg
     * jelen lévő fájlnevek alapján (V005, ADR-003).
     *
     * Négy eset kap eltérő kezelést, és ez a különbségtétel szándékos:
     *
     * | Eset | Kimenet | Miért |
     * |---|---|---|
     * | nincs hivatkozás | `null` | a rekordnak nincs képe, ez nem hiba |
     * | a `ref` nincs a nyilvántartásban | `Placeholder` + BLOKKOLÓ + `INCOMPLETE` | lógó hivatkozás: tartalmi hiba (ADR-002 „hibás hivatkozású rekord") |
     * | a nyilvántartásban `file = null` | `Placeholder`, észrevétel nélkül | szándékos helyőrző |
     * | licenc nélkül, vagy a fájl nincs a csomagban | `Placeholder` + FIGYELM. | a tartalom rendben van, csak a kép nem használható (CR-03, V005) |
     *
     * A hiányzó fájl **nem** fokozza le a státuszt: az csak a képes kérdéstípusokat zárja
     * ki, a szöveges kvízt és a tanulást nem (V005 hatása a szabálytáblázat szerint).
     */
    private fun resolveIllustration(
        rawRef: String?,
        registry: Map<String, IllustrationInput>,
        availableFiles: Set<String>,
    ): IllustrationResolution {
        val ref = rawRef?.trim()?.takeIf { it.isNotEmpty() } ?: return IllustrationResolution(ref = null)
        val entry = registry[ref]
            ?: return IllustrationResolution(
                ref = IllustrationRef.Placeholder,
                detail = "Ismeretlen képhivatkozás: „$ref\" nem szerepel a manifest illustrations " +
                    "nyilvántartásában; a rekord helyőrző képet kap és hiányosnak minősül.",
                severity = Severity.BLOCKING,
                brokenReference = true,
            )

        // A nyilvántartás útvonalat is tartalmazhat (`illustrations/KY-0044.jpg`), a
        // csomagban viszont fájlnevek vannak – a kettőt a fájlnévre hozzuk össze.
        val fileName = entry.file?.trim()?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
            ?: return IllustrationResolution(ref = IllustrationRef.Placeholder)

        if (entry.license.isNullOrBlank()) {
            return IllustrationResolution(
                ref = IllustrationRef.Placeholder,
                detail = "A „$ref\" illusztrációnak nincs licence, ezért nem használható (CR-03); " +
                    "a rekord helyőrző képet kap.",
            )
        }

        if (fileName !in availableFiles) {
            return IllustrationResolution(
                ref = IllustrationRef.Placeholder,
                detail = "A „$ref\" illusztráció fájlja („$fileName\") nincs a csomagban; " +
                    "a rekord helyőrző képet kap, és kiesik a képes kvíztípusokból.",
            )
        }

        return IllustrationResolution(
            ref = IllustrationRef.Asset(
                ref = ref,
                fileName = fileName,
                // Hiányzó érték esetén mindig a szigorúbb olvasat: a feliratos kép
                // elárulná a megoldást, a tanulókártyán pedig a felfedés értelmét veszi el.
                quizUsable = entry.quizUsable == true,
                hideableOnStudyCard = entry.hideableOnStudyCard != false,
            ),
        )
    }

    /** A V006 vizsgálat eredménye: a kizárási csoportok és a hozzá tartozó észrevételek. */
    private data class SynonymResult(
        val groups: List<Set<TechniqueId>>,
        val issues: List<ValidationIssue>,
    )

    /**
     * Felderíti az azonos normalizált nevű rekordokat, és kölcsönös distraktor-kizárási
     * csoportokat képez belőlük (V006, CR-07).
     *
     * A `romaji`, a `hungarian` és az `aliases` **egy névtérben** van: ha két rekord
     * bármelyik neve egybeesik, a kettő összekeverhető, tehát nem lehetnek egymás
     * distraktorai. A csoportok nem záródnak tranzitívan (A~B és B~C nem von össze A~C-t):
     * a kizárás páronkénti tény, a generátor pedig több, egymást átfedő csoportot is
     * kezelhet.
     *
     * A bejárás a rekordok fájlbeli sorrendjét követi, és a csoportokat ugyanilyen
     * sorrendben adja vissza – a `Set` iterációs sorrendjére épülő nemdeterminizmus a
     * kvízgenerátor seedelt reprodukálhatóságát törné el (NFR-07).
     */
    private fun detectSynonyms(
        accepted: List<Technique>,
        entryIndexById: Map<TechniqueId, Int>,
    ): SynonymResult {
        val byName = LinkedHashMap<String, MutableSet<TechniqueId>>()
        accepted.forEach { technique ->
            namesOf(technique).forEach { name ->
                byName.getOrPut(name) { LinkedHashSet() } += technique.id
            }
        }

        val groups = mutableListOf<Set<TechniqueId>>()
        val issues = mutableListOf<ValidationIssue>()
        byName.forEach { (name, ids) ->
            if (ids.size < 2) return@forEach
            val group = ids.toSet()
            if (groups.none { it == group }) groups += group
            val listed = ids.joinToString(", ") { it.value }
            ids.forEach { id ->
                issues += ValidationIssue(
                    code = IssueCode.V006,
                    severity = IssueCode.V006.defaultSeverity,
                    entryId = id.value,
                    entryIndex = entryIndexById[id],
                    field = "name",
                    detail = "A „$name\" normalizált név több rekordon szerepel ($listed); " +
                        "ezek kölcsönös distraktor-kizárási csoportba kerülnek (CR-07).",
                )
            }
        }
        return SynonymResult(groups = groups.toList(), issues = issues.toList())
    }

    /** Egy technika összes összehasonlítható neve normalizált alakban (V006). */
    private fun namesOf(technique: Technique): Set<String> = buildSet {
        add(NameNormalizer.normalize(technique.romaji))
        add(NameNormalizer.normalize(technique.hungarian))
        technique.aliases.forEach { add(NameNormalizer.normalize(it)) }
        remove("")
    }

    private companion object {
        /**
         * A támogatott `schema_version` **major** verziója (ADR-011: a támogatott halmaz
         * `{"1.0.0"}`, és a támogatottság a major verzión dől el).
         */
        const val SUPPORTED_SCHEMA_MAJOR = 1
    }
}
