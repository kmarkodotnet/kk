package hu.kyokushin.trainer.core.quiz.generator

import kotlin.random.Random

/**
 * A kvízgenerátor egyetlen véletlenforrása – Fisher–Yates keverés a **kapott** RNG-vel
 * (NFR-07, ADR-007, T-110).
 *
 * Miért külön típus, és miért nem a `List.shuffled(random)` stdlib hívás?
 *
 * 1. **Egy belépési pont.** A determinizmus akkor romlik el észrevétlenül, ha valahol egy
 *    `Random.Default` vagy egy paraméter nélküli `shuffled()` csúszik be. Ha a modulban
 *    egyetlen keverő függvény van, és az `Random`-ot **paraméterként** kér, akkor a seed
 *    nélküli keverés nem „elfelejtett paraméter", hanem fordítási hiba.
 * 2. **Kiszámítható RNG-fogyás.** A keverés pontosan `size - 1` darab `nextInt` hívást
 *    használ, mindig ugyanabban a sorrendben. A generátor ezért ugyanazt a tervet adja
 *    akkor is, ha a stdlib keverési algoritmusa egy Kotlin-verzióval változna – a mentett
 *    kvízállapot visszaállítása (T-405) ezen a garancián áll.
 * 3. **Nem módosít.** A bemeneti lista érintetlen marad, a kimenet új lista: a jelöltlisták
 *    a [QuizCandidateFilter] rendezett kimenetei, azokat nem keverhetjük a helyükön.
 *
 * A keverés **csak sorrendet** változtat: egyetlen elemet sem ad hozzá, nem hagy el és nem
 * duplikál. Ez az, ami miatt a négy opció keverése nem tud kérdést elrontani – az opciók
 * kiválasztása a [DistractorSelector] dolga, a keverés már csak permutál.
 */
internal object SeededShuffle {
    /**
     * Az [items] elemei új sorrendben, kizárólag a [random] által meghatározott módon.
     *
     * Klasszikus, lefelé haladó Fisher–Yates: az `i` indexű helyre a `0..i` tartományból
     * sorsolt elem kerül. Egy és nulla elemű listára nem sorsol – ilyenkor nincs mit
     * permutálni, és így az RNG-fogyás sem csúszik el egy haszontalan híváson.
     */
    fun <T> shuffled(items: List<T>, random: Random): List<T> {
        if (items.size < 2) return items.toList()
        val result = items.toMutableList()
        for (index in result.lastIndex downTo 1) {
            val swapWith = random.nextInt(index + 1)
            if (swapWith != index) {
                val held = result[index]
                result[index] = result[swapWith]
                result[swapWith] = held
            }
        }
        return result
    }
}
