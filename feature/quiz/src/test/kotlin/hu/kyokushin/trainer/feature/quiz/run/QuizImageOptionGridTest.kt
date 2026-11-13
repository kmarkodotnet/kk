package hu.kyokushin.trainer.feature.quiz.run

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.TechniqueId
import org.junit.Test

/**
 * A [QuizImageOptionGrid] tiszta elrendezési logikája – emulátor és Robolectric nélkül,
 * sima JUnit teszttel (T-408, FR-12, NFR-04).
 */
class QuizImageOptionGridTest {
    private fun option(id: String) = QuizOptionDisplay(
        techniqueId = TechniqueId.of(id),
        romaji = "$id-romaji",
        hungarian = "$id-hungarian",
        illustration = null,
    )

    @Test
    fun rows_fourOptionsTwoColumns_splitsIntoTwoRowsOfTwo_FR12() {
        val options = listOf(option("KY-0001"), option("KY-0002"), option("KY-0003"), option("KY-0004"))

        val rows = QuizImageOptionGrid.rows(options, columns = 2)

        assertThat(rows).hasSize(2)
        assertThat(rows[0]).hasSize(2)
        assertThat(rows[1]).hasSize(2)
    }

    /**
     * NFR-04: a pozíció az [QuizOptionDisplay] lista EREDETI sorrendjéből jön (1-alapú),
     * nem rendezi újra a kártyákat – a `IllustrationLabel.QuizOption` leírása emiatt
     * megbízhatóan ugyanarra az opcióra mutat, amit a TalkBack bejelent.
     */
    @Test
    fun rows_assignsSequentialOneBasedPositions_inOriginalOrder_NFR04() {
        val options = listOf(option("KY-0001"), option("KY-0002"), option("KY-0003"), option("KY-0004"))

        val rows = QuizImageOptionGrid.rows(options, columns = 2)
        val flattened = rows.flatten()

        assertThat(flattened.map { it.position }).containsExactly(1, 2, 3, 4).inOrder()
        assertThat(flattened.map { it.option.techniqueId }).containsExactlyElementsIn(
            options.map { it.techniqueId },
        ).inOrder()
    }

    @Test
    fun rows_notDivisibleByColumns_lastRowIsShorter_FR12() {
        val options = listOf(option("KY-0001"), option("KY-0002"), option("KY-0003"))

        val rows = QuizImageOptionGrid.rows(options, columns = 2)

        assertThat(rows).hasSize(2)
        assertThat(rows[0]).hasSize(2)
        assertThat(rows[1]).hasSize(1)
        assertThat(rows[1].single().position).isEqualTo(3)
    }

    @Test
    fun rows_rejectsNonPositiveColumnCount_FR12() {
        runCatching { QuizImageOptionGrid.rows(listOf(option("KY-0001")), columns = 0) }
            .also { assertThat(it.isFailure).isTrue() }
    }
}
