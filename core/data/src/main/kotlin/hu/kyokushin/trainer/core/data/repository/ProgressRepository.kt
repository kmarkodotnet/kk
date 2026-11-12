package hu.kyokushin.trainer.core.data.repository

import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.QuizAttempt
import hu.kyokushin.trainer.core.model.QuizSession
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.coroutines.flow.Flow

/**
 * A felhasználói adat **írási és saját-önértékelési** felülete a feature modulok felé
 * (`docs/ARCHITECTURE.md` 5.3, DR-04, DR-07, FR-10, FR-18, FR-19, FR-28, FR-35).
 *
 * Ez a `core/data` publikus szerződése a kvízmunkamenetekre, a próbálkozásokra és az
 * önértékelésekre. A megvalósítás `internal`, és a Hilt köti be – ugyanaz a mintázat, mint
 * a [TechniqueRepository]-nál: a szerződésben **egyetlen** Room típus sem szerepel.
 *
 * ## Mi tartozik ehhez az interfészhez, és mi nem (még)
 *
 * Ez a felület a T-301 (tanulási/önértékelési rész), a T-401 (kvízrész) és a T-502
 * (törlés) tagjait fogja össze egy csoportban. Az aggregált, **olvasó** statisztikai
 * `Flow`-k (`observeOverallProgress`, `observeCategoryProgress`, `observeTechniqueProgress`,
 * `observeNeedsPractice`) az architektúra-vázlatban már szerepelnek, de a T-501 tárgya –
 * ide szándékosan nem kerültek bele, nehogy a felület olyan tagot ígérjen, amihez még
 * nincs `@Query`. A [observeStudyReviewSummary] ugyanilyen jellegű olvasó `Flow`, de az
 * ehhez tartozó adat (`study_review` darabszám besorolásonként) a T-301 hatókörébe esik,
 * ezért már itt megjelenik (FR-28, FUNCTIONAL_SPEC 6.1/5.).
 *
 * A [getSessionResult] is olvasó tag, de **nem** a T-501 aggregált, több-munkamenetes
 * statisztikája (az a `observeOverallProgress`/`observeCategoryProgress`/
 * `observeTechniqueProgress`/`observeNeedsPractice` négyes, még nincs megírva): ez
 * kizárólag **egyetlen, már ismert `SessionId`-jú** menet visszaolvasása, a kvíz
 * eredményképernyőjéhez (FR-20, FR-21, T-411/T-412, G-09 csoport). A kettő nem ütközik:
 * az eredményképernyő a saját menetét nézi, nem a felhasználó teljes előzményét.
 *
 * ## Négy szabály, amit a megvalósításnak be kell tartania
 *
 * 1. **A kvízstatisztika és az önértékelés soha nem keveredik** (FR-28): a
 *    [recordStudyReview] és a [observeStudyReviewSummary] kizárólag a `study_review`
 *    táblát érinti, a [startSession]/[recordAttempt]/[finishSession] kizárólag a
 *    `quiz_session`/`quiz_attempt` táblákat.
 * 2. **Az egyszeri válasz a szerződés része, nem UI-konvenció** (ADR-005): a
 *    [recordAttempt] visszatérési értéke `false`, ha az adott kérdésre ebben a menetben
 *    már volt válasz – ilyenkor az adatbázis állapota **nem** változik.
 * 3. **Minden időbélyeg az injektált `Clock`-ból jön** (NFR-07): a hívó sem a
 *    [startSession] hívásnak, sem a [recordAttempt]/[recordStudyReview] hívásoknak nem ad
 *    át időpontot – a megvalósítás olvassa ki.
 * 4. **A [clearAllProgress] csak a felhasználói táblákat üríti** (FR-35, NFR-09): a
 *    tartalomtáblák és a `content_state` érintetlenek maradnak.
 */
interface ProgressRepository {

    /**
     * Új kvízmenet nyitása (FR-19, ADR-007).
     *
     * @param requestedCount a felhasználó által kért kérdésszám (5 / 10 / 20 – FR-11). Ennél
     *   kevesebb kérdés is generálódhat; ez a menet rekordjában nem változik, a tényleges
     *   terv a [hu.kyokushin.trainer.core.quiz.generator.QuizPlan] felelőssége.
     * @param seed a kvízmotor **injektált** RNG-ből sorsolt magja (ADR-007). A repository
     *   maga soha nem sorsol: a hívó (a `quiz/setup` ViewModel, T-403) adja át, hogy a
     *   menet utólag, bitre reprodukálható legyen.
     * @return az új menet azonosítója. A hívó ezzel tölti fel a `SavedStateHandle`-be
     *   mentett `QuizRunState.sessionId`-t (ARCHITECTURE 6.2).
     */
    suspend fun startSession(requestedCount: Int, seed: Long): SessionId

    /**
     * Egy kvízválasz rögzítése (FR-18, ADR-005).
     *
     * @param attempt az új próbálkozás adatai – lásd [NewAttempt]. Nincs benne `id`
     *   (azt az adatbázis adja) és nincs benne időbélyeg (azt az injektált `Clock` adja).
     * @return `false`, ha a [NewAttempt.questionId]-ra ebben a menetben **már volt**
     *   válasz – ilyenkor a megvalósítás nem ír semmit, a korábbi próbálkozás és a menet
     *   `answeredCount`-ja változatlan marad. `true`, ha a válasz most rögzült, és a menet
     *   megválaszolt kérdésszáma is nőtt.
     */
    suspend fun recordAttempt(attempt: NewAttempt): Boolean

    /**
     * A menet lezárása (FR-19, FR-20).
     *
     * Csak akkor ír, ha a menet még befejezetlen – egy már lezárt menet lezárási
     * időpontja nem íródik át (lásd [hu.kyokushin.trainer.core.data.db.QuizSessionDao]).
     * Ismeretlen `id`-ra és már lezárt menetre is biztonságosan hívható: nincs kivétel,
     * egyszerűen nem történik írás.
     */
    suspend fun finishSession(id: SessionId)

    /**
     * Egy már lezárt vagy megszakított kvízmenet teljes eredménye (FR-20, FR-21, T-411).
     *
     * `null`, ha nincs ilyen azonosítójú menet – a hívó (`quiz/result`, T-411) ezt
     * védelmi hálóként kezeli (NFR-06), nem kivételként: pl. egy sérült vagy törölt
     * `sessionId` (a felhasználó törölte a haladást, miközben az eredményképernyőn volt)
     * esetén.
     *
     * A [QuizSessionResult.attempts] a válaszadás sorrendjében érkezik (lásd
     * [hu.kyokushin.trainer.core.data.db.QuizAttemptDao.getBySession] rendezése); a
     * `UNIQUE(sessionId, questionId)` kényszer miatt (ADR-005) egy kérdésre legfeljebb egy
     * próbálkozás van benne, tehát a hívónak nem kell újra deduplikálnia.
     */
    suspend fun getSessionResult(sessionId: SessionId): QuizSessionResult?

    /**
     * Önértékelés rögzítése egy tanulókártyán (FR-10).
     *
     * Minden hívás **új** `study_review` sort hoz létre – az ismételt önértékelés nem
     * ütközés, hanem a haladás időbeli alakulásának adata (DR-09). A [SelfRating.KNEW] /
     * [SelfRating.NEEDS_PRACTICE] besorolás soha nem hat a kvízstatisztikára (FR-28).
     */
    suspend fun recordStudyReview(techniqueId: TechniqueId, rating: SelfRating)

    /**
     * Hány önértékelés született „Tudtam" és hány „Gyakorolnom kell" besorolással,
     * megfigyelhetően (FR-28, FUNCTIONAL_SPEC 6.1/5.).
     *
     * A Haladás képernyő ebből írja ki: „Tudtam: `{n}` · Gyakorolnom kell: `{m}`" – külön,
     * elhatárolt szakaszban, a kvíz sikerarányától függetlenül.
     */
    fun observeStudyReviewSummary(): Flow<StudyReviewSummary>

    /**
     * Minden felhasználói adat törlése egyetlen tranzakcióban (FR-35, NFR-09).
     *
     * A `quiz_session`, a rá `CASCADE`-elő `quiz_attempt` és a `study_review` tábla ürül.
     * A tartalomtáblák (`category`, `technique`, `technique_alias`, `synonym_group`) és a
     * `content_state` **változatlan** marad: a tudástár törlés után ugyanúgy böngészhető
     * és tanulható, csak a statisztika nullázódik (US-19).
     */
    suspend fun clearAllProgress()
}

/**
 * Egy még nem perzisztált kvízválasz – a [ProgressRepository.recordAttempt] bemenete
 * (DR-04, ADR-005, ARCHITECTURE 5.3: `NewAttempt`).
 *
 * Miért nem a domain [hu.kyokushin.trainer.core.model.QuizAttempt]-et veszi át a
 * [ProgressRepository.recordAttempt]? Mert annak az `id` (Room-generált) és az
 * `answeredAt` (injektált `Clock`-ból) mezője a perzisztálás **eredménye**, nem a hívó
 * bemenete – egy olyan API, amely ezeket is elkérné, hamis választási szabadságot adna
 * (a hívó kitalálhatna egy `id`-t vagy egy `Instant.now()`-t, pontosan azt, amit az
 * NFR-07 tilt).
 *
 * @property sessionId melyik kvízmenethez tartozik a válasz.
 * @property questionId a megválaszolt kérdés azonosítója a menetben. Az egyszeri válasz
 *   kikényszerítésének kulcsa (ADR-005).
 * @property techniqueId a kérdés **helyes** válaszának technikája.
 * @property questionType a kérdés típusa.
 * @property selectedTechniqueId amit a felhasználó **választott**.
 * @property correct a válasz helyes volt-e – ezt a döntést a kvízmotor hozza meg
 *   (szinonimacsoport esetén is), a repository csak megőrzi (DR-09).
 */
data class NewAttempt(
    val sessionId: SessionId,
    val questionId: QuestionId,
    val techniqueId: TechniqueId,
    val questionType: QuestionType,
    val selectedTechniqueId: TechniqueId,
    val correct: Boolean,
)

/**
 * Egy kvízmenet teljes, visszaolvasott eredménye – a [ProgressRepository.getSessionResult]
 * kimenete (FR-20, FR-21, T-411).
 *
 * @property session a menet metaadatai: [hu.kyokushin.trainer.core.model.QuizSession.isFinished]
 *   dönti el, hogy a menet rendesen befejeződött-e, vagy megszakított (FR-19) –
 *   az eredményképernyő ebből választja a „Megszakított kvíz – …" szöveget.
 * @property attempts a menetben rögzített próbálkozások (lásd a metódus KDoc-ja).
 */
data class QuizSessionResult(
    val session: QuizSession,
    val attempts: List<QuizAttempt>,
)

/**
 * A tanulókártyás önértékelések összegzése besorolás szerint (FR-28, T-301).
 *
 * Darabszám, nem technikánkénti utolsó ítélet: minden leadott önértékelés önmagáért
 * számít (lásd [hu.kyokushin.trainer.core.data.db.StudyReviewDao.observeCountByRating]).
 *
 * @property knownCount hány önértékelés volt [SelfRating.KNEW].
 * @property needsPracticeCount hány önértékelés volt [SelfRating.NEEDS_PRACTICE].
 */
data class StudyReviewSummary(
    val knownCount: Int,
    val needsPracticeCount: Int,
) {
    init {
        require(knownCount >= 0) { "StudyReviewSummary.knownCount nem lehet negatív." }
        require(needsPracticeCount >= 0) { "StudyReviewSummary.needsPracticeCount nem lehet negatív." }
    }

    /** A leadott önértékelések összes száma – a Haladás képernyő üres állapotához kell. */
    val totalCount: Int get() = knownCount + needsPracticeCount
}
