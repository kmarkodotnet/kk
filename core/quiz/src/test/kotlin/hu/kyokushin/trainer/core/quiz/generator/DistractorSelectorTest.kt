package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import org.junit.Test

/**
 * A distraktorválasztás (T-109, TR-02, FR-14, FR-22, CR-07).
 *
 * Amit ez a készlet bizonyít:
 *
 * 1. pontosan három, páronként különböző téves válasz, vagy **semmi** – háromopciós
 *    kérdés nem keletkezhet (FR-14),
 * 2. a prioritás az azonos kategória, és más kategória csak feltöltésként jön (FR-14),
 * 3. szinonim rekord soha nem lesz distraktor, és két szinonim rekord nem kerül
 *    egyszerre ugyanabba a kérdésbe (CR-07),
 * 4. a (D) típusban nincs két vizuálisan azonos illusztráció az opciók között (TR-02),
 * 5. kevés adatnál a motor nem „kisegít": inkább nincs kérdés (FR-22),
 * 6. minden opció **létező** technikarekord – a motor nem gyárt opciót,
 * 7. a kiválasztás rendezett bejárású, tehát a bemenet sorrendjétől független (NFR-07).
 */
class DistractorSelectorTest {
    private fun selected(
        request: QuizRequest,
        type: QuestionType = QuestionType.JP_TO_HU,
        correctId: String = "KY-0001",
    ): List<Technique>? {
        val correct = QuizCandidateFilter.correctAnswerPool(request, type)
            .single { it.id.value == correctId }
        return DistractorSelector.selectDistractors(request, type, correct)
    }

    // ----------------------------------------------------------------------------
    // Pontosan négy opció vagy semmi (FR-14, TR-02)
    // ----------------------------------------------------------------------------

    @Test
    fun selectDistractors_enoughCandidates_yieldsThreeDistinctWrongAnswers_FR14() {
        val request = request(techniques = textCandidates())

        val distractors = selected(request)

        assertThat(distractors).isNotNull()
        assertThat(distractors!!).hasSize(DistractorSelector.DISTRACTOR_COUNT)
        assertThat(distractors.map { it.id.value }).containsNoDuplicates()
        assertThat(distractors.map { it.id.value }).doesNotContain("KY-0001")
    }

    @Test
    fun selectOptionIds_buildsExactlyFourOptionsWithOneCorrect_FR14() {
        val request = request(techniques = textCandidates())
        val correct = QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_HU).first()

        val options = DistractorSelector.selectOptionIds(request, QuestionType.JP_TO_HU, correct)

        assertThat(options).isNotNull()
        assertThat(options!!).hasSize(QuizQuestion.OPTION_COUNT)
        assertThat(options.toSet()).hasSize(QuizQuestion.OPTION_COUNT)
        assertThat(options.count { it == correct.id }).isEqualTo(1)
        // A kérdés konstruktora is elfogadja: nincs háromopciós vagy kéthelyes kérdés.
        val question = question(
            correct = correct.id.value,
            options = options.map { it.value },
        )
        assertThat(question.distractorTechniqueIds).hasSize(DistractorSelector.DISTRACTOR_COUNT)
    }

    @Test
    fun selectDistractors_everyOptionIsAnExistingTechniqueRecord_FR14() {
        val request = request(techniques = textCandidates())
        val knownIds = request.candidates.techniques.map { it.id }

        val distractors = selected(request)

        // A motor nem gyárt opciót: minden distraktor a jelöltkészlet egy rekordja.
        assertThat(knownIds).containsAtLeastElementsIn(distractors!!.map { it.id })
    }

    @Test
    fun selectDistractors_fewerThanThreeAcceptable_buildsNoQuestion_FR14_TR02() {
        // Három rekord összesen: a helyes mellé csak két distraktor van.
        val request = request(techniques = textCandidates().take(3))

        assertThat(selected(request)).isNull()
        assertThat(
            DistractorSelector.selectOptionIds(
                request,
                QuestionType.JP_TO_HU,
                request.candidates.techniques.first(),
            ),
        ).isNull()
        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_HU))
            .isEqualTo(0)
    }

    // ----------------------------------------------------------------------------
    // Prioritás: azonos kategória, majd feltöltés más kategóriából (FR-14)
    // ----------------------------------------------------------------------------

    @Test
    fun selectDistractors_prefersSameCategoryWhenThreeAreAvailable_FR14() {
        val request = request(
            techniques = textCandidates("geri") + (5..8).map {
                technique(id = "KY-000$it", categoryId = "tsuki", romaji = "Tsuki $it")
            },
        )

        val distractors = selected(request)!!

        // A kategórián belüli megkülönböztetés a tanulási cél: más kategória nem kell.
        assertThat(distractors.map { it.categoryId.value }.toSet()).containsExactly("geri")
    }

    @Test
    fun selectDistractors_fewerThanThreeInCategory_fillsFromOtherCategories_FR14() {
        val request = request(
            techniques = listOf(
                technique(id = "KY-0001", categoryId = "geri"),
                technique(id = "KY-0002", categoryId = "geri"),
                technique(id = "KY-0003", categoryId = "tsuki"),
                technique(id = "KY-0004", categoryId = "uke"),
            ),
        )

        val distractors = selected(request)!!

        assertThat(distractors.map { it.id.value }).containsExactly("KY-0002", "KY-0003", "KY-0004")
        // A saját kategóriájú jelölt elöl van: a feltöltés csak a hiányt pótolja.
        assertThat(distractors.first().categoryId.value).isEqualTo("geri")
    }

    // ----------------------------------------------------------------------------
    // Szinonimakizárás – V006 csoportok (CR-07)
    // ----------------------------------------------------------------------------

    @Test
    fun selectDistractors_synonymOfCorrectAnswer_isNeverADistractor_CR07() {
        val request = request(
            techniques = textCandidates() + technique(id = "KY-0005", romaji = "Technique 5"),
            synonymGroups = listOf(setOf("KY-0001", "KY-0002")),
        )

        val distractors = selected(request)!!

        assertThat(distractors.map { it.id.value }).doesNotContain("KY-0002")
        assertThat(distractors.map { it.id.value }).containsExactly("KY-0003", "KY-0004", "KY-0005")
    }

    @Test
    fun selectDistractors_twoSynonymousCandidates_neverShareOneQuestion_CR07() {
        val request = request(
            techniques = textCandidates() + (5..6).map {
                technique(id = "KY-000$it", romaji = "Technique $it")
            },
            // A két distraktorjelölt egymás szinonimája – csak az egyikük jöhet be.
            synonymGroups = listOf(setOf("KY-0002", "KY-0003")),
        )

        val distractors = selected(request)!!
        val ids = distractors.map { it.id.value }

        assertThat(ids).contains("KY-0002")
        assertThat(ids).doesNotContain("KY-0003")
        assertThat(ids).containsExactly("KY-0002", "KY-0004", "KY-0005")
    }

    @Test
    fun selectDistractors_synonymShortageIsNotRescuedBySynonym_FR22_CR07() {
        // Négy kvízképes rekord, de a helyesnek két szinonimája van: két distraktor marad.
        val request = request(
            techniques = textCandidates(),
            synonymGroups = listOf(setOf("KY-0001", "KY-0002", "KY-0003")),
        )

        // Inkább nincs kérdés, mint szinonim opcióval kisegített kérdés.
        assertThat(selected(request)).isNull()
        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.JP_TO_HU)).hasSize(4)
        assertThat(DistractorSelector.buildableCorrectAnswers(request, QuestionType.JP_TO_HU))
            .isEmpty()
    }

    @Test
    fun buildableQuestionCount_synonymExclusionNarrowsTheUpperBound_CR07() {
        val request = request(
            techniques = textCandidates(),
            synonymGroups = listOf(setOf("KY-0001", "KY-0002")),
        )

        // A T-108 felső korlátja: négy kvízképes rekord, négy opció – látszólag elég.
        assertThat(QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_HU)).hasSize(4)
        assertThat(QuizCandidateFilter.optionPool(request, QuestionType.JP_TO_HU)).hasSize(4)
        // A V006 kizárás után viszont egyetlen helyes válasz mellé sem jön ki három
        // elfogadható distraktor: KY-0001 és KY-0002 kizárják egymást, a maradék kettő
        // pedig csak két distraktor. Inkább nincs kérdés (FR-22).
        assertThat(DistractorSelector.buildableCorrectAnswers(request, QuestionType.JP_TO_HU))
            .isEmpty()
        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_HU))
            .isEqualTo(0)
    }

    @Test
    fun buildableQuestionCount_enoughRecordsBesideASynonymPair_staysExact_CR07() {
        val request = request(
            techniques = textCandidates() + (5..6).map {
                technique(id = "KY-000$it", romaji = "Technique $it")
            },
            synonymGroups = listOf(setOf("KY-0001", "KY-0002")),
        )

        // Hat rekordnál a szinonimapár már nem akadály: minden helyes jelölt mellé
        // összeáll három, egymást ki nem záró distraktor.
        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_HU))
            .isEqualTo(6)
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_HU)).isNull()
        // És a szinonimapár tagjai akkor sem kerülnek egy kérdésbe.
        QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_HU).forEach { correct ->
            val options = DistractorSelector
                .selectOptionIds(request, QuestionType.JP_TO_HU, correct)!!
                .map { it.value }
            assertThat(options.containsAll(listOf("KY-0001", "KY-0002"))).isFalse()
        }
    }

    @Test
    fun skipReason_everyCorrectAnswerBlockedBySynonyms_isCandidateShortage_FR22() {
        val request = request(
            techniques = textCandidates(),
            synonymGroups = listOf(setOf("KY-0001", "KY-0002"), setOf("KY-0003", "KY-0004")),
        )

        // Mind a négy rekordnak van szinonimája, ezért egyikhez sincs három distraktor.
        assertThat(QuizCandidateFilter.buildableQuestionCount(request, QuestionType.JP_TO_HU))
            .isEqualTo(0)
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_HU))
            .isEqualTo(SkipReason.NOT_ENOUGH_CANDIDATES)
    }

    // ----------------------------------------------------------------------------
    // (D) JP_TO_IMAGE – vizuálisan azonos kép nem lehet két opció (TR-02, FR-14)
    // ----------------------------------------------------------------------------

    private fun imageRequest(
        digests: Map<String, String>,
        count: Int = digests.size,
    ) = request(
        techniques = (1..count).map { index ->
            technique(
                id = "KY-000$index",
                romaji = "Technique $index",
                illustration = asset(ref = "KY-000$index"),
            )
        },
        illustrationDigests = digests,
    )

    @Test
    fun selectDistractors_jpToImage_neverRepeatsAVisuallyIdenticalIllustration_TR02() {
        val request = imageRequest(
            mapOf(
                "KY-0001" to "d1",
                "KY-0002" to "d2",
                "KY-0003" to "d2",
                "KY-0004" to "d3",
                "KY-0005" to "d4",
            ),
        )

        val distractors = selected(request, QuestionType.JP_TO_IMAGE)!!
        val digests = distractors.map { request.candidates.illustrationDigestOf(it.id) }

        assertThat(digests).containsNoDuplicates()
        // A d2 ujjlenyomatú párból csak az első kerül be.
        assertThat(distractors.map { it.id.value }).containsExactly("KY-0002", "KY-0004", "KY-0005")
    }

    @Test
    fun selectDistractors_jpToImage_imageIdenticalToTheCorrectOne_isExcluded_TR02() {
        val request = imageRequest(
            mapOf(
                "KY-0001" to "same",
                "KY-0002" to "same",
                "KY-0003" to "d3",
                "KY-0004" to "d4",
                "KY-0005" to "d5",
            ),
        )

        val distractors = selected(request, QuestionType.JP_TO_IMAGE)!!

        assertThat(distractors.map { it.id.value }).doesNotContain("KY-0002")
        assertThat(distractors.map { it.id.value }).containsExactly("KY-0003", "KY-0004", "KY-0005")
    }

    @Test
    fun selectDistractors_jpToImage_tooFewDistinctImages_buildsNoQuestion_FR14() {
        val request = imageRequest(
            mapOf(
                "KY-0001" to "a",
                "KY-0002" to "a",
                "KY-0003" to "b",
                "KY-0004" to "c",
            ),
        )

        QuizCandidateFilter.correctAnswerPool(request, QuestionType.JP_TO_IMAGE).forEach { correct ->
            assertThat(
                DistractorSelector.selectDistractors(request, QuestionType.JP_TO_IMAGE, correct),
            ).isNull()
        }
        assertThat(QuizCandidateFilter.skipReason(request, QuestionType.JP_TO_IMAGE))
            .isEqualTo(SkipReason.NOT_ENOUGH_IMAGES)
    }

    @Test
    fun selectDistractors_jpToImage_missingDigestDoesNotProveIdentity_FR14() {
        // Ujjlenyomat nélküli, de kvízképes képek: önálló opciónak számítanak.
        val request = imageRequest(digests = emptyMap(), count = 4)

        val distractors = selected(request, QuestionType.JP_TO_IMAGE)

        assertThat(distractors).isNotNull()
        assertThat(distractors!!).hasSize(DistractorSelector.DISTRACTOR_COUNT)
    }

    @Test
    fun selectDistractors_imageToJp_distractorsAreNamesSoNoImageIsRequired_FR14() {
        // A (C) kérdés képe a helyes válaszé; a distraktoroknak nem kell képük.
        val request = request(
            techniques = listOf(
                technique(id = "KY-0001", illustration = asset(ref = "KY-0001")),
            ) + textCandidates().drop(1),
        )

        val distractors = selected(request, QuestionType.IMAGE_TO_JP)!!

        assertThat(distractors.map { it.id.value }).containsExactly("KY-0002", "KY-0003", "KY-0004")
        assertThat(distractors.none { it.illustration != null }).isTrue()
    }

    // ----------------------------------------------------------------------------
    // Determinizmus rendezett bejárással (NFR-07) – a seedelt keverés a T-110 tárgya
    // ----------------------------------------------------------------------------

    @Test
    fun selectDistractors_isDeterministicAndIndependentOfInputOrder_NFR07() {
        val techniques = textCandidates() + (5..8).map {
            technique(id = "KY-000$it", romaji = "Technique $it")
        }
        val ascending = request(techniques = techniques)
        val reversed = request(techniques = techniques.reversed())

        val first = selected(ascending)!!.map { it.id.value }
        val again = selected(ascending)!!.map { it.id.value }
        val fromReversedInput = selected(reversed)!!.map { it.id.value }

        assertThat(again).isEqualTo(first)
        assertThat(fromReversedInput).isEqualTo(first)
        assertThat(first).isInOrder()
    }

    @Test
    fun acceptableDistractors_areOrderedSameCategoryFirstThenById_NFR07() {
        val request = request(
            techniques = listOf(
                technique(id = "KY-0009", categoryId = "geri"),
                technique(id = "KY-0003", categoryId = "tsuki"),
                technique(id = "KY-0007", categoryId = "geri"),
                technique(id = "KY-0001", categoryId = "geri"),
                technique(id = "KY-0002", categoryId = "tsuki"),
            ),
        )
        val correct = request.candidates.techniques.single { it.id.value == "KY-0001" }

        val acceptable = DistractorSelector
            .acceptableDistractors(request, QuestionType.JP_TO_HU, correct)

        assertThat(acceptable.map { it.id.value })
            .containsExactly("KY-0007", "KY-0009", "KY-0002", "KY-0003")
            .inOrder()
    }

    @Test
    fun buildableCorrectAnswers_areOrderedById_forStableQuestionSelection_NFR07() {
        val request = request(
            techniques = (textCandidates() + technique(id = "KY-0005")).reversed(),
        )

        val buildable = DistractorSelector
            .buildableCorrectAnswers(request, QuestionType.JP_TO_HU)

        assertThat(buildable.map { it.id.value }).isInOrder()
        // Minden helyes jelölt mellé összeáll kérdés: a nem ismétlés alapja ez az öt elem.
        assertThat(buildable).hasSize(5)
    }
}
