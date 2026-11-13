package hu.kyokushin.trainer.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import java.time.Instant

/**
 * A **felhasználói táblák** (DR-04, DR-07): a tanulás és a kvíz eredménye.
 *
 * A fájl egyetlen legfontosabb tulajdonsága az, ami **nincs** benne: a [QuizAttemptEntity]
 * és a [StudyReviewEntity] `techniqueId` oszlopán **nincs idegen kulcs kényszer a
 * `technique` táblára** (ADR-004 3. pont).
 *
 * Miért a „nincs FK" és nem az `onDelete = NO_ACTION`?
 * A `NO_ACTION` is megakadályozná a cascade törlést, de **beszúrási kényszert** adna: egy
 * próbálkozás csak akkor rögzülhetne, ha a technika sora abban a pillanatban létezik. Ez
 * a tartalomfrissítés és az előrehaladás életciklusát (DR-07) újra összekötné – pontosan
 * azt, amit a döntés szét akar választani. FK nélkül a felhasználói sor akkor is érvényes
 * marad, ha a technika visszavont (`retired = true`), átnevezett, vagy egy jövőbeli
 * migráció fizikailag eltávolítja a tartalomtáblából. A hivatkozás a `TechniqueId`-n
 * keresztül **örök** (DR-06), ezért feloldható marad. Ez a TR-05 garancia alapja.
 *
 * A `techniqueId` oszlopokon ezért sima (nem egyedi) index van: a Haladás képernyő
 * technikánkénti aggregált lekérdezései (T-501) ezen futnak.
 *
 * A felhasználói adatok **egymás közötti** hivatkozása más eset: a [QuizAttemptEntity]
 * `sessionId`-ja a `quiz_session` táblára FK, `CASCADE` törléssel. Ez nem a tartalomra
 * mutat, hanem a saját aggregátumgyökerére: egy menet nélküli válasz értelmezhetetlen
 * lenne, és az előrehaladás törlése (FR-28, T-502) így egyetlen `DELETE` lesz.
 */

/**
 * Egy kvízmenet (FR-19, FR-20).
 *
 * Az elsődleges kulcs `TEXT`, nem generált `INTEGER`: a menet azonosítóját a domain adja
 * ([SessionId]), mert a futásállapot és a válaszok már a beszúrás előtt hivatkoznak rá.
 *
 * A [finishedAt] nullázható, és ez nem kényelmi döntés: a megszakított kvíz munkamenete
 * befejezetlen marad (FR-19), így a „befejezetlen" és a „lezárt" eset nem keverhető össze.
 * Az [seed] tárolása az ADR-007 döntése – egy lezárt kvíz utólag bitre reprodukálható.
 */
@Entity(tableName = "quiz_session")
internal data class QuizSessionEntity(
    @PrimaryKey val id: SessionId,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val requestedCount: Int,
    val answeredCount: Int = 0,
    val seed: Long,
)

/**
 * Egy rögzített kvízválasz (DR-04).
 *
 * **A `UNIQUE(sessionId, questionId)` index ennek a táblának a lényege** (ADR-005, FR-18):
 * ez a második védelmi vonal a `QuizRunViewModel` ellenőrzése mögött, és az egyetlen,
 * amely versenyhelyzetben, folyamat-visszaállításnál és hibás UI-állapot mellett is tart.
 * A beszúrás `OnConflictStrategy.IGNORE`-ral megy (lásd [QuizAttemptDao]), így ütközéskor
 * nem kivétel keletkezik, hanem `-1` sorazonosító – ebből lesz a
 * `ProgressRepository.recordAttempt` `false` visszatérése.
 *
 * Az index a **kérdés** azonosítójára épül, nem a technikáéra: ugyanaz a technika egy
 * munkameneten belül elvileg több kérdésben is szerepelhet, ezért a
 * `(sessionId, techniqueId)` pár nem lenne helyes kulcs (ADR-005).
 *
 * A [correct] redundánsnak tűnik a két azonosító mellett, de nem az: szinonimacsoport
 * esetén a helyesség megítélése a kvízmotor dolga, és azt az ítéletet kell megőrizni – a
 * statisztika ezt az oszlopot összegzi, nem számol újra (DR-09).
 */
@Entity(
    tableName = "quiz_attempt",
    foreignKeys = [
        ForeignKey(
            entity = QuizSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            // Felhasználói adat → felhasználói adat. A tartalomra NINCS idegen kulcs.
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        // ADR-005: adatbázis-szintű lehetetlenség a dupla válasz (FR-18).
        Index(value = ["sessionId", "questionId"], unique = true),
        Index(value = ["techniqueId"]),
    ],
)
internal data class QuizAttemptEntity(
    /** Room által generált kulcs; `0` = még nem perzisztált rekord. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: SessionId,
    val questionId: QuestionId,
    val techniqueId: TechniqueId,
    val questionType: QuestionType,
    val selectedTechniqueId: TechniqueId,
    val correct: Boolean,
    val answeredAt: Instant,
)

/**
 * Egy tanulókártyán adott önértékelés (DR-04, FR-14).
 *
 * Minden önértékelés **külön sor**, nem felülírt állapot: a haladás időbeli alakulása csak
 * így olvasható ki, a legutóbbi ítélet pedig a legfrissebb [reviewedAt] alapján adódik
 * (DR-09 – a statisztika levezetett, nem duplikált számláló). Ezért nincs itt
 * `UNIQUE(techniqueId)`: az ismételt önértékelés *kívánt* viselkedés, nem ütközés.
 */
@Entity(
    tableName = "study_review",
    indices = [Index(value = ["techniqueId"])],
)
internal data class StudyReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val techniqueId: TechniqueId,
    val selfRating: SelfRating,
    val reviewedAt: Instant,
)

/**
 * Lekérdezés-projekciók a Haladás képernyő aggregált statisztikáihoz (FR-23–FR-27, T-501).
 *
 * Nem `@Entity`: ezek a sorok soha nem perzisztálódnak, kizárólag a [QuizAttemptDao]
 * aggregált `@Query`-inek kimenetei. Mindegyik `answered`/`correctCount` oszlopa
 * `COUNT(*)`/`COALESCE(SUM(correct), 0)`-ból jön – a „nincs adat vs. 0%" döntést
 * (FR-27) a mapper ([hu.kyokushin.trainer.core.data.db.toScore]) hozza meg, a `ratio`-t
 * soha nem SQL számolja (DR-09: a levezetett érték egyetlen helyen él, lásd
 * `core/quiz` [hu.kyokushin.trainer.core.quiz.scoring.Score]).
 */

/** [hu.kyokushin.trainer.core.data.repository.OverallProgress] forrása. */
internal data class OverallScoreRow(
    val answered: Int,
    val correctCount: Int,
)

/** [hu.kyokushin.trainer.core.data.repository.CategoryProgress] egy sorának forrása. */
internal data class CategoryScoreRow(
    val categoryId: CategoryId,
    val answered: Int,
    val correctCount: Int,
)

/**
 * [hu.kyokushin.trainer.core.data.repository.TechniqueProgress] összesített (nem
 * kérdéstípus szerinti) részének forrása. Egyszerre szolgálja az egyetlen technikára
 * szűrt lekérdezést ([QuizAttemptDao.observeTechniqueScore], ahol a `techniqueId`
 * oszlop a bemeneti paraméter szó szerinti visszaadása) és a „gyakorlást igénylő"
 * listát ([QuizAttemptDao.observeNeedsPracticeScores], ahol valódi `GROUP BY` oszlop) –
 * nem kell két külön, majdnem azonos projekciós típus.
 */
internal data class TechniqueScoreRow(
    val techniqueId: TechniqueId,
    val answered: Int,
    val correctCount: Int,
    val lastPracticedAt: Instant?,
)

/**
 * [hu.kyokushin.trainer.core.data.repository.TechniqueProgress.byQuestionType] egy
 * bejegyzésének forrása. Ugyanaz a kettős felhasználás, mint a [TechniqueScoreRow]-nál:
 * egyetlen technikára ([QuizAttemptDao.observeTechniqueScoreByQuestionType]) és több
 * technikára egyszerre ([QuizAttemptDao.observeQuestionTypeScoresFor]).
 */
internal data class TechniqueQuestionTypeScoreRow(
    val techniqueId: TechniqueId,
    val questionType: QuestionType,
    val answered: Int,
    val correctCount: Int,
)
