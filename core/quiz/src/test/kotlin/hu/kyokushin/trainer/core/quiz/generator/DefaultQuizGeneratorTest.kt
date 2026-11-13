package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import org.junit.Test
import java.io.File

/**
 * A [DefaultQuizGenerator] seedelt determinizmusa és a terv összeállítása
 * (T-110, TR-02, NFR-07, FR-14, FR-22, ADR-007).
 *
 * Amit ez a készlet bizonyít:
 *
 * 1. ugyanaz a kérés + seed → **bitre** ugyanaz a terv, az opciók sorrendjével együtt,
 * 2. a keverés **csak sorrendet** változtat: az opciók halmaza a [DistractorSelector]
 *    kiválasztása, nem a keverés terméke,
 * 3. a keverés tényleg fut: a helyes válasz nem mindig az első opció, és más seed más
 *    kérdéssorrendet ad,
 * 4. ugyanaz a helyes technika nem ismétlődik egy terven belül, és a kértnél kevesebb
 *    kérdés [Shortfall]-t kap ismétlés helyett,
 * 5. a modulban egyetlen `Random.Default` / rendszeróra-hívás sincs.
 *
 * A kiválasztás szabályait (szinonimakizárás, képazonosság, jogosultsági kapu) a T-108 és
 * a T-109 készlete fedi; itt szándékosan nem ismételjük meg őket.
 */
class DefaultQuizGeneratorTest {
    /** Egy kategóriában ennyi, szöveges típusokra kvízképes technika. */
    private fun pool(count: Int, categoryId: String = "geri"): List<Technique> =
        (1..count).map { index ->
            technique(
                id = "KY-" + index.toString().padStart(4, '0'),
                categoryId = categoryId,
                romaji = "Technique $index",
                hungarian = "Technika $index",
            )
        }

    private val textTypes = setOf(QuestionType.JP_TO_HU, QuestionType.HU_TO_JP)

    // ----------------------------------------------------------------------------
    // Determinizmus (NFR-07, ADR-007, TR-02)
    // ----------------------------------------------------------------------------

    @Test
    fun generate_sameRequestAndSeedTwice_producesIdenticalPlan_NFR07() {
        val subject = request(techniques = pool(12), enabledTypes = textTypes, requestedCount = 10, seed = 7L)

        val first = DefaultQuizGenerator.generate(subject)
        val second = DefaultQuizGenerator.generate(subject)

        assertThat(first).isEqualTo(second)
        // A mezőegyenlőségen túl kimondjuk azt is, amit a `SavedStateHandle`
        // visszaállítása használ: az opciók **sorrendje** is azonos (T-405).
        assertThat(first.questions.map { it.optionTechniqueIds })
            .isEqualTo(second.questions.map { it.optionTechniqueIds })
        assertThat(first.questions.map { it.correctOptionIndex })
            .isEqualTo(second.questions.map { it.correctOptionIndex })
    }

    /**
     * Két külön példányban felépített, de tartalmilag azonos kérés is ugyanazt a tervet
     * adja: a determinizmus a kérés **értékén** áll, nem objektumidentitáson.
     */
    @Test
    fun generate_equalRequestsBuiltSeparately_produceIdenticalPlan_NFR07() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(9), enabledTypes = textTypes, requestedCount = 5, seed = 2024L),
        )
        val replayed = DefaultQuizGenerator.generate(
            request(techniques = pool(9), enabledTypes = textTypes, requestedCount = 5, seed = 2024L),
        )

        assertThat(plan).isEqualTo(replayed)
    }

    /**
     * A jelöltek sorrendje a hívó listájában nem számít: a bejárás a
     * [QuizCandidateFilter] azonosító szerint rendezett kimenetéből megy (ADR-007).
     */
    @Test
    fun generate_candidateInputOrder_doesNotChangeThePlan_NFR07() {
        val techniques = pool(8)
        val ascending = request(techniques = techniques, enabledTypes = textTypes, requestedCount = 6, seed = 11L)
        val reversed = request(
            techniques = techniques.reversed(),
            enabledTypes = textTypes,
            requestedCount = 6,
            seed = 11L,
        )

        assertThat(DefaultQuizGenerator.generate(ascending))
            .isEqualTo(DefaultQuizGenerator.generate(reversed))
    }

    @Test
    fun generate_differentSeeds_produceDifferentQuestionOrder_NFR07() {
        val orders = (1L..20L).map { seed ->
            DefaultQuizGenerator
                .generate(request(techniques = pool(12), enabledTypes = textTypes, requestedCount = 8, seed = seed))
                .questions
                .map { it.correctTechniqueId.value }
        }

        // Ha a seed nem hatna, mind a húsz sorrend azonos lenne – a keverés tehát nem fut.
        assertThat(orders.toSet().size).isGreaterThan(1)
    }

    /** A modulban nincs seed nélküli véletlen és nincs rendszeróra (NFR-07, ADR-007). */
    @Test
    fun quizModuleMainSources_containNoUnseededRandomOrClock_NFR07() {
        val repoRoot = File(requireNotNull(System.getProperty("kyo.repoRoot")) { "kyo.repoRoot" })
        val mainSources = File(repoRoot, "core/quiz/src/main/kotlin")
        val forbidden = listOf(
            "Random.Default",
            "Random.nextInt(",
            "System.currentTimeMillis",
            "System.nanoTime",
            "Instant.now",
            "LocalDate.now",
            "LocalDateTime.now",
        )

        val sources = mainSources.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertThat(sources).isNotEmpty()

        // A tiltott mintákat a **kódban** keressük: a KDoc és a soros megjegyzés épp
        // ezekről a szabályokról beszél, azt nem szabad szabálysértésnek venni.
        val codeByFile = sources.associate { it.name to withoutComments(it.readText()) }

        val offenders = codeByFile.flatMap { (name, code) ->
            forbidden.filter { it in code }.map { "$name: $it" }
        }
        assertThat(offenders).isEmpty()

        // A keverés egyetlen belépési pontja a `SeededShuffle`; paraméter nélküli
        // `shuffled()` hívás a seedet csendben kikerülné.
        val unseededShuffles = codeByFile.filterValues { "shuffled()" in it }.keys
        assertThat(unseededShuffles).isEmpty()

        // Az ellenőrzés csak akkor jelent valamit, ha tényleg talál kódot: egy üres vagy
        // csak megjegyzésekből álló olvasat hamisan zöld lenne.
        assertThat(codeByFile["DefaultQuizGenerator.kt"]).contains("Random(request.seed)")
    }

    /** A Kotlin forrás megjegyzések nélkül – blokk- és soros megjegyzés is kiesik. */
    private fun withoutComments(source: String): String = source
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        .lines()
        .joinToString("\n") { line -> line.substringBefore("//") }

    // ----------------------------------------------------------------------------
    // A keverés csak permutál (FR-14, TR-02)
    // ----------------------------------------------------------------------------

    @Test
    fun generate_optionShuffle_onlyReordersTheSelectedOptions_FR14() {
        val subject = request(techniques = pool(10), enabledTypes = textTypes, requestedCount = 10, seed = 5L)
        val techniquesById = subject.candidates.techniques.associateBy { it.id }

        DefaultQuizGenerator.generate(subject).questions.forEach { question ->
            val canonical = requireNotNull(
                DistractorSelector.selectOptionIds(
                    subject,
                    question.type,
                    requireNotNull(techniquesById[question.correctTechniqueId]),
                ),
            )
            assertThat(question.optionTechniqueIds).hasSize(QuizQuestion.OPTION_COUNT)
            assertThat(question.optionTechniqueIds.toSet()).isEqualTo(canonical.toSet())
            assertThat(question.optionTechniqueIds).contains(question.correctTechniqueId)
        }
    }

    @Test
    fun generate_correctAnswerPosition_isShuffledNotAlwaysFirst_TR02() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(16), enabledTypes = textTypes, requestedCount = 16, seed = 99L),
        )

        assertThat(plan.questionCount).isEqualTo(16)
        // A kanonikus sorrend a helyes választ az első helyre teszi; ha a keverés nem
        // futna, minden kérdésben a 0. index lenne a helyes.
        assertThat(plan.questions.map { it.correctOptionIndex }.toSet().size).isGreaterThan(1)
    }

    // ----------------------------------------------------------------------------
    // Nincs ismétlés, van indoklás (FR-22, TR-02)
    // ----------------------------------------------------------------------------

    @Test
    fun generate_neverRepeatsTheSameCorrectTechnique_TR02() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(20), enabledTypes = textTypes, requestedCount = 20, seed = 3L),
        )

        val correctIds = plan.questions.map { it.correctTechniqueId }
        assertThat(correctIds).hasSize(20)
        assertThat(correctIds.toSet()).hasSize(20)
    }

    @Test
    fun generate_fewerCandidatesThanRequested_yieldsShortfallInsteadOfRepetition_FR22() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(6), enabledTypes = textTypes, requestedCount = 20, seed = 8L),
        )

        assertThat(plan.questionCount).isEqualTo(6)
        assertThat(plan.questions.map { it.correctTechniqueId }.toSet()).hasSize(6)
        assertThat(plan.shortfall).isEqualTo(
            Shortfall(
                requestedCount = 20,
                actualCount = 6,
                reason = ShortfallReason.NOT_ENOUGH_CANDIDATES,
            ),
        )
    }

    @Test
    fun generate_exactlyEnoughCandidates_hasNoShortfall_FR13() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(10), enabledTypes = textTypes, requestedCount = 10, seed = 4L),
        )

        assertThat(plan.questionCount).isEqualTo(10)
        assertThat(plan.shortfall).isNull()
        assertThat(plan.canStart).isTrue()
    }

    @Test
    fun generate_noUsableCandidate_returnsExplainedEmptyPlan_FR22() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(3), enabledTypes = textTypes, requestedCount = 5, seed = 1L),
        )

        assertThat(plan.canStart).isFalse()
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.ALL_TYPES_SKIPPED)
        assertThat(plan.skippedTypes[QuestionType.JP_TO_HU]).isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
        assertThat(plan.skippedTypes[QuestionType.IMAGE_TO_JP]).isEqualTo(SkipReason.TYPE_DISABLED)
    }

    @Test
    fun generate_imageTypesWithoutIllustrations_reportSkipReasonAndShortfall_TR03() {
        val plan = DefaultQuizGenerator.generate(
            request(
                techniques = pool(6),
                enabledTypes = QuestionType.entries.toSet(),
                requestedCount = 10,
                seed = 6L,
            ),
        )

        assertThat(plan.skippedTypes[QuestionType.IMAGE_TO_JP]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        assertThat(plan.skippedTypes[QuestionType.JP_TO_IMAGE]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        assertThat(plan.skippedTypes).doesNotContainKey(QuestionType.JP_TO_HU)
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.NOT_ENOUGH_IMAGES)
        // A képes típus kiesése nem gyárt képes kérdést és nem is dob kivételt.
        assertThat(plan.questions.map { it.type }.toSet()).isEqualTo(textTypes)
    }

    @Test
    fun generate_questionIds_areUniqueAndFollowPlayOrder_ADR005() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(12), enabledTypes = textTypes, requestedCount = 12, seed = 13L),
        )

        val ids = plan.questions.map { it.id.value }
        assertThat(ids).hasSize(12)
        assertThat(ids.toSet()).hasSize(12)
        assertThat(ids).isInOrder()
        assertThat(ids.first()).isEqualTo("q-01")
    }

    /**
     * A kérdéstípusok keverednek a terven belül: a sorsolás egysége a
     * (típus, helyes válasz) pár, nem a típus – a felhasználó nem típusonként tömbösített
     * kvízt kap (FR-12).
     */
    @Test
    fun generate_mixesEnabledQuestionTypesWithinOnePlan_FR12() {
        val plan = DefaultQuizGenerator.generate(
            request(techniques = pool(12), enabledTypes = textTypes, requestedCount = 12, seed = 21L),
        )

        assertThat(plan.questions.map { it.type }.toSet()).isEqualTo(textTypes)
    }

    // ----------------------------------------------------------------------------
    // A dryRun ígérete nem térhet el a tervtől (FR-13, T-111 előkészítés)
    // ----------------------------------------------------------------------------

    @Test
    fun dryRun_promisesExactlyWhatGenerateProduces_FR13() {
        listOf(
            request(techniques = pool(12), enabledTypes = textTypes, requestedCount = 10, seed = 1L),
            request(techniques = pool(6), enabledTypes = textTypes, requestedCount = 20, seed = 2L),
            request(techniques = pool(3), enabledTypes = textTypes, requestedCount = 5, seed = 3L),
            request(
                techniques = pool(8),
                enabledTypes = QuestionType.entries.toSet(),
                requestedCount = 10,
                seed = 4L,
            ),
        ).forEach { subject ->
            val plan = DefaultQuizGenerator.generate(subject)
            val readiness = DefaultQuizGenerator.dryRun(subject)

            assertThat(readiness.expectedQuestionCount).isEqualTo(plan.questionCount)
            assertThat(readiness.skippedTypes).isEqualTo(plan.skippedTypes)
            assertThat(readiness.expectedShortfall).isEqualTo(plan.shortfall)
            assertThat(readiness.canStart).isEqualTo(plan.canStart)
        }
    }

    // ----------------------------------------------------------------------------
    // dryRun: kérdésépítés nélküli készenléti összegzés (T-111, FR-13, FR-22)
    // ----------------------------------------------------------------------------

    /** Képes típusokra is kvízképes készlet: minden technikának saját illusztrációja van. */
    private fun imagePool(count: Int, categoryId: String = "uke"): List<Technique> =
        (1..count).map { index ->
            val id = "KY-" + (100 + index).toString()
            technique(
                id = id,
                categoryId = categoryId,
                romaji = "Image $index",
                hungarian = "Kép $index",
                illustration = asset(ref = id),
            )
        }

    /**
     * A készenléti összegzés és a terv **minden** jellemző kérésre egyezik (FR-13, FR-22).
     *
     * A mátrix szándékosan végigmegy azokon az ágakon, ahol a két számolás elválhatna:
     * bőséges és szűk készlet, letiltott típusok, kategóriaszűrő, képes típusok,
     * szinonimakizárás (CR-07), azonos képujjlenyomatok (D típus) és több seed.
     */
    @Test
    fun dryRun_matchesGenerateOnEveryBranch_FR13() {
        val requests = listOf(
            // Bőséges szöveges készlet – a kért kérdésszám kijön.
            request(techniques = pool(12), enabledTypes = textTypes, requestedCount = 10, seed = 1L),
            // Szűk készlet – shortfall.
            request(techniques = pool(6), enabledTypes = textTypes, requestedCount = 20, seed = 2L),
            // Négy alatti készlet – egyetlen kérdés sem áll össze.
            request(techniques = pool(3), enabledTypes = textTypes, requestedCount = 5, seed = 3L),
            // Minden típus engedélyezve, de kép nélküli készlet.
            request(techniques = pool(8), requestedCount = 10, seed = 4L),
            // Egyetlen engedélyezett típus.
            request(
                techniques = pool(8),
                enabledTypes = setOf(QuestionType.HU_TO_JP),
                requestedCount = 10,
                seed = 5L,
            ),
            // Egyetlen típus sincs engedélyezve – minden típus kihagyva.
            request(techniques = pool(8), enabledTypes = emptySet(), requestedCount = 5, seed = 6L),
            // Kategóriaszűrő: a kijelölt kategóriában kevesebb jelölt van.
            request(
                techniques = pool(5, categoryId = "geri") + imagePool(5, categoryId = "tsuki"),
                enabledTypes = textTypes,
                categoryFilter = setOf("tsuki"),
                requestedCount = 10,
                seed = 7L,
            ),
            // Képes típusok saját, különböző illusztrációkkal.
            request(techniques = imagePool(6), requestedCount = 10, seed = 8L),
            // Képes típusok, de minden kép vizuálisan azonos – a (D) típus kiesik.
            request(
                techniques = imagePool(6),
                requestedCount = 10,
                seed = 9L,
                illustrationDigests = (1..6).associate { "KY-" + (100 + it) to "same-digest" },
            ),
            // Szinonimakizárás: a készlet fele egyetlen szinonimacsoport (CR-07).
            request(
                techniques = pool(8),
                enabledTypes = textTypes,
                requestedCount = 10,
                seed = 10L,
                synonymGroups = listOf(setOf("KY-0001", "KY-0002", "KY-0003", "KY-0004")),
            ),
            // Pontosan a kért kérdésszám határán.
            request(techniques = pool(7), enabledTypes = textTypes, requestedCount = 7, seed = 11L),
        )

        requests.forEach { subject ->
            val plan = DefaultQuizGenerator.generate(subject)
            val readiness = DefaultQuizGenerator.dryRun(subject)

            assertThat(readiness.expectedQuestionCount).isEqualTo(plan.questionCount)
            assertThat(readiness.skippedTypes).isEqualTo(plan.skippedTypes)
            assertThat(readiness.expectedShortfall).isEqualTo(plan.shortfall)
            assertThat(readiness.canStart).isEqualTo(plan.canStart)
            assertThat(readiness.requestedCount).isEqualTo(subject.requestedCount)
        }
    }

    /**
     * A készenléti összegzés nem a seedtől függ: a `dryRun` nem sorsol és nem kever, ezért
     * bármely seeddel ugyanazt mondja – miközben a `generate` is ugyanannyi kérdést ad
     * (NFR-07, FR-13).
     */
    @Test
    fun dryRun_isSeedIndependentWhileGenerateCountStays_FR13() {
        val seeds = listOf(1L, 2L, 77L, -5L, Long.MAX_VALUE)
        val subjects = seeds.map { seed ->
            request(techniques = pool(7), enabledTypes = textTypes, requestedCount = 10, seed = seed)
        }

        val readinesses = subjects.map(DefaultQuizGenerator::dryRun)

        assertThat(readinesses.toSet()).hasSize(1)
        subjects.forEach { subject ->
            assertThat(DefaultQuizGenerator.generate(subject).questionCount)
                .isEqualTo(readinesses.first().expectedQuestionCount)
        }
    }

    /**
     * Az `availableByType` típusonkénti **kapacitás**, nem a terv felosztása (T-111).
     *
     * Ugyanaz a technika két szöveges típusban is helyes válasz lehet, de a motor egy
     * kvízen belül legfeljebb egyszer használja – ezért az értékek összege nagyobb, mint a
     * várható kérdésszám, és a kulcsok nem fedik a kihagyott típusokat.
     */
    @Test
    fun dryRun_availableByTypeIsPerTypeCapacity_FR13() {
        val subject = request(
            techniques = pool(6),
            enabledTypes = textTypes,
            requestedCount = 20,
            seed = 12L,
        )

        val readiness = DefaultQuizGenerator.dryRun(subject)

        assertThat(readiness.availableByType).isEqualTo(
            mapOf(QuestionType.JP_TO_HU to 6, QuestionType.HU_TO_JP to 6),
        )
        assertThat(readiness.expectedQuestionCount).isEqualTo(6)
        assertThat(readiness.availableByType.values.sum())
            .isGreaterThan(readiness.expectedQuestionCount)
        assertThat(readiness.skippedTypes.keys).containsNoneIn(readiness.availableByType.keys)
    }

    /**
     * A kapacitás minden típusnál a jelöltszűrő pontos számával egyezik, a kihagyott
     * típusok pedig kimaradnak belőle (FR-22).
     */
    @Test
    fun dryRun_availableByTypeAgreesWithCandidateFilter_FR22() {
        val subject = request(techniques = imagePool(6), requestedCount = 10, seed = 13L)

        val readiness = DefaultQuizGenerator.dryRun(subject)

        QuestionType.entries.forEach { type ->
            val buildable = QuizCandidateFilter.buildableQuestionCount(subject, type)
            if (type in readiness.skippedTypes) {
                assertThat(buildable).isEqualTo(0)
                assertThat(readiness.availableByType).doesNotContainKey(type)
            } else {
                assertThat(readiness.availableByType[type]).isEqualTo(buildable)
            }
        }
    }

    /**
     * A `dryRun` ága **nem** épít kérdést és nem sorsol (T-111).
     *
     * Ugyanaz a forrásellenőrzéses minta, mint az NFR-07 `Random.Default`-tesztnél: a
     * `dryRun` törzsében nincs `generate(` hívás, nincs `QuizQuestion`, nincs `Random` és
     * nincs keverés – a beállító képernyő élő összegzése így minden kapcsolóváltásnál
     * csak a szűrést és a jogosultsági kaput futtatja.
     */
    @Test
    fun dryRun_bodyBuildsNoQuestionAndDrawsNoRandom_FR13() {
        val source = File(
            "src/main/kotlin/hu/kyokushin/trainer/core/quiz/generator/DefaultQuizGenerator.kt",
        )
        assertThat(source.exists()).isTrue()

        val body = source.readText()
            .substringAfter("override fun dryRun(request: QuizRequest): QuizReadiness {")
            .substringBefore("private fun questionSlots(")

        assertThat(body).contains("questionSlots(request, skippedTypes)")
        listOf("generate(", "QuizQuestion", "Random", "SeededShuffle").forEach { forbidden ->
            assertThat(body).doesNotContain(forbidden)
        }
    }
}
