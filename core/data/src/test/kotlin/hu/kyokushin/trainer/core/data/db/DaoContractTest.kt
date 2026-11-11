package hu.kyokushin.trainer.core.data.db

import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlinx.coroutines.flow.Flow
import org.junit.Test

/**
 * A DAO-k szerződése: **minden** művelet `suspend` vagy `Flow` (NFR-01, kyo-android-module 6).
 *
 * Reflexióval, nem forráskód-kereséssel: a lefordított interfész az, amit a Room és a
 * hívó tényleg lát. Egy `suspend fun` utolsó paramétere a bájtkódban [Continuation], a
 * folyamatos olvasás pedig [Flow]-t ad vissza – a kettő közül valamelyiknek teljesülnie
 * kell, különben a hívás blokkolná a hívó szálat.
 *
 * Azért nem elég az adatbázis `allowMainThreadQueries` tilalma: az csak futásidőben, a
 * főszálon dob. Egy blokkoló DAO-függvény a háttérszálon is hibás tervezés, mert a
 * repository nem tudja, mit hív.
 */
class DaoContractTest {

    private val daos = listOf(
        CategoryDao::class.java,
        TechniqueDao::class.java,
        TechniqueAliasDao::class.java,
        SynonymGroupDao::class.java,
        ContentStateDao::class.java,
        QuizSessionDao::class.java,
        QuizAttemptDao::class.java,
        StudyReviewDao::class.java,
    )

    @Test
    fun dao_everyFunctionIsSuspendOrReturnsFlow_NFR01() {
        val blocking = daos.flatMap { dao ->
            dao.declaredMethods
                .filterNot { it.isSynthetic }
                .filterNot { it.isSuspend() || it.returnsFlow() }
                .map { "${dao.simpleName}.${it.name}" }
        }

        assertThat(blocking).isEmpty()
    }

    @Test
    fun dao_databaseExposesEveryDao_DR04() {
        // A nyolc tábla nyolc DAO-n érhető el; egy elfelejtett getter azt jelentené, hogy
        // a tábla létezik, de nincs rá út.
        val exposed = KyokushinDatabase::class.java.declaredMethods
            .filterNot { it.isSynthetic }
            .map { it.returnType }

        assertThat(exposed).containsAtLeastElementsIn(daos)
    }

    private fun Method.isSuspend(): Boolean =
        parameterTypes.lastOrNull() == Continuation::class.java

    private fun Method.returnsFlow(): Boolean = Flow::class.java.isAssignableFrom(returnType)
}
