package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A [QuizQuestion] szerződése: pontosan négy opció, pontosan egy helyes (FR-14), és a
 * kérdés **kizárólag azonosítókat** tárol (ADR-006, DR-03).
 *
 * Ezek a tesztek szándékosan a struktúrát és nem a tartalmat vizsgálják: a kérdéstípus
 * helyessége nem függhet attól, milyen technikák vannak éppen a tudástárban.
 */
class QuizQuestionContractTest {

    private val json = Json

    @Test
    fun quizQuestion_rejectsFewerThanFourOptions_FR14() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            question(options = listOf("KY-0001", "KY-0002", "KY-0003"))
        }
        assertThat(failure).hasMessageThat().contains("pontosan 4 opciója")
    }

    @Test
    fun quizQuestion_rejectsMoreThanFourOptions_FR14() {
        assertThrows(IllegalArgumentException::class.java) {
            question(options = listOf("KY-0001", "KY-0002", "KY-0003", "KY-0004", "KY-0005"))
        }
    }

    @Test
    fun quizQuestion_rejectsRepeatedOption_FR14() {
        assertThrows(IllegalArgumentException::class.java) {
            question(options = listOf("KY-0001", "KY-0002", "KY-0002", "KY-0004"))
        }
    }

    @Test
    fun quizQuestion_rejectsCorrectAnswerMissingFromOptions_FR14() {
        assertThrows(IllegalArgumentException::class.java) {
            question(
                correct = "KY-0099",
                options = listOf("KY-0001", "KY-0002", "KY-0003", "KY-0004"),
            )
        }
    }

    @Test
    fun quizQuestion_hasExactlyOneCorrectOptionAndThreeDistractors_FR14() {
        val subject = question(correct = "KY-0003")

        assertThat(subject.optionTechniqueIds).hasSize(QuizQuestion.OPTION_COUNT)
        assertThat(subject.optionTechniqueIds.count { it == subject.correctTechniqueId }).isEqualTo(1)
        assertThat(subject.correctOptionIndex).isEqualTo(2)
        assertThat(subject.distractorTechniqueIds).hasSize(3)
        assertThat(subject.distractorTechniqueIds).doesNotContain(subject.correctTechniqueId)
    }

    /**
     * ADR-006: a kérdésnek nem lehet olyan mezője, amibe megjelenítendő szöveg, kép vagy
     * `contentDescription` kerülhetne. Ha valaki ilyet vesz fel, ez a teszt bukik – és
     * éppen ez a dolga: a DR-03 hibaosztály (szövegegyezéses összehasonlítás) itt
     * zárható ki szerkezetileg.
     */
    @Test
    fun quizQuestion_declaresIdentifierFieldsOnly_ADR006() {
        // Csak a példánymezők érdekesek: a statikusak a `companion` és a szerializációs
        // plugin által generált segédmezők, nem a kérdés állapota.
        val fieldNames = QuizQuestion::class.java.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }

        assertThat(fieldNames).containsExactly(
            "id",
            "type",
            "correctTechniqueId",
            "optionTechniqueIds",
        )
    }

    /**
     * A tartalmi szivárgás tiltása működés közben: a szerializált kérdésben egyetlen
     * megjelenítendő szöveg vagy képfájlnév sem szerepelhet (ADR-006, NFR-04).
     */
    @Test
    fun serializedQuestion_leaksNoDisplayTextOrImageName_ADR006() {
        val correct = technique(
            id = "KY-0001",
            romaji = "Mae geri",
            hungarian = "Előre rúgás",
            illustration = IllustrationRef.Asset(
                ref = "ill-mae-geri",
                fileName = "mae_geri.webp",
                quizUsable = true,
                hideableOnStudyCard = false,
            ),
        )
        val subject = question(
            type = QuestionType.IMAGE_TO_JP,
            correct = correct.id.value,
        )

        val encoded = json.encodeToString(QuizQuestion.serializer(), subject)

        assertThat(encoded).doesNotContain(correct.romaji)
        assertThat(encoded).doesNotContain(correct.hungarian)
        assertThat(encoded).doesNotContain("mae_geri.webp")
        assertThat(encoded).doesNotContain("ill-mae-geri")
        assertThat(encoded).contains("KY-0001")
    }

    /**
     * A kérdés a `SavedStateHandle`-be kerül (ADR-007, T-405), ezért a körbefordulásnak
     * azonos objektumot kell adnia – az **opciók sorrendjével együtt**, mert a sorrend a
     * seedelt keverés eredménye (NFR-07).
     */
    @Test
    fun quizQuestion_survivesSerializationRoundTripWithOptionOrder_FR19() {
        val subject = question(
            id = "Q-7",
            type = QuestionType.JP_TO_IMAGE,
            correct = "KY-0004",
            options = listOf("KY-0004", "KY-0001", "KY-0003", "KY-0002"),
        )

        val restored = json.decodeFromString(
            QuizQuestion.serializer(),
            json.encodeToString(QuizQuestion.serializer(), subject),
        )

        assertThat(restored).isEqualTo(subject)
        assertThat(restored.optionTechniqueIds).isEqualTo(subject.optionTechniqueIds)
    }

    /**
     * A visszafejtés a `TechniqueId.of(...)` gyáron megy át, ezért a mentett állapotból sem
     * tud üres azonosító vagy négytől eltérő opciószám bejutni (DR-01, FR-14).
     */
    @Test
    fun corruptSavedState_cannotBypassQuestionInvariants_FR14() {
        val tooFewOptions = """
            {"id":"Q-1","type":"JP_TO_HU","correctTechniqueId":"KY-0001",
             "optionTechniqueIds":["KY-0001","KY-0002"]}
        """.trimIndent()
        val blankId = """
            {"id":"Q-1","type":"JP_TO_HU","correctTechniqueId":"  ",
             "optionTechniqueIds":["  ","KY-0002","KY-0003","KY-0004"]}
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            json.decodeFromString(QuizQuestion.serializer(), tooFewOptions)
        }
        assertThrows(IllegalArgumentException::class.java) {
            json.decodeFromString(QuizQuestion.serializer(), blankId)
        }
    }

    @Test
    fun answerEvaluation_comparesIdentifiersNotText_DR03() {
        // Két technika azonos magyar jelentéssel: szövegegyezéssel a téves válasz is
        // helyesnek tűnne. Azonosító-összehasonlítással nem.
        val correct = technique(id = "KY-0001", hungarian = "Előre rúgás")
        val sameHungarian = technique(id = "KY-0002", hungarian = "Előre rúgás")
        val subject = question(
            correct = correct.id.value,
            options = listOf("KY-0001", "KY-0002", "KY-0003", "KY-0004"),
        )

        assertThat(sameHungarian.hungarian).isEqualTo(correct.hungarian)
        assertThat(sameHungarian.id == subject.correctTechniqueId).isFalse()
        assertThat(correct.id == subject.correctTechniqueId).isTrue()
    }

    @Test
    fun quizQuestion_identityIsItsIdNotItsContent_ADR005() {
        val first = QuizQuestion(
            id = QuestionId.of("Q-1"),
            type = QuestionType.JP_TO_HU,
            correctTechniqueId = TechniqueId.of("KY-0001"),
            optionTechniqueIds = ids("KY-0001", "KY-0002", "KY-0003", "KY-0004"),
        )
        val sameContentDifferentId = first.copy(id = QuestionId.of("Q-2"))

        assertThat(sameContentDifferentId).isNotEqualTo(first)
    }
}
