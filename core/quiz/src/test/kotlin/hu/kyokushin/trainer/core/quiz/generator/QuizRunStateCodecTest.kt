package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.TechniqueId
import org.junit.Test

/**
 * A [QuizRunStateCodec] – a futó kvíz mentett állapotának (`QuizRunState`,
 * `docs/ARCHITECTURE.md` 6.2) `SavedStateHandle`-be írható alakja (T-405, FR-19, NFR-07).
 *
 * A `feature/quiz` ViewModelje csak ezeket a függvényeket hívja, sosem `Json`-t
 * közvetlenül (ADR-001, L3 – lásd a kódolt osztály KDoc-ja) – ezért a round-trip
 * garanciát itt, a `core/quiz`-ben kell bizonyítani.
 */
class QuizRunStateCodecTest {

    @Test
    fun encodePlan_thenDecode_restoresIdenticalPlan_FR19() {
        val plan = QuizPlan(
            questions = listOf(
                question(id = "Q-1", type = QuestionType.JP_TO_HU),
                question(
                    id = "Q-2",
                    type = QuestionType.HU_TO_JP,
                    correct = "KY-0003",
                    options = listOf("KY-0003", "KY-0001", "KY-0004", "KY-0002"),
                ),
            ),
            skippedTypes = mapOf(QuestionType.JP_TO_IMAGE to SkipReason.NOT_ENOUGH_IMAGES),
            shortfall = Shortfall(
                requestedCount = 5,
                actualCount = 2,
                reason = ShortfallReason.NOT_ENOUGH_IMAGES,
            ),
        )

        val restored = QuizRunStateCodec.decodePlan(QuizRunStateCodec.encodePlan(plan))

        assertThat(restored).isEqualTo(plan)
    }

    @Test
    fun encodePlan_thenDecode_preservesOptionOrder_FR19() {
        // ARCHITECTURE 6.2: "a questions mentése miatt ... bitre ugyanaz a kvíz áll
        // vissza" – az opciósorrend is a mentett állapot része (ADR-005).
        val plan = QuizPlan(
            questions = listOf(
                question(
                    id = "Q-1",
                    correct = "KY-0004",
                    options = listOf("KY-0002", "KY-0004", "KY-0001", "KY-0003"),
                ),
            ),
        )

        val restored = QuizRunStateCodec.decodePlan(QuizRunStateCodec.encodePlan(plan))

        assertThat(restored.questions.single().optionTechniqueIds)
            .isEqualTo(plan.questions.single().optionTechniqueIds)
    }

    @Test
    fun encodeAnswers_thenDecode_restoresIdenticalMap_FR18() {
        val answers = mapOf(
            QuestionId.of("Q-1") to TechniqueId.of("KY-0001"),
            QuestionId.of("Q-2") to TechniqueId.of("KY-0003"),
        )

        val restored = QuizRunStateCodec.decodeAnswers(QuizRunStateCodec.encodeAnswers(answers))

        assertThat(restored).isEqualTo(answers)
    }

    @Test
    fun encodeAnswers_emptyMap_roundTrips_FR19() {
        val restored = QuizRunStateCodec.decodeAnswers(QuizRunStateCodec.encodeAnswers(emptyMap()))

        assertThat(restored).isEmpty()
    }

    @Test
    fun decodePlan_rejectsManipulatedEmptyIdentifier() {
        // A `TechniqueId`/`QuestionId` privát konstruktora a visszafejtéskor is érvényesít
        // (lásd `QuizIdSerializers.kt` KDoc-ja) – egy sérült mentett állapot nem tud üres
        // azonosítót becsempészni a domainbe.
        val manipulated = """
            {"questions":[{"id":"","type":"JP_TO_HU","correctTechniqueId":"KY-0001",
            "optionTechniqueIds":["KY-0001","KY-0002","KY-0003","KY-0004"]}],
            "skippedTypes":{},"shortfall":null}
        """.trimIndent()

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            QuizRunStateCodec.decodePlan(manipulated)
        }
    }
}
