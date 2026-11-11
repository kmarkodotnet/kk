package hu.kyokushin.trainer.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.di.IoDispatcher
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeValidator
import java.time.Clock
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Az alkalmazásszintű kötések belépési pontja (T-203).
 *
 * ## Miért létezik egyáltalán?
 *
 * Mert a Dagger csak azokat a kötéseket oldja fel fordítási időben, amelyek egy belépési
 * pont függőségi útvonalán szerepelnek: a nem használt kötések hiányzó függőségei
 * csendben elnézve maradnának. Ez a feladat (T-203) viszont pontosan a gráf *teljességéről*
 * szól, a felhasználóit (ViewModelek) pedig a későbbi feladatok hozzák. Ez az `@EntryPoint`
 * tehát a gráf fordítási idejű bizonyítéka: ha bármelyik kötés – adatbázis, repository,
 * importáló, `Clock`, dispatcher, RNG – hiányzik vagy kétértelmű, az `assembleDebug`
 * elbukik, nem az első olyan képernyő, amelyik először kérné.
 *
 * Mellette a `HiltGraphTest` (app modul, Robolectric) **futásidőben** is felépíti és
 * lekérdezi ezeket a kötéseket, tehát nemcsak a feloldás, hanem a példányosítás is
 * ellenőrzött – emulátor nélkül.
 *
 * Nem helyettesíti a `@HiltViewModel` injektálást: az egyes képernyők a saját
 * konstruktorukon kapják a függőségeiket, nem ezen keresztül. Ez a felület diagnosztika
 * (lásd még FR-45, T-508), nem alkalmazáslogikai hozzáférési út.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface KyokushinGraph {

    fun techniqueRepository(): TechniqueRepository

    fun contentImporter(): ContentImporter

    fun quizGenerator(): QuizGenerator

    fun knowledgeValidator(): KnowledgeValidator

    fun clock(): Clock

    fun random(): Random

    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher

    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher
}
