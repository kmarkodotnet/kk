package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.RealKnowledgeBase
import org.junit.Test

/**
 * A **teljes kvízgenerálás** a valódi tartalmon (T-112, TR-02, TR-03, FR-11…FR-14, FR-22).
 *
 * A T-108 valós tartalmi készlete (`RealContentCandidateFilterTest`) a *szűrésig* jut el:
 * azt bizonyítja, hogy a képes típusokhoz csak az `stances` kategóriában van hét kvízképes
 * illusztráció. Ez a készlet onnan folytatja: tényleges terveket generál a
 * `knowledgebase_transfer/` 161 rekordjából, és azt állítja, hogy
 *
 * 1. az `stances` kategória **tényleg ad** (C) és (D) kérdést, a többi kategória nem – és
 *    nem is hallgatólagosan, hanem [SkipReason.NOT_ENOUGH_IMAGES] indoklással,
 * 2. a valódi tartalomra generált minden kérdés teljesíti a kvízmotor nyolc invariánsát –
 *    a szinonimacsoportok a validátor tényleges V006 kimenetéből jönnek, nem tesztadatból
 *    (a mostani tartalomban ez üres halmaz, és a teszt ezt a tényt is kimondja),
 * 3. a `dryRun` ígérete a valódi tartalmon is egyezik a tervvel.
 *
 * Miért kell ez a szintetikus mátrix mellé? Mert a szintetikus bemenetet a teszt írta: a
 * motor pontosan olyan alakot kap, amilyet kezelni tud. A valódi tartalom a tényleges
 * kategóriaméreteket, szinonimacsoportokat és a három feliratos képet hordozza (ADR-011).
 */
class RealContentQuizGenerationTest {

    private val report = DefaultKnowledgeValidator().validate(RealKnowledgeBase.load())

    private val imageTypes = setOf(QuestionType.IMAGE_TO_JP, QuestionType.JP_TO_IMAGE)
    private val textTypes = setOf(QuestionType.JP_TO_HU, QuestionType.HU_TO_JP)

    /** A valódi tartalom összes kategóriája, determinisztikus sorrendben. */
    private val allCategories: List<String> =
        report.accepted.map { it.categoryId.value }.distinct().sorted()

    private fun realRequest(
        enabledTypes: Set<QuestionType> = QuestionType.entries.toSet(),
        categoryFilter: Set<String> = emptySet(),
        requestedCount: Int = 10,
        seed: Long = 1L,
    ) = QuizRequest(
        candidates = QuizCandidateSet(
            techniques = report.accepted,
            synonymGroups = report.synonymGroups,
            // A tartalmi ujjlenyomatok a képfájlokból a `core/data` importban készülnek
            // (T-115); a hiányzó ujjlenyomat nem bizonyít vizuális egyezést.
            illustrationDigests = emptyMap(),
        ),
        enabledTypes = enabledTypes,
        categoryFilter = categoryFilter.mapTo(LinkedHashSet(), CategoryId::of),
        requestedCount = requestedCount,
        seed = seed,
        datasetTrust = report.datasetTrust,
    )

    // ----------------------------------------------------------------------------
    // TR-03: képes kérdés csak ott, ahol tényleg van kvízképes kép
    // ----------------------------------------------------------------------------

    /**
     * Az `stances` kategória (C) **és** (D) kérdést is ad: hét kvízképes illusztráció mind
     * a négy típusra elég, és a terv tényleg mind a négy típust használja (TR-03).
     */
    @Test
    fun realContent_stancesCategory_yieldsImageQuestionsInThePlan_TR03() {
        val subject = realRequest(
            categoryFilter = setOf(RealKnowledgeBase.ILLUSTRATED_CATEGORY_ID),
            requestedCount = 15,
            seed = 101L,
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.skippedTypes).isEmpty()
        assertThat(plan.canStart).isTrue()
        assertThat(plan.questions.map { it.type }.toSet())
            .containsExactlyElementsIn(QuestionType.entries)
        // A képes kérdések helyes válaszai a hét kvízképes rekord közül jönnek.
        val imageCapable = QuizCandidateFilter
            .correctAnswerPool(subject, QuestionType.JP_TO_IMAGE)
            .mapTo(HashSet()) { it.id }
        assertThat(imageCapable).hasSize(RealKnowledgeBase.EXPECTED_QUIZ_USABLE_ILLUSTRATION_COUNT)
        plan.questions.filter { it.type in imageTypes }.forEach { question ->
            assertThat(imageCapable).contains(question.correctTechniqueId)
        }
        assertQuizPlanInvariants("valódi tartalom – stances", subject, plan)
    }

    /**
     * Minden más kategória **nem** ad képes kérdést, és ezt a terv meg is indokolja: a
     * (C)/(D) típus [SkipReason.NOT_ENOUGH_IMAGES] okkal marad ki, miközben a szöveges
     * kérdések összeállnak (TR-03, FR-22).
     */
    @Test
    fun realContent_everyOtherCategory_yieldsNoImageQuestion_TR03() {
        val others = allCategories - RealKnowledgeBase.ILLUSTRATED_CATEGORY_ID
        assertThat(others).hasSize(8)

        others.forEach { category ->
            val subject = realRequest(categoryFilter = setOf(category), requestedCount = 10, seed = 103L)
            val plan = DefaultQuizGenerator.generate(subject)

            imageTypes.forEach { type ->
                assertThat(plan.skippedTypes[type]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
            }
            assertThat(plan.questions.map { it.type }.toSet()).containsNoneIn(imageTypes)
            // A szöveges kvíz viszont minden kategóriában elindítható.
            assertThat(plan.canStart).isTrue()
            assertThat(plan.questions.map { it.type }.toSet()).isEqualTo(textTypes)
            assertQuizPlanInvariants("valódi tartalom – $category", subject, plan)
        }
    }

    /**
     * Képes típusokra szűkített kérés a kép nélküli kategóriákban: a terv **üres**, nem
     * félkész – és az ok a képhiány, nem a jelölthiány (FR-22, TR-03).
     */
    @Test
    fun realContent_imageOnlyQuizOutsideStances_cannotStart_TR03() {
        val subject = realRequest(
            enabledTypes = imageTypes,
            categoryFilter = setOf("kata"),
            requestedCount = 5,
            seed = 107L,
        )

        val plan = DefaultQuizGenerator.generate(subject)

        assertThat(plan.questions).isEmpty()
        assertThat(plan.canStart).isFalse()
        assertThat(plan.shortfall?.reason).isEqualTo(ShortfallReason.ALL_TYPES_SKIPPED)
        imageTypes.forEach { type ->
            assertThat(plan.skippedTypes[type]).isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
        }
    }

    // ----------------------------------------------------------------------------
    // TR-02: a nyolc invariáns a valódi tartalmon
    // ----------------------------------------------------------------------------

    /**
     * A teljes tartalomra, több kérdésszámmal és több seeddel generált terv minden kérdése
     * teljesíti a kvízmotor invariánsait (TR-02, CR-07).
     *
     * A V006 szinonimacsoportok a validátor **tényleges** kimenetéből jönnek: a mostani
     * tartalomban egyetlen ütköző jelentéspár sincs, ezért a lista üres – ezt is kimondjuk,
     * hogy ha a tartalom egyszer szinonimacsoportot hoz, az ne csendben kerüljön a motorba.
     * A kölcsönös kizárás viselkedését a szintetikus mátrix fedi (`QuizPlanInvariantTest`).
     */
    @Test
    fun realContent_everyGeneratedQuestionSatisfiesAllInvariants_TR02() {
        assertThat(report.synonymGroups).isEmpty()

        listOf(5, 10, 20).forEach { requestedCount ->
            listOf(1L, 2L, 42L, -7L, Long.MAX_VALUE).forEach { seed ->
                val subject = realRequest(requestedCount = requestedCount, seed = seed)
                val plan = DefaultQuizGenerator.generate(subject)

                assertThat(plan.questionCount).isEqualTo(requestedCount)
                assertQuizPlanInvariants("valódi tartalom – $requestedCount / $seed", subject, plan)
            }
        }
    }

    /** Ugyanaz a kérés a valódi tartalmon is bitre ugyanazt a tervet adja (TR-02, NFR-07). */
    @Test
    fun realContent_sameSeed_producesIdenticalPlan_TR02() {
        val subject = realRequest(requestedCount = 20, seed = 109L)

        val first = DefaultQuizGenerator.generate(subject)
        val second = DefaultQuizGenerator.generate(subject)

        assertThat(first).isEqualTo(second)
        assertThat(first.questions.map { it.optionTechniqueIds })
            .isEqualTo(second.questions.map { it.optionTechniqueIds })
    }

    /** Más seed más kvízt ad ugyanabból a tartalomból (TR-02). */
    @Test
    fun realContent_differentSeeds_produceDifferentPlans_TR02() {
        val plans = listOf(1L, 2L, 3L).map { seed ->
            DefaultQuizGenerator.generate(realRequest(requestedCount = 20, seed = seed))
        }

        assertThat(plans.toSet()).hasSize(3)
        assertThat(plans.map { it.questionCount }.toSet()).containsExactly(20)
    }

    // ----------------------------------------------------------------------------
    // TR-03: dryRun ↔ generate a valódi tartalmon
    // ----------------------------------------------------------------------------

    @Test
    fun realContent_dryRunMatchesGenerateForEveryCategory_TR03() {
        val requests = buildList {
            add(realRequest(requestedCount = 20, seed = 211L))
            add(realRequest(enabledTypes = imageTypes, requestedCount = 10, seed = 212L))
            add(realRequest(enabledTypes = emptySet(), requestedCount = 10, seed = 213L))
            allCategories.forEach { category ->
                add(realRequest(categoryFilter = setOf(category), requestedCount = 20, seed = 214L))
                add(
                    realRequest(
                        enabledTypes = imageTypes,
                        categoryFilter = setOf(category),
                        requestedCount = 5,
                        seed = 215L,
                    ),
                )
            }
        }

        requests.forEach { subject ->
            val plan = DefaultQuizGenerator.generate(subject)
            val readiness = DefaultQuizGenerator.dryRun(subject)

            assertThat(readiness.expectedQuestionCount).isEqualTo(plan.questionCount)
            assertThat(readiness.skippedTypes).isEqualTo(plan.skippedTypes)
            assertThat(readiness.expectedShortfall).isEqualTo(plan.shortfall)
            assertThat(readiness.canStart).isEqualTo(plan.canStart)
        }
    }
}
