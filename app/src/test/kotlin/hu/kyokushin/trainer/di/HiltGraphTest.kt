package hu.kyokushin.trainer.di

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.di.IoDispatcher
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import hu.kyokushin.trainer.core.quiz.generator.DefaultQuizGenerator
import hu.kyokushin.trainer.core.quiz.generator.QuizGenerator
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeValidator
import java.time.Clock
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Az alkalmazásszintű Hilt gráf felépülése (T-203, NFR-03, NFR-07).
 *
 * ## Miért ez a teszt a „Kész, ha: az app elindul, a Hilt gráf felépül" bizonyítéka?
 *
 * Mert a gráfot nem *leírja*, hanem **felépíti**: a [HiltTestApplication] ugyanazt a
 * `SingletonComponent`-et hozza létre, mint a `@HiltAndroidApp` alkalmazásosztály – minden
 * `@InstallIn(SingletonComponent::class)` modullal, ami a classpathon van (`core/data`:
 * `DatabaseModule`, `ContentModule`, `RepositoryModule`; `di`: `TimeModule`,
 * `DispatcherModule`, `QuizModule`). Az `@Inject` mezők feloldása tehát nemcsak a kötések
 * *létezését* igazolja, hanem azt is, hogy a példányok valóban előállnak: a Room adatbázis
 * megnyílik, az assetolvasó megkapja az `AssetManager`-t, a repository és az importáló
 * megépül.
 *
 * Robolectric alatt fut, tehát **emulátor nélkül** is ellenőrzött; az igazi eszközön futó
 * indítást a TR-06 instrumentált tesztek fedik (T-606).
 *
 * A fordítási idejű ellenőrzést ez nem váltja ki, hanem megerősíti: a `KyokushinGraph`
 * belépési pont miatt egy hiányzó kötés már az `assembleDebug`-ot is elbuktatja.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HiltTestApplication::class)
class HiltGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var techniqueRepository: TechniqueRepository

    @Inject
    lateinit var progressRepository: ProgressRepository

    @Inject
    lateinit var contentImporter: ContentImporter

    @Inject
    lateinit var quizGenerator: QuizGenerator

    @Inject
    lateinit var knowledgeValidator: KnowledgeValidator

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var random: Random

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Before
    fun injectGraph() {
        hiltRule.inject()
    }

    /**
     * A repository és a tartalomimportáló a Room-implementációra kötve áll elő – a UI pedig
     * csak az interfészt látja (NFR-03, ADR-001 L4).
     */
    @Test
    fun hiltGraph_providesRepositoryAndImporterBehindInterfaces_NFR03() {
        assertThat(techniqueRepository).isNotNull()
        assertThat(progressRepository).isNotNull()
        assertThat(contentImporter).isNotNull()

        // A konkrét típus a `core/data` modulon belül marad: a név ellenőrzése itt azt
        // igazolja, hogy a `@Binds` a Room-implementációra mutat, miközben a tesztnek
        // (mint minden `core/data`-n kívüli kódnak) az osztály nem is importálható.
        assertThat(techniqueRepository.javaClass.simpleName).isEqualTo("RoomTechniqueRepository")
        assertThat(progressRepository.javaClass.simpleName).isEqualTo("RoomProgressRepository")
        assertThat(contentImporter.javaClass.simpleName).isEqualTo("RoomContentImporter")
    }

    /** A kvízmotor és a validátor a `core/quiz` implementációira kötve (ARCHITECTURE 7.). */
    @Test
    fun hiltGraph_providesQuizEngineBindings_NFR03() {
        assertThat(quizGenerator).isSameInstanceAs(DefaultQuizGenerator)
        assertThat(knowledgeValidator).isInstanceOf(DefaultKnowledgeValidator::class.java)
    }

    /**
     * `Clock` és RNG injektálva, nem a hívás helyén példányosítva (NFR-07).
     *
     * Az óra UTC zónájú: a tárolt időbélyegek zónamentes `Instant`-ok (DR-05).
     */
    @Test
    fun hiltGraph_providesInjectedClockAndRandom_NFR07() {
        assertThat(clock.zone).isEqualTo(ZoneOffset.UTC)
        assertThat(clock.instant()).isNotNull()

        // Az RNG-ből valóban húzható seed – ez a kvízmunkamenet seedjének forrása (ADR-007).
        val firstSeeds = List(5) { random.nextLong() }
        assertThat(firstSeeds.toSet().size).isGreaterThan(1)
    }

    /**
     * A két dispatcher-qualifier két **különböző** készletre mutat (NFR-07).
     *
     * Ha ugyanarra mutatnának, a CPU-munka a blokkoló I/O szálain futna, és a qualifierek
     * léte hamis biztonságot adna.
     */
    @Test
    fun hiltGraph_bindsDistinctIoAndDefaultDispatchers_NFR07() {
        assertThat(ioDispatcher).isSameInstanceAs(Dispatchers.IO)
        assertThat(defaultDispatcher).isSameInstanceAs(Dispatchers.Default)
        assertThat(ioDispatcher).isNotSameInstanceAs(defaultDispatcher)
    }
}
