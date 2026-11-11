package hu.kyokushin.trainer.core.data.content

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.QuizAttemptEntity
import hu.kyokushin.trainer.core.data.db.QuizSessionEntity
import hu.kyokushin.trainer.core.data.db.StudyReviewEntity
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **TR-05: a tudástár frissítése nem viszi el az előrehaladást** (FR-33, DR-06, DR-07,
 * DR-08).
 *
 * Ez a projekt egyik legfontosabb garanciája, ezért a mérése nem a megvalósítás
 * szerkezetére épül, hanem a megfigyelhető viselkedésre: *valódi* (in-memory) Room
 * adatbázisba betöltünk egy kiadást, felveszünk előrehaladást, majd betöltjük a következő
 * kiadást – és utána **összevetjük** a felhasználói sorokat és a belőlük számolt
 * statisztikát a frissítés előtti állapottal.
 *
 * A T-117 `ContentImporterTest` a betöltés *mechanikáját* mondja ki (upsert, verziókapu,
 * visszavonás, elutasítás). Ez az osztály a *következményt*: egyetlen felhasználói sor, és
 * egyetlen levezetett szám sem változhat attól, hogy a tartalom frissült.
 *
 * ## Miért nem a `ProgressRepository` statisztikáját hívja
 *
 * Az aggregált lekérdezések a T-501 feladat tárgya; azok még nem léteznek. A statisztikát
 * ezért a teszt maga számolja ki a tárolt sorokból ([snapshot]) – ugyanabból az adatból,
 * amiből a későbbi lekérdezések is fognak (DR-09: a statisztika levezetett, nem
 * duplikált számláló). A garancia így a *tárolt tényekre* vonatkozik, és nem romlik el
 * attól, ha a lekérdezések megírása később máshogy alakul.
 *
 * Robolectric kell hozzá (a Room `SupportSQLite` Android API), az óra fix és a dispatcher
 * teszt-dispatcher: a determinizmus feltétele, hogy egyik se a rendszertől jöjjön (NFR-07).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentUpdateProgressTest {

    private lateinit var database: KyokushinDatabase

    private val clock = Clock.fixed(Instant.ofEpochMilli(1_762_000_000_000L), ZoneOffset.UTC)

    private val zenkutsu = TechniqueId.of("KY-0001")
    private val kokutsu = TechniqueId.of("KY-0002")
    private val kiba = TechniqueId.of("KY-0003")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KyokushinDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- 1. A próbálkozások és a számolt statisztika változatlansága ---------------

    /**
     * A TR-05 fő esete: v1 betöltés → előrehaladás → v2 betöltés.
     *
     * A v2 kiadás mindhárom lehetséges tartalmi mozgást elvégzi egyszerre, mert a garancia
     * éppen a vegyes esetre kell: egy technika átnevezve, egy kimarad (visszavonás), egy új
     * jön be. Az elvárás ettől függetlenül ugyanaz – a felhasználói sorok **bitre
     * azonosak**, és a belőlük számolt statisztika is.
     */
    @Test
    fun contentUpdate_keepsEveryAttemptAndTheComputedStatisticsUnchanged_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val before = snapshot()

        val result = importer(version2()).importIfNeeded()

        assertThat(result).isInstanceOf(ImportResult.Imported::class.java)
        assertThat(database.contentStateDao().get()!!.contentVersion).isEqualTo("2.0.0")

        val after = snapshot()
        // Nem csak a darabszám: a teljes sorhalmaz, a megítélt helyességgel és az
        // időbélyegekkel együtt. Egy „statisztikát újraszámoló" frissítés itt bukna el.
        assertThat(after).isEqualTo(before)
        assertThat(after.attempts).hasSize(5)
        assertThat(after.reviews).hasSize(4)
        assertThat(after.overallCorrect).isEqualTo(3)
        assertThat(after.accuracyPercent).isEqualTo(60)
    }

    /**
     * Ugyanaz, több egymást követő frissítésen át: v1 → v2 → v3.
     *
     * Egyetlen frissítés még elrejthetne egy olyan hibát, ami csak a *második* körben
     * ütne be – például ha a visszavonás vagy a `content_state` írása halmozódva nyúlna a
     * felhasználói sorokhoz.
     */
    @Test
    fun contentUpdate_progressSurvivesSeveralConsecutiveVersions_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val before = snapshot()

        importer(version2()).importIfNeeded()
        importer(version3()).importIfNeeded()

        assertThat(database.contentStateDao().get()!!.contentVersion).isEqualTo("3.0.0")
        assertThat(snapshot()).isEqualTo(before)
    }

    /**
     * Az önértékelések (`study_review`) ugyanúgy védettek, mint a kvízválaszok.
     *
     * A legutóbbi ítélet a legfrissebb `reviewedAt` alapján adódik (DR-09), ezért a
     * frissítés utáni állapotot *ezen* a levezetésen is mérjük: a „gyakorlást igénylő"
     * lista (FR-24) ettől a számtól függ majd.
     */
    @Test
    fun contentUpdate_keepsStudyReviewsAndTheLatestSelfRating_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()

        importer(version2()).importIfNeeded()

        assertThat(database.studyReviewDao().count()).isEqualTo(4)
        assertThat(latestRating(zenkutsu)).isEqualTo(SelfRating.KNEW)
        // A legfrissebb önértékelés nyer, nem az első (két sor van erre a technikára).
        assertThat(latestRating(kokutsu)).isEqualTo(SelfRating.NEEDS_PRACTICE)
    }

    // --- 2. Átnevezett technika: az `id` marad, az előrehaladás követi (DR-06) -----

    /**
     * Átnevezés: a `romaji`, a magyar jelentés és az aliasok cserélődnek, az `id` nem.
     *
     * A technikára hivatkozó próbálkozások ezért **az új néven** oldódnak fel: a Haladás
     * képernyő a frissítés után az új nevet mutatja ugyanazzal a statisztikával. Ez az
     * azonosítóstabilitás (DR-06) megfigyelhető következménye.
     */
    @Test
    fun contentUpdate_renamedTechniqueKeepsItsIdAndProgressFollowsTheNewName_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val attemptsBefore = database.quizAttemptDao().getByTechnique(zenkutsu)

        importer(version2()).importIfNeeded()

        // 1. Az `id` ugyanaz, új sor nem keletkezett az átnevezéstől.
        assertThat(database.techniqueDao().getAllIds()).contains(zenkutsu)
        val renamed = database.techniqueDao().getById(zenkutsu)!!
        assertThat(renamed.romaji).isEqualTo(RENAMED_ROMAJI)
        assertThat(renamed.hungarian).isEqualTo(RENAMED_HUNGARIAN)
        assertThat(renamed.datasetVersion).isEqualTo("2.0.0")
        assertThat(renamed.retired).isFalse()

        // 2. Az előrehaladás ugyanazokon a sorokon, változatlanul – az `id`-t követi.
        assertThat(database.quizAttemptDao().getByTechnique(zenkutsu)).isEqualTo(attemptsBefore)
        assertThat(attemptsBefore).hasSize(2)

        // 3. És az új nevet mutatjuk hozzá: a próbálkozás feloldása az új rekordot adja.
        val resolved = database.techniqueDao().getById(attemptsBefore.first().techniqueId)!!
        assertThat(resolved.romaji).isEqualTo(RENAMED_ROMAJI)
        // Az átnevezéssel eldobott régi alias nem él túl, de ez tartalom, nem előrehaladás.
        assertThat(database.techniqueAliasDao().getForTechnique(zenkutsu).map { it.alias })
            .containsExactly(RENAMED_ALIAS)
    }

    // --- 3. Visszavont technika: a hivatkozás feloldható marad (ADR-004) ----------

    /**
     * A csomagból **kimaradt** technika visszavonása.
     *
     * Ez a fontosabb eset a kettő közül: a tartalom szerkesztője nem jelezte a kivezetést,
     * egyszerűen nincs ott a rekord. Az importáló ekkor is jelöl, nem töröl – a sor
     * megmarad, a próbálkozás feloldható, a technika viszont kikerül az aktív listából.
     */
    @Test
    fun contentUpdate_techniqueMissingFromTheNewVersionIsRetiredButStaysResolvable_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val attemptsBefore = database.quizAttemptDao().getByTechnique(kiba)
        assertThat(attemptsBefore).hasSize(1)

        val result = importer(version2()).importIfNeeded()

        assertThat((result as ImportResult.Imported).retiredCount).isEqualTo(1)
        val retired = database.techniqueDao().getById(kiba)!!
        assertThat(retired.retired).isTrue()
        // A régi nevét megtartja: a feloldott hivatkozásnak meg kell tudni jelenni.
        assertThat(retired.romaji).isEqualTo("Kiba dachi")

        // Az aktív tananyagból kiesett, a hivatkozása viszont él.
        assertThat(database.techniqueDao().observeActive().first().map { it.id }).doesNotContain(kiba)
        assertThat(database.quizAttemptDao().getByTechnique(kiba)).isEqualTo(attemptsBefore)
        assertThat(database.studyReviewDao().getForTechnique(kiba)).hasSize(1)
    }

    /**
     * A tartalom által **kimondottan** visszavontnak jelölt technika (`retired: true`).
     *
     * Elfogadott rekord marad – tehát nem a `retireMissing` ágon, hanem az upsert
     * oszlopértékén keresztül lesz visszavont –, és a hozzá tartozó előrehaladás ugyanúgy
     * sértetlen.
     */
    @Test
    fun contentUpdate_techniqueDeclaredRetiredByTheContentKeepsItsProgress_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val before = snapshot()

        val result = importer(
            source(
                version = "2.0.0",
                entries = listOf(
                    KnowledgeSourceFixtures.entry(id = "KY-0001"),
                    kokutsuEntry(),
                    kibaEntry(retired = true),
                ),
            ),
        ).importIfNeeded()

        // Elfogadott rekord, nem visszavonás a kimaradás miatt.
        assertThat((result as ImportResult.Imported).acceptedCount).isEqualTo(3)
        assertThat(result.retiredCount).isEqualTo(0)
        assertThat(database.techniqueDao().getById(kiba)!!.retired).isTrue()
        assertThat(snapshot()).isEqualTo(before)
    }

    /**
     * A visszavonás nem végleges: a következő kiadásban visszatérő technika újra aktív, és
     * a *teljes* korábbi előrehaladása ott van alatta.
     *
     * Ez az, amiért a visszavonás jelölés és nem törlés (ADR-004): egy tartalmi hiba
     * javítható, anélkül hogy a felhasználó hónapok gyakorlását elvesztené.
     */
    @Test
    fun contentUpdate_retiredTechniqueComingBackKeepsItsEarlierProgress_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val before = snapshot()
        importer(version2()).importIfNeeded()
        assertThat(database.techniqueDao().getById(kiba)!!.retired).isTrue()

        importer(version3()).importIfNeeded()

        assertThat(database.techniqueDao().getById(kiba)!!.retired).isFalse()
        assertThat(database.techniqueDao().observeActive().first().map { it.id }).contains(kiba)
        assertThat(snapshot()).isEqualTo(before)
    }

    // --- 4. Idempotencia: ugyanaz a verzió kétszer → nincs duplikáció (FR-30) ------

    /**
     * Ugyanaz a kiadás kétszer betöltve: se tartalmi, se felhasználói duplikáció.
     *
     * Új importálópéldányokkal, mert a verziókapunak a **tárolt** állapotból kell döntenie,
     * nem a processz memóriájából – különben az app minden indulásánál újraírná a
     * tartalmat.
     */
    @Test
    fun contentUpdate_sameVersionImportedTwice_duplicatesNothing_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val before = snapshot()
        val techniquesBefore = database.techniqueDao().getAll()
        val aliasesBefore = database.techniqueAliasDao().count()
        val synonymsBefore = database.synonymGroupDao().count()

        val second = importer(version1())
        val result = second.importIfNeeded()

        assertThat(result).isEqualTo(ImportResult.UpToDate("1.0.0"))
        assertThat(second.lastValidationReport()).isNull()
        assertThat(database.techniqueDao().getAll()).isEqualTo(techniquesBefore)
        assertThat(database.categoryDao().count()).isEqualTo(1)
        assertThat(database.techniqueAliasDao().count()).isEqualTo(aliasesBefore)
        assertThat(database.synonymGroupDao().count()).isEqualTo(synonymsBefore)
        assertThat(snapshot()).isEqualTo(before)
    }

    /**
     * Idempotencia a **frissítés után** is: a v2 kiadás harmadszori betöltése sem duplikál.
     *
     * A felvitel ilyenkor már tényleg lefutott egyszer, tehát a kapu zárva van – de ha
     * nyitva lenne, az upsert miatt a tartalom akkor is helyes maradna. Ezért a tesztben a
     * *mennyiségek* a mérték, nem a visszatérési ág.
     */
    @Test
    fun contentUpdate_repeatedImportOfTheUpdatedVersionDuplicatesNothing_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        importer(version2()).importIfNeeded()
        val before = snapshot()
        val techniquesAfterUpdate = database.techniqueDao().getAll()

        assertThat(importer(version2()).importIfNeeded()).isEqualTo(ImportResult.UpToDate("2.0.0"))
        assertThat(importer(version2()).importIfNeeded()).isEqualTo(ImportResult.UpToDate("2.0.0"))

        assertThat(database.techniqueDao().getAll()).isEqualTo(techniquesAfterUpdate)
        // Négy sor: a három aktív rekord és a visszavont `KY-0003` – a visszavonás nem törlés.
        assertThat(database.techniqueDao().count()).isEqualTo(4)
        assertThat(database.techniqueDao().observeActive().first()).hasSize(3)
        assertThat(database.techniqueAliasDao().getForTechnique(zenkutsu).map { it.alias })
            .containsExactly(RENAMED_ALIAS)
        assertThat(snapshot()).isEqualTo(before)
    }

    // --- 5. Az elutasított frissítés sem nyúl az előrehaladáshoz (FR-31 + FR-33) ---

    /**
     * A legrosszabb eset: a frissítés **hibás** csomaggal érkezik.
     *
     * A tartalom ekkor a korábbi kiadásé marad (FR-31) – és az előrehaladás ugyanúgy
     * sértetlen. A két garancia egymástól független, de a felhasználó szempontjából
     * együtt jelent használható appot.
     */
    @Test
    fun contentUpdate_rejectedDatasetKeepsBothContentAndProgress_TR05() = runTest {
        importer(version1()).importIfNeeded()
        recordProgress()
        val before = snapshot()
        val techniquesBefore = database.techniqueDao().getAll()

        val result = importer(source(version = "2.0.0", schemaVersion = "9.0.0")).importIfNeeded()

        assertThat((result as ImportResult.Rejected).report.issues.single().code)
            .isEqualTo(IssueCode.V001)
        assertThat(database.techniqueDao().getAll()).isEqualTo(techniquesBefore)
        assertThat(database.contentStateDao().get()!!.contentVersion).isEqualTo("1.0.0")
        assertThat(snapshot()).isEqualTo(before)
    }

    // --- Kiadások -----------------------------------------------------------------

    /** Az alap kiadás: három valódi Kyokushin állás. */
    private fun version1() = source(
        version = "1.0.0",
        entries = listOf(KnowledgeSourceFixtures.entry(id = "KY-0001"), kokutsuEntry(), kibaEntry()),
    )

    /**
     * A frissítés: `KY-0001` átnevezve (ugyanaz az `id`), `KY-0003` kimarad (visszavonás),
     * `KY-0004` új technikaként bejön.
     */
    private fun version2() = source(
        version = "2.0.0",
        entries = listOf(
            KnowledgeSourceFixtures.entry(
                id = "KY-0001",
                romaji = RENAMED_ROMAJI,
                hungarian = RENAMED_HUNGARIAN,
                aliases = listOf(RENAMED_ALIAS),
            ),
            kokutsuEntry(),
            KnowledgeSourceFixtures.entry(
                id = "KY-0004",
                romaji = "Sanchin dachi",
                hungarian = "Homokóra állás",
                aliases = emptyList(),
            ),
        ),
    )

    /** A harmadik kiadás: a v2 tartalma, és a visszavont `KY-0003` visszatér. */
    private fun version3() = source(
        version = "3.0.0",
        entries = listOf(
            KnowledgeSourceFixtures.entry(
                id = "KY-0001",
                romaji = RENAMED_ROMAJI,
                hungarian = RENAMED_HUNGARIAN,
                aliases = listOf(RENAMED_ALIAS),
            ),
            kokutsuEntry(),
            kibaEntry(),
            KnowledgeSourceFixtures.entry(
                id = "KY-0004",
                romaji = "Sanchin dachi",
                hungarian = "Homokóra állás",
                aliases = emptyList(),
            ),
        ),
    )

    private fun kokutsuEntry() = KnowledgeSourceFixtures.entry(
        id = "KY-0002",
        romaji = "Kokutsu dachi",
        hungarian = "Hátul álló állás",
        aliases = emptyList(),
    )

    private fun kibaEntry(retired: Boolean = false) = KnowledgeSourceFixtures.entry(
        id = "KY-0003",
        romaji = "Kiba dachi",
        hungarian = "Lovagló állás",
        aliases = emptyList(),
        retired = retired,
    )

    private fun source(
        version: String,
        schemaVersion: String = "1.0.0",
        entries: List<String> = KnowledgeSourceFixtures.defaultEntries,
    ) = KnowledgeSourceFixtures.source(version, schemaVersion, entries)

    private fun importer(source: KnowledgeAssetSource): ContentImporter = RoomContentImporter(
        database = database,
        reader = KnowledgeContentReader(source),
        validator = DefaultKnowledgeValidator(),
        clock = clock,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    // --- Előrehaladás és statisztika ----------------------------------------------

    /**
     * Két kvízmenet és három önértékelés – a frissítés előtti „élettörténet".
     *
     * Szándékosan vegyes: helyes és helytelen válasz, két menet, ismételt önértékelés,
     * és mindhárom technika érintett (az is, amelyik a frissítésben kimarad). Egy
     * egytechnikás, egyválaszos felvétel nem tudná megmutatni, hogy a *per technika*
     * számolt statisztika is változatlan.
     */
    private suspend fun recordProgress() {
        startSession(SESSION_1, requestedCount = 3)
        attempt(SESSION_1, "q-1", zenkutsu, correct = true)
        attempt(SESSION_1, "q-2", kokutsu, correct = false)
        attempt(SESSION_1, "q-3", kiba, correct = true)

        startSession(SESSION_2, requestedCount = 2)
        attempt(SESSION_2, "q-1", zenkutsu, correct = true)
        attempt(SESSION_2, "q-2", kokutsu, correct = false)

        review(zenkutsu, SelfRating.KNEW, plusSeconds = 0)
        review(kokutsu, SelfRating.KNEW, plusSeconds = 10)
        // Ugyanarra a technikára későbbi, rosszabb önértékelés: a frissebb ítélet számít.
        review(kokutsu, SelfRating.NEEDS_PRACTICE, plusSeconds = 20)
        // A frissítésben kimaradó technikára is van önértékelés: a visszavonás ezt sem viszi el.
        review(kiba, SelfRating.NEEDS_PRACTICE, plusSeconds = 30)
    }

    private suspend fun startSession(id: SessionId, requestedCount: Int) {
        database.quizSessionDao().insert(
            QuizSessionEntity(
                id = id,
                startedAt = clock.instant(),
                requestedCount = requestedCount,
                seed = 42L,
            ),
        )
    }

    private suspend fun attempt(
        sessionId: SessionId,
        questionId: String,
        techniqueId: TechniqueId,
        correct: Boolean,
    ) {
        val inserted = database.quizAttemptDao().insertIgnoringDuplicate(
            QuizAttemptEntity(
                sessionId = sessionId,
                questionId = QuestionId.of(questionId),
                techniqueId = techniqueId,
                questionType = QuestionType.JP_TO_HU,
                // A kiválasztott technika a helyesség szerint: helytelen válasznál másik.
                selectedTechniqueId = if (correct) techniqueId else kokutsu.takeIf { it != techniqueId } ?: zenkutsu,
                correct = correct,
                answeredAt = clock.instant(),
            ),
        )
        assertThat(inserted).isNotEqualTo(-1L)
        database.quizSessionDao().incrementAnsweredCount(sessionId)
    }

    private suspend fun review(techniqueId: TechniqueId, rating: SelfRating, plusSeconds: Long) {
        database.studyReviewDao().insert(
            StudyReviewEntity(
                techniqueId = techniqueId,
                selfRating = rating,
                reviewedAt = clock.instant().plusSeconds(plusSeconds),
            ),
        )
    }

    /**
     * A frissítés előtti/utáni összevetés egyetlen értéke: a nyers felhasználói sorok **és**
     * a belőlük levezetett statisztika.
     *
     * A nyers sorok azért vannak benne, mert az összegek még egy sorcseréltünk esetén is
     * egyezhetnének; a levezetett számok pedig azért, mert a TR-05 szövege kifejezetten a
     * *statisztika* változatlanságát kéri.
     */
    private suspend fun snapshot(): ProgressSnapshot {
        val attempts = listOf(SESSION_1, SESSION_2).flatMap { database.quizAttemptDao().getBySession(it) }
        val reviews = listOf(zenkutsu, kokutsu, kiba).flatMap { database.studyReviewDao().getForTechnique(it) }
        return ProgressSnapshot(
            attempts = attempts,
            reviews = reviews,
            overallCorrect = attempts.count { it.correct },
            accuracyPercent = if (attempts.isEmpty()) 0 else attempts.count { it.correct } * 100 / attempts.size,
            perTechnique = attempts.groupBy { it.techniqueId }.mapValues { (_, rows) ->
                TechniqueStatistics(
                    attemptCount = rows.size,
                    correctCount = rows.count { it.correct },
                    latestRating = latestRating(rows.first().techniqueId),
                )
            },
            answeredCounts = listOf(SESSION_1, SESSION_2)
                .associateWith { database.quizSessionDao().getById(it)!!.answeredCount },
        )
    }

    /** A legutóbbi önértékelés – a legfrissebb `reviewedAt` dönt (DR-09). */
    private suspend fun latestRating(techniqueId: TechniqueId): SelfRating? =
        database.studyReviewDao().getForTechnique(techniqueId).firstOrNull()?.selfRating

    /** A felhasználói előrehaladás teljes, összevethető képe egy adott pillanatban. */
    private data class ProgressSnapshot(
        val attempts: List<QuizAttemptEntity>,
        val reviews: List<StudyReviewEntity>,
        val overallCorrect: Int,
        val accuracyPercent: Int,
        val perTechnique: Map<TechniqueId, TechniqueStatistics>,
        val answeredCounts: Map<SessionId, Int>,
    )

    /** Egy technika levezetett statisztikája – amit a Haladás képernyő mutatni fog (FR-23). */
    private data class TechniqueStatistics(
        val attemptCount: Int,
        val correctCount: Int,
        val latestRating: SelfRating?,
    )

    private companion object {
        val SESSION_1: SessionId = SessionId.of("session-1")
        val SESSION_2: SessionId = SessionId.of("session-2")

        /** Az átnevezés: kötőjeles átírás és javított magyar jelentés, ugyanazon az `id`-n. */
        const val RENAMED_ROMAJI: String = "Zenkutsu-dachi"
        const val RENAMED_HUNGARIAN: String = "Előre álló állás"
        const val RENAMED_ALIAS: String = "Zenkucu-dachi"
    }
}
