package hu.kyokushin.trainer.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A domain azonosítótípusok szerződése: DR-01 (stabil `id`), DR-03 (az azonosító az
 * összehasonlítás alapja, nem a név), DR-04 (a felhasználói rekordok azonosítói).
 */
class IdentifiersTest {

    @Test
    fun techniqueId_keepsRawValue_DR01() {
        assertThat(TechniqueId.of("tech-mae-geri").value).isEqualTo("tech-mae-geri")
    }

    @Test
    fun identifiers_trimSurroundingWhitespace_DR01() {
        assertThat(TechniqueId.of("  tech-seiken  ").value).isEqualTo("tech-seiken")
        assertThat(CategoryId.of("\tkicks\n").value).isEqualTo("kicks")
        assertThat(SessionId.of(" s-1 ").value).isEqualTo("s-1")
        assertThat(QuestionId.of(" q-1 ").value).isEqualTo("q-1")
    }

    @Test
    fun identifiers_rejectBlankValue_DR01() {
        assertThrows(IllegalArgumentException::class.java) { TechniqueId.of("") }
        assertThrows(IllegalArgumentException::class.java) { TechniqueId.of("   ") }
        assertThrows(IllegalArgumentException::class.java) { CategoryId.of("") }
        assertThrows(IllegalArgumentException::class.java) { SessionId.of("") }
        assertThrows(IllegalArgumentException::class.java) { QuestionId.of("\n") }
    }

    @Test
    fun techniqueId_comparesByIdentifierNotByName_DR03() {
        val fromImport = TechniqueId.of("tech-mae-geri")
        val fromDatabase = TechniqueId.of("tech-mae-geri")
        val other = TechniqueId.of("tech-mawashi-geri")

        assertThat(fromImport).isEqualTo(fromDatabase)
        assertThat(fromImport.hashCode()).isEqualTo(fromDatabase.hashCode())
        assertThat(fromImport).isNotEqualTo(other)
    }

    @Test
    fun techniqueId_usableAsMapKey_DR03() {
        val answers = mapOf(
            TechniqueId.of("tech-a") to true,
            TechniqueId.of("tech-b") to false,
        )

        assertThat(answers[TechniqueId.of("tech-a")]).isTrue()
        assertThat(answers[TechniqueId.of("tech-b")]).isFalse()
        assertThat(answers[TechniqueId.of("tech-c")]).isNull()
    }

    @Test
    fun identifiers_toStringExposesPlainValue_DR04() {
        assertThat(SessionId.of("session-7").toString()).isEqualTo("session-7")
        assertThat(QuestionId.of("question-3").toString()).isEqualTo("question-3")
    }

    @Test
    fun identifierTypes_areDistinctTypes_DR03() {
        // A TechniqueId és a CategoryId azonos nyers értékből is két különböző típus:
        // a felcserélésük fordítási hiba, nem futásidejű bug. Itt csak azt igazoljuk,
        // hogy a dobozolt típusok nem azonos osztályt kapnak.
        assertThat(TechniqueId.of("x")::class).isNotEqualTo(CategoryId.of("x")::class)
        assertThat(SessionId.of("x")::class).isNotEqualTo(QuestionId.of("x")::class)
    }
}
