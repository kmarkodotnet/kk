package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Test
import java.io.File

/**
 * A kvízmotor **invariánsainak** teljes lefedése egyetlen, végigsöpört mátrixon
 * (T-112, TR-02, TR-03, FR-13, FR-14, FR-22, CR-07, NFR-07).
 *
 * A T-108…T-111 készletei egy-egy lépést vizsgálnak a maga szintjén: a szűrést, a
 * distraktorválasztást, a keverést, a készenléti összegzést. Ez a készlet a **kimenetet**
 * vizsgálja: azt állítja, hogy bármelyik értelmes kérésre és bármelyik seeddel előálló
 * terv minden kérdése egyszerre teljesíti a kvízmotor nyolc invariánsát. Ez az a garancia,
 * amire a termék helyessége épül – ha egy jövőbeli optimalizálás a lépések *között*
 * rontana el valamit (például a keverés után választana opciót), a rétegenkénti tesztek
 * zöldek maradhatnának, ez a készlet viszont nem.
 *
 * A [matrix] szándékosan azokat az ágakat járja végig, ahol egy invariáns elromolhat:
 * bőséges és szűk készlet, letiltott típusok, kategóriaszűrő, kép nélküli, feliratos,
 * placeholderes és azonos ujjlenyomatú illusztrációk, átfedő szinonimacsoportok, üres
 * készlet. A seedek a keverés minden ágát mozgatják, a negatív és a szélsőérték-seeddel
 * együtt.
 */
class QuizPlanInvariantTest {
    private val textTypes = setOf(QuestionType.JP_TO_HU, QuestionType.HU_TO_JP)
    private val imageTypes = setOf(QuestionType.IMAGE_TO_JP, QuestionType.JP_TO_IMAGE)

    /** Szöveges típusokra kvízképes készlet egy kategóriában. */
    private fun textPool(count: Int, categoryId: String = "geri"): List<Technique> =
        (1..count).map { index ->
            technique(
                id = "KY-" + index.toString().padStart(4, '0'),
                categoryId = categoryId,
                romaji = "Technique $index",
                hungarian = "Technika $index",
            )
        }

    /** Minden típusra kvízképes készlet: minden rekordnak saját, kvízképes képe van. */
    private fun imagePool(
        count: Int,
        categoryId: String = "stances",
        quizUsable: Boolean = true,
    ): List<Technique> = (1..count).map { index ->
        val id = "KY-" + (200 + index).toString()
        technique(
            id = id,
            categoryId = categoryId,
            romaji = "Image $index",
            hungarian = "Kép $index",
            illustration = asset(ref = id, quizUsable = quizUsable),
        )
    }

    /** Placeholder illusztráció: a rekord látható, de képes kvíztípusba nem kerül (CR-05). */
    private fun placeholderPool(count: Int, categoryId: String = "stances"): List<Technique> =
        (1..count).map { index ->
            technique(
                id = "KY-" + (300 + index).toString(),
                categoryId = categoryId,
                romaji = "Placeholder $index",
                hungarian = "Helyőrző $index",
                illustration = IllustrationRef.Placeholder,
            )
        }

    private fun imageIds(count: Int): List<String> = (1..count).map { "KY-" + (200 + it) }

    /**
     * A végigsöpört kérésmátrix: minden belépés egy seedet kap, és a teljes tervre
     * ugyanazokat az invariánsokat kell teljesítenie.
     */
    private fun matrix(seed: Long): Map<String, QuizRequest> = mapOf(
        "bőséges szöveges készlet" to
            request(techniques = textPool(12), enabledTypes = textTypes, requestedCount = 10, seed = seed),
        "szűk készlet, shortfall" to
            request(techniques = textPool(6), enabledTypes = textTypes, requestedCount = 20, seed = seed),
        "négy alatti készlet" to
            request(techniques = textPool(3), enabledTypes = textTypes, requestedCount = 5, seed = seed),
        "üres jelöltkészlet" to
            request(techniques = emptyList(), requestedCount = 5, seed = seed),
        "minden típus engedélyezve, kép nélkül" to
            request(techniques = textPool(8), requestedCount = 10, seed = seed),
        "egyetlen engedélyezett típus" to
            request(
                techniques = textPool(8),
                enabledTypes = setOf(QuestionType.HU_TO_JP),
                requestedCount = 10,
                seed = seed,
            ),
        "nincs engedélyezett típus" to
            request(techniques = textPool(8), enabledTypes = emptySet(), requestedCount = 5, seed = seed),
        "kategóriaszűrő két kategóriából" to
            request(
                techniques = textPool(6, categoryId = "geri") + imagePool(6, categoryId = "stances"),
                categoryFilter = setOf("stances"),
                requestedCount = 10,
                seed = seed,
            ),
        "kategóriaszűrő: csak szöveges kategória" to
            request(
                techniques = textPool(6, categoryId = "geri") + imagePool(6, categoryId = "stances"),
                categoryFilter = setOf("geri"),
                requestedCount = 10,
                seed = seed,
            ),
        "képes készlet, különböző ujjlenyomat" to
            request(
                techniques = imagePool(6),
                requestedCount = 10,
                seed = seed,
                illustrationDigests = imageIds(6).associateWith { "digest-$it" },
            ),
        "képes készlet, ujjlenyomat nélkül" to
            request(techniques = imagePool(6), requestedCount = 10, seed = seed),
        "minden kép vizuálisan azonos" to
            request(
                techniques = imagePool(6),
                requestedCount = 10,
                seed = seed,
                illustrationDigests = imageIds(6).associateWith { "same-digest" },
            ),
        "részben azonos ujjlenyomatok" to
            request(
                techniques = imagePool(8),
                requestedCount = 10,
                seed = seed,
                illustrationDigests = imageIds(8)
                    .mapIndexed { index, id ->
                        id to if (index < 3) "shared-digest" else "digest-$id"
                    }
                    .toMap(),
            ),
        "feliratos képek (quizUsable = false)" to
            request(techniques = imagePool(6, quizUsable = false), requestedCount = 10, seed = seed),
        "placeholder illusztrációk" to
            request(techniques = placeholderPool(6), requestedCount = 10, seed = seed),
        "szinonimacsoport a készlet felén" to
            request(
                techniques = textPool(8),
                enabledTypes = textTypes,
                requestedCount = 10,
                seed = seed,
                synonymGroups = listOf(setOf("KY-0001", "KY-0002", "KY-0003", "KY-0004")),
            ),
        "átfedő szinonimacsoportok" to
            request(
                techniques = textPool(5),
                enabledTypes = textTypes,
                requestedCount = 5,
                seed = seed,
                synonymGroups = listOf(setOf("KY-0001", "KY-0002"), setOf("KY-0001", "KY-0003")),
            ),
        "szöveges és képes készlet együtt" to
            request(
                techniques = textPool(8, categoryId = "geri") + imagePool(6, categoryId = "stances"),
                requestedCount = 20,
                seed = seed,
                illustrationDigests = imageIds(6).associateWith { "digest-$it" },
            ),
        "pontosan a kért kérdésszám" to
            request(techniques = textPool(7), enabledTypes = textTypes, requestedCount = 7, seed = seed),
        "hiányos rekordok (INCOMPLETE)" to
            request(
                techniques = textPool(8).map { it.copy(status = VerificationStatus.INCOMPLETE) },
                requestedCount = 10,
                seed = seed,
            ),
        "kvízből kizárt és visszavont rekordok" to
            request(
                techniques = textPool(8).mapIndexed { index, item ->
                    item.copy(quizExcluded = index % 2 == 0, retired = index % 2 == 1)
                },
                requestedCount = 10,
                seed = seed,
            ),
        "lektorált tartalom (REVIEWED bizalmi szint)" to
            request(
                techniques = textPool(8).map { it.copy(status = VerificationStatus.VERIFIED) },
                enabledTypes = textTypes,
                requestedCount = 10,
                seed = seed,
                datasetTrust = DatasetTrust.REVIEWED,
            ),
    )

    private val seeds = listOf(1L, 2L, 7L, 42L, -13L, Long.MAX_VALUE, Long.MIN_VALUE)

    // ----------------------------------------------------------------------------
    // 1-8. invariáns a teljes mátrixon (TR-02)
    // ----------------------------------------------------------------------------

    @Test
    fun generate_everyPlanOnEveryBranchSatisfiesAllInvariants_TR02() {
        seeds.forEach { seed ->
            matrix(seed).forEach { (label, subject) ->
                assertQuizPlanInvariants(label, subject, DefaultQuizGenerator.generate(subject))
            }
        }
    }

    // ----------------------------------------------------------------------------
    // 7. invariáns: determinizmus a teljes mátrixon (TR-02, NFR-07)
    // ----------------------------------------------------------------------------

    @Test
    fun generate_sameRequestOnEveryBranch_producesBitIdenticalPlan_TR02() {
        seeds.forEach { seed ->
            matrix(seed).forEach { (_, subject) ->
                val first = DefaultQuizGenerator.generate(subject)
                val second = DefaultQuizGenerator.generate(subject)

                assertThat(first).isEqualTo(second)
                assertThat(first.questions.map { it.optionTechniqueIds })
                    .isEqualTo(second.questions.map { it.optionTechniqueIds })
            }
        }
    }

    /**
     * A terv nem függ a jelöltek **bemeneti sorrendjétől**: a bejárás a `QuizCandidateFilter`
     * rendezett listáin megy, nem `Set`-iterációs sorrenden (NFR-07, ADR-007).
     */
    @Test
    fun generate_candidateInputOrderOnEveryBranch_doesNotChangeThePlan_TR02() {
        matrix(seed = 31L).forEach { (_, subject) ->
            val reversed = subject.copy(
                candidates = subject.candidates.copy(
                    techniques = subject.candidates.techniques.reversed(),
                ),
            )

            assertThat(DefaultQuizGenerator.generate(reversed))
                .isEqualTo(DefaultQuizGenerator.generate(subject))
        }
    }

    /** Különböző seed más kérdéssorrendet ad – a keverés tényleg fut (TR-02). */
    @Test
    fun generate_differentSeedsOnTheSameRequest_produceDifferentOrder_TR02() {
        val plans = seeds.map { seed ->
            DefaultQuizGenerator.generate(
                request(techniques = textPool(16), enabledTypes = textTypes, requestedCount = 16, seed = seed),
            )
        }

        // Ugyanannyi kérdés, de nem ugyanaz a sorrend.
        assertThat(plans.map { it.questionCount }.toSet()).containsExactly(16)
        assertThat(plans.map { plan -> plan.questions.map { it.correctTechniqueId } }.toSet().size)
            .isGreaterThan(1)
    }

    // ----------------------------------------------------------------------------
    // TR-03: kategóriaszűrés és típusjogosultság a kész terven
    // ----------------------------------------------------------------------------

    /**
     * A szűrt kategórián kívüli technika **sem helyesként, sem distraktorként** nem jelenik
     * meg – akkor sem, ha a distraktorfeltöltés más kategóriából pótolna (FR-11, TR-03).
     */
    @Test
    fun generate_filteredCategory_neverLeaksAnOutsideTechniqueIntoAnyOption_TR03() {
        val inside = textPool(6, categoryId = "stances")
        val outside = (1..6).map { index ->
            technique(
                id = "KY-" + (500 + index).toString(),
                categoryId = "punches",
                romaji = "Outside $index",
                hungarian = "Kívül $index",
            )
        }
        val subject = request(
            techniques = inside + outside,
            enabledTypes = textTypes,
            categoryFilter = setOf("stances"),
            requestedCount = 10,
            seed = 17L,
        )

        val plan = DefaultQuizGenerator.generate(subject)
        val outsideIds = outside.mapTo(HashSet()) { it.id }

        assertThat(plan.questionCount).isEqualTo(6)
        plan.questions.forEach { question ->
            assertThat(question.optionTechniqueIds.toSet()).containsNoneIn(outsideIds)
            assertThat(outsideIds).doesNotContain(question.correctTechniqueId)
        }
    }

    /**
     * Ha a szűrt kategórián belül kevesebb mint négy jelölt van, a kérdés **nem jön létre**
     * – a motor nem lép ki a kategóriaszűrőből, hogy opciót találjon (FR-11, FR-14, TR-03).
     */
    @Test
    fun generate_filteredCategoryTooSmall_buildsNoQuestionInsteadOfLeaving_TR03() {
        val subject = request(
            techniques = textPool(3, categoryId = "stances") + textPool(9, categoryId = "geri")
                .map { it.copy(id = TechniqueId.of("KY-9" + it.id.value.takeLast(3))) },
            enabledTypes = textTypes,
            categoryFilter = setOf("stances"),
            requestedCount = 5,
            seed = 19L,
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.canStart).isFalse()
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.ALL_TYPES_SKIPPED)
        assertThat(plan.skippedTypes[QuestionType.JP_TO_HU])
            .isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
    }

    /**
     * Feliratos kép (`quizUsable = false`): a (C)/(D) típus kimarad `NOT_ENOUGH_IMAGES`
     * okkal, a szöveges típusok viszont ugyanabból a készletből összeállnak
     * (CR-05, ADR-011, TR-03).
     */
    @Test
    fun generate_captionedIllustrations_skipImageTypesWithReason_TR03() {
        val subject = request(
            techniques = imagePool(8, quizUsable = false),
            requestedCount = 10,
            seed = 23L,
        )

        val plan = DefaultQuizGenerator.generate(subject)

        imageTypes.forEach { type ->
            assertThat(plan.skippedTypes[type]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        }
        assertThat(plan.questions.map { it.type }.toSet()).containsNoneIn(imageTypes)
        assertThat(plan.questions.map { it.type }.toSet()).isEqualTo(textTypes)
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.NOT_ENOUGH_IMAGES)
    }

    /** Placeholder illusztráció nem validált kép: ugyanaz a kapu zárja (CR-05, TR-03). */
    @Test
    fun generate_placeholderIllustrations_skipImageTypesWithReason_TR03() {
        val subject = request(techniques = placeholderPool(8), requestedCount = 10, seed = 29L)

        val plan = DefaultQuizGenerator.generate(subject)

        imageTypes.forEach { type ->
            assertThat(plan.skippedTypes[type]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        }
        assertThat(plan.questions.map { it.type }.toSet()).containsNoneIn(imageTypes)
    }

    /**
     * Hiányzó kép: a kép nélküli rekord szöveges kérdésben használható, képesben nem –
     * és a képes típus kihagyása nem gyárt képes kérdést (CR-05, TR-03).
     */
    @Test
    fun generate_missingIllustration_keepsTextTypesAndSkipsImageTypes_TR03() {
        val subject = request(techniques = textPool(8), requestedCount = 10, seed = 33L)

        val plan = DefaultQuizGenerator.generate(subject)

        imageTypes.forEach { type ->
            assertThat(plan.skippedTypes[type]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        }
        assertThat(plan.questions).isNotEmpty()
        assertThat(plan.questions.map { it.type }.toSet()).isEqualTo(textTypes)
    }

    /**
     * A (C) `IMAGE_TO_JP` kérdés képe a **helyes** válaszé: minden ilyen kérdésnél a helyes
     * válasznak van kvízképes képe, a distraktoroknak viszont nem kell (ADR-003, TR-03).
     */
    @Test
    fun generate_imageToJp_correctAnswerAlwaysHasQuizUsableImage_TR03() {
        val subject = request(
            techniques = imagePool(5, categoryId = "stances") + textPool(5, categoryId = "stances"),
            enabledTypes = setOf(QuestionType.IMAGE_TO_JP),
            requestedCount = 5,
            seed = 37L,
        )

        val plan = DefaultQuizGenerator.generate(subject)
        val byId = subject.candidates.techniques.associateBy { it.id }

        assertThat(plan.questions).isNotEmpty()
        plan.questions.forEach { question ->
            val correct = requireNotNull(byId[question.correctTechniqueId])
            assertThat((correct.illustration as IllustrationRef.Asset).quizUsable).isTrue()
            // A distraktorok romaji nevek: kép nélküli rekord is lehet köztük.
            assertThat(question.distractorTechniqueIds).hasSize(3)
        }
        // A kép nélküli rekordok tényleg opcióként szerepelnek valahol – a (C) típus
        // opciói nevek, nem képek.
        val textIds = textPool(5, categoryId = "stances").mapTo(HashSet()) { it.id }
        assertThat(plan.questions.flatMap { it.optionTechniqueIds }.toSet().any { it in textIds })
            .isTrue()
    }

    /**
     * A (D) `JP_TO_IMAGE` **minden** opciójának kvízképes, vizuálisan különböző képe van
     * (FR-14, TR-02, TR-03).
     */
    @Test
    fun generate_jpToImage_everyOptionHasADistinctQuizUsableImage_TR03() {
        val subject = request(
            techniques = imagePool(6, categoryId = "stances") + textPool(6, categoryId = "stances"),
            enabledTypes = setOf(QuestionType.JP_TO_IMAGE),
            requestedCount = 6,
            seed = 41L,
            illustrationDigests = imageIds(6).associateWith { "digest-$it" },
        )

        val plan = DefaultQuizGenerator.generate(subject)
        val byId = subject.candidates.techniques.associateBy { it.id }

        assertThat(plan.questions).isNotEmpty()
        plan.questions.forEach { question ->
            val digests = question.optionTechniqueIds.map { id ->
                val illustration = byId.getValue(id).illustration
                assertThat(illustration).isInstanceOf(IllustrationRef.Asset::class.java)
                assertThat((illustration as IllustrationRef.Asset).quizUsable).isTrue()
                requireNotNull(subject.candidates.illustrationDigestOf(id))
            }
            assertThat(digests.toSet()).hasSize(QuizQuestion.OPTION_COUNT)
        }
    }

    /**
     * Négy kvízképes kép közül kettő vizuálisan azonos: a (D) típus kimarad képhiánnyal,
     * mert a felhasználó nem négy valódi opciót látna (FR-14, TR-02).
     */
    @Test
    fun generate_jpToImage_visuallyIdenticalPairLeavesTooFewOptions_TR02() {
        val ids = imageIds(4)
        val subject = request(
            techniques = imagePool(4, categoryId = "stances"),
            enabledTypes = setOf(QuestionType.JP_TO_IMAGE),
            requestedCount = 4,
            seed = 43L,
            illustrationDigests = mapOf(
                ids[0] to "shared",
                ids[1] to "shared",
                ids[2] to "digest-c",
                ids[3] to "digest-d",
            ),
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.canStart).isFalse()
        assertThat(plan.skippedTypes[QuestionType.JP_TO_IMAGE])
            .isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.ALL_TYPES_SKIPPED)
    }

    /**
     * A jogosultsági kapu lefelé zár: `INCOMPLETE`, `quizExcluded` és `retired` rekordból
     * egyetlen kérdés sem lesz, akkor sem, ha egyébként kvízképes képe van – és a tartalmi
     * állapotot a motor **soha nem emeli** (ADR-010, CR-02, TR-03).
     */
    @Test
    fun generate_gateBlockedRecords_buildNoQuestionAtAll_TR03() {
        val subject = request(
            techniques = imagePool(4).map { it.copy(status = VerificationStatus.INCOMPLETE) } +
                imagePool(3, categoryId = "geri").mapIndexed { index, item ->
                    item.copy(
                        id = TechniqueId.of("KY-8" + (index + 1).toString().padStart(3, '0')),
                        quizExcluded = true,
                    )
                } +
                placeholderPool(3).map { it.copy(retired = true) },
            requestedCount = 10,
            seed = 47L,
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.canStart).isFalse()
        assertThat(plan.questions).isEmpty()
        assertThat(plan.skippedTypes.keys).containsExactlyElementsIn(QuestionType.entries)
    }

    /**
     * A hitelesített és a lektorálatlan rekord ugyanabban a tervben: a
     * `PRACTICE_ON_UNREVIEWED` szint mellett a kapu nem emeli és nem süllyeszti a rekord
     * állapotát – a lektorálatlan rekord is kvízképes, ha a tartalom bizalmi szintje ezt
     * engedi (ADR-010, CR-02).
     */
    @Test
    fun generate_mixedVerificationStatus_followsTheTrustGateOnly_TR03() {
        val reviewed = textPool(4).map { it.copy(status = VerificationStatus.VERIFIED) }
        val awaiting = textPool(4, categoryId = "geri").mapIndexed { index, item ->
            item.copy(
                id = TechniqueId.of("KY-07" + (index + 1).toString().padStart(2, '0')),
                status = VerificationStatus.AWAITING_REVIEW,
            )
        }
        val subject = request(
            techniques = reviewed + awaiting,
            enabledTypes = textTypes,
            requestedCount = 8,
            seed = 53L,
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.questionCount).isEqualTo(8)
        assertThat(plan.questions.map { it.correctTechniqueId }.toSet()).hasSize(8)
    }

    // ----------------------------------------------------------------------------
    // Fallback: shortfall okok prioritása (FR-22, TR-03)
    // ----------------------------------------------------------------------------

    /**
     * Nulla jelölt: üres, de **megindokolt** terv – nincs kivétel, nincs félkész kérdés
     * (FR-22, TR-02).
     */
    @Test
    fun generate_zeroCandidates_returnsEmptyExplainedPlan_TR02() {
        val subject = request(techniques = emptyList(), requestedCount = 5, seed = 59L)

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.questions).isEmpty()
        assertThat(plan.canStart).isFalse()
        assertThat(plan.shortfall)
            .isEqualTo(Shortfall(requestedCount = 5, actualCount = 0, reason = ShortfallReason.ALL_TYPES_SKIPPED))
        assertThat(plan.skippedTypes.keys).containsExactlyElementsIn(QuestionType.entries)
    }

    /**
     * Az okok prioritási sorrendje motordöntés, és ezért rögzítve van:
     * `ALL_TYPES_SKIPPED` → `NOT_ENOUGH_IMAGES` → `NOT_ENOUGH_DISTRACTORS` →
     * `NOT_ENOUGH_CANDIDATES`. A sorrend a leghasznosabb magyar üzenet felé rendez: a
     * teljes típus kiesése nagyobb és konkrétabban orvosolható veszteség, mint néhány
     * kimaradt kérdés (FR-22, TR-03).
     */
    @Test
    fun generate_shortfallReason_followsTheDocumentedPriority_TR03() {
        // 1. Egyetlen kérdés sem jött össze → ALL_TYPES_SKIPPED, minden más ok előtt.
        val allSkipped = request(
            techniques = textPool(3),
            enabledTypes = textTypes,
            requestedCount = 5,
            seed = 61L,
        )
        assertThat(DefaultQuizGenerator.generate(allSkipped).shortfall?.reason)
            .isEqualTo(ShortfallReason.ALL_TYPES_SKIPPED)

        // 2. Van kérdés, de egy engedélyezett képes típus egésze kiesett → NOT_ENOUGH_IMAGES.
        val imagesMissing = request(
            techniques = textPool(6),
            requestedCount = 20,
            seed = 62L,
        )
        assertThat(DefaultQuizGenerator.generate(imagesMissing).shortfall?.reason)
            .isEqualTo(ShortfallReason.NOT_ENOUGH_IMAGES)

        // 3. Minden típus összeáll, de volt olyan helyes jelölt, amely mellé nem jött ki
        // három elfogadható distraktor (átfedő szinonimacsoportok) → NOT_ENOUGH_DISTRACTORS.
        val distractorShortage = request(
            techniques = textPool(5),
            enabledTypes = textTypes,
            requestedCount = 5,
            seed = 63L,
            synonymGroups = listOf(setOf("KY-0001", "KY-0002"), setOf("KY-0001", "KY-0003")),
        )
        val distractorPlan = DefaultQuizGenerator.generate(distractorShortage)
        assertThat(distractorPlan.questions).isNotEmpty()
        assertThat(
            QuizCandidateFilter.correctAnswerPool(distractorShortage, QuestionType.JP_TO_HU).size,
        ).isGreaterThan(
            DistractorSelector.buildableCorrectAnswers(distractorShortage, QuestionType.JP_TO_HU).size,
        )
        assertThat(distractorPlan.shortfall?.reason)
            .isEqualTo(ShortfallReason.NOT_ENOUGH_DISTRACTORS)

        // 4. Egyébként egyszerűen kevesebb kvízképes technika volt → NOT_ENOUGH_CANDIDATES.
        val candidateShortage = request(
            techniques = textPool(6),
            enabledTypes = textTypes,
            requestedCount = 20,
            seed = 64L,
        )
        assertThat(DefaultQuizGenerator.generate(candidateShortage).shortfall?.reason)
            .isEqualTo(ShortfallReason.NOT_ENOUGH_CANDIDATES)
    }

    /**
     * A kevesebb kérdés **nem** hibás kérdés: a shortfallos terv minden kérdése ugyanazokat
     * az invariánsokat teljesíti, mint a teljes terv (FR-22, TR-02).
     */
    @Test
    fun generate_shortfallPlan_stillSatisfiesEveryQuestionInvariant_TR02() {
        val subject = request(
            techniques = textPool(5),
            enabledTypes = textTypes,
            requestedCount = 20,
            seed = 67L,
            synonymGroups = listOf(setOf("KY-0001", "KY-0002"), setOf("KY-0001", "KY-0003")),
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.shortfall).isNotNull()
        assertQuizPlanInvariants("shortfallos terv", subject, plan)
    }

    // ----------------------------------------------------------------------------
    // dryRun ↔ generate: egy közös ponton (T-111, FR-13, FR-22, TR-03)
    // ----------------------------------------------------------------------------

    /**
     * A készenléti összegzés és a terv a **teljes mátrixon, minden seeddel** egyezik: a
     * kérdésszám, a kihagyási okok és a shortfall is (FR-13, FR-22, TR-03).
     */
    @Test
    fun dryRun_matchesGenerateOnTheWholeMatrix_TR03() {
        seeds.forEach { seed ->
            matrix(seed).forEach { (label, subject) ->
                val plan = DefaultQuizGenerator.generate(subject)
                val readiness = DefaultQuizGenerator.dryRun(subject)

                assertThat(readiness.expectedQuestionCount).isEqualTo(plan.questionCount)
                assertThat(readiness.skippedTypes).isEqualTo(plan.skippedTypes)
                assertThat(readiness.expectedShortfall).isEqualTo(plan.shortfall)
                assertThat(readiness.canStart).isEqualTo(plan.canStart)
                assertThat(readiness.requestedCount).isEqualTo(subject.requestedCount)
                // A terv minden felhasznált típusa kapacitással bír az összegzésben, és az
                // értéke sosem kisebb, mint ahány kérdést a terv abból a típusból adott.
                plan.questions.groupingBy { it.type }.eachCount().forEach { (type, used) ->
                    assertThat(readiness.availableByType[type]).isAtLeast(used)
                }
                assertThat(label).isNotEmpty()
            }
        }
    }

    /**
     * A két ág nem csak egyező eredményt ad, hanem **ugyanazon a függvényen** megy át: a
     * `generate` és a `dryRun` törzse is a `questionSlots(...)` közös pontot hívja, és
     * egyik sem fogalmazza meg újra a szűrést. Ez az, ami miatt a két ág nem tud később
     * elválni egymástól (T-111, T-112).
     */
    @Test
    fun dryRunAndGenerate_shareTheSameQuestionSlotsFunction_TR03() {
        val source = File(
            "src/main/kotlin/hu/kyokushin/trainer/core/quiz/generator/DefaultQuizGenerator.kt",
        )
        assertThat(source.exists()).isTrue()
        val text = source.readText()

        val generateBody = text
            .substringAfter("override fun generate(request: QuizRequest): QuizPlan {")
            .substringBefore("override fun dryRun(")
        val dryRunBody = text
            .substringAfter("override fun dryRun(request: QuizRequest): QuizReadiness {")
            .substringBefore("private fun questionSlots(")

        listOf(generateBody, dryRunBody).forEach { body ->
            assertThat(body).contains("questionSlots(request, skippedTypes)")
            assertThat(body).contains("QuizCandidateFilter.skipReasons(request)")
        }
        // A `questionSlots` az egyetlen hely, ahol a felhasználható helyes válaszok
        // listája összeáll – a `dryRun` nem számol belőle másodszor.
        assertThat(dryRunBody).doesNotContain("buildableCorrectAnswers")
    }

    /**
     * A készenléti összegzés `availableByType` értéke **kapacitás**, nem kérdéskiosztás: az
     * értékek összege nagyobb lehet a várható kérdésszámnál, mert ugyanaz a technika több
     * típusban is helyes válasz lehet, a motor viszont egyszer használja (T-111, TR-03).
     */
    @Test
    fun dryRun_availableByTypeIsCapacityNotAllocation_TR03() {
        val subject = request(
            techniques = imagePool(8, categoryId = "stances"),
            requestedCount = 20,
            seed = 71L,
            illustrationDigests = imageIds(8).associateWith { "digest-$it" },
        )

        val readiness = DefaultQuizGenerator.dryRun(subject)
        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(readiness.availableByType.keys).containsExactlyElementsIn(QuestionType.entries)
        assertThat(readiness.availableByType.values.toSet()).containsExactly(8)
        assertThat(readiness.expectedQuestionCount).isEqualTo(8)
        assertThat(readiness.availableByType.values.sum())
            .isGreaterThan(readiness.expectedQuestionCount)
        assertThat(plan.questionCount).isEqualTo(readiness.expectedQuestionCount)
    }
}
