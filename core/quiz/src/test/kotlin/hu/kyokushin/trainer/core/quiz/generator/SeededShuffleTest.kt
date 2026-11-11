package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/**
 * A [SeededShuffle] Fisher–Yates keverése – a determinizmus legalsó építőköve
 * (T-110, T-112, TR-02, NFR-07, ADR-007).
 *
 * A generátor minden sorrenddöntése ezen az egyetlen függvényen megy át: a kérdéssorrend
 * és az opciósorrend is. Ezért három állítást itt, elemi szinten bizonyítunk, hogy a
 * magasabb szintű tervtesztek ne ezt mérjék újra:
 *
 * 1. a keverés **csak permutál** – nem ad hozzá, nem hagy el és nem duplikál elemet,
 *    és a bemeneti listát nem módosítja (az `optionPool` rendezett listáit más is használja),
 * 2. **kiszámítható RNG-fogyás**: pontosan `size - 1` sorsolás, egy és nulla elemnél nulla.
 *    Erre épül az, hogy a kérdéssorrend és az opciósorrend sorsolásai nem csúsznak el
 *    egymáson, tehát a mentett kvízállapot visszaállítása ugyanazt a kvízt adja (T-405),
 * 3. a keverés **tényleg kever**: ugyanaz a seed ugyanazt a permutációt adja, de a seedek
 *    felett minden permutáció elérhető – nem egy elfajuló, sorrendet őrző művelet.
 */
class SeededShuffleTest {

    /**
     * Sorsolásszámláló RNG: a kérdéseket delegálja egy seedelt forrásnak, de megszámolja a
     * `nextInt(until)` hívásokat. Így az RNG-fogyás nem „valószínűleg annyi", hanem mért
     * érték – a determinizmus garanciája ezen áll.
     */
    private class CountingRandom(seed: Long) : Random() {
        private val delegate = Random(seed)
        var drawCount: Int = 0
            private set

        override fun nextBits(bitCount: Int): Int = delegate.nextBits(bitCount)

        override fun nextInt(until: Int): Int {
            drawCount++
            return delegate.nextInt(until)
        }
    }

    private val tenItems = (1..10).map { "item-$it" }

    @Test
    fun shuffled_isAPermutationNeverAddingOrDroppingAnElement_TR02() {
        (0L..40L).forEach { seed ->
            val result = SeededShuffle.shuffled(tenItems, Random(seed))

            assertThat(result).hasSize(tenItems.size)
            // Multihalmaz-egyezés: se duplikálás, se elhagyás, se új elem.
            assertThat(result.sorted()).isEqualTo(tenItems.sorted())
        }
    }

    @Test
    fun shuffled_doesNotModifyTheInputList_TR02() {
        val source = tenItems.toMutableList()
        val snapshot = source.toList()

        SeededShuffle.shuffled(source, Random(7L))

        assertThat(source).isEqualTo(snapshot)
    }

    @Test
    fun shuffled_sameSeed_producesTheSamePermutation_TR02() {
        val first = SeededShuffle.shuffled(tenItems, Random(123L))
        val second = SeededShuffle.shuffled(tenItems, Random(123L))

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun shuffled_consumesExactlySizeMinusOneDraws_TR02() {
        listOf(2, 3, 4, 10, 25).forEach { size ->
            val random = CountingRandom(seed = size.toLong())

            SeededShuffle.shuffled((1..size).toList(), random)

            assertThat(random.drawCount).isEqualTo(size - 1)
        }
    }

    @Test
    fun shuffled_emptyOrSingleList_drawsNothing_TR02() {
        listOf(emptyList(), listOf("only")).forEach { items ->
            val random = CountingRandom(seed = 1L)

            val result = SeededShuffle.shuffled(items, random)

            assertThat(result).isEqualTo(items)
            assertThat(random.drawCount).isEqualTo(0)
        }
    }

    /**
     * A keverés nem elfajuló: négy elemen a seedek felett **mind a 24** permutáció elő is
     * fordul. Ez az, ami miatt a helyes válasz pozíciója sem tapad az első helyre (FR-14).
     */
    @Test
    fun shuffled_reachesEveryPermutationAcrossSeeds_TR02() {
        val options = listOf("a", "b", "c", "d")

        val permutations = (0L until 2_000L)
            .mapTo(HashSet()) { seed -> SeededShuffle.shuffled(options, Random(seed)) }

        assertThat(permutations).hasSize(24)
    }

    @Test
    fun shuffled_differentSeeds_produceDifferentOrderings_TR02() {
        val first = SeededShuffle.shuffled(tenItems, Random(1L))
        val second = SeededShuffle.shuffled(tenItems, Random(2L))

        assertThat(first).isNotEqualTo(second)
    }
}
