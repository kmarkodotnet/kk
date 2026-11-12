package hu.kyokushin.trainer.feature.quiz.run

import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizQuestion

/**
 * A futó kvíz képernyő állapota (T-405, T-406, T-407, T-409, T-410; FR-16, FR-18, FR-19,
 * US-15).
 *
 * A [Content] a kérdés-UI (T-407, T-408) és a válasz-visszajelzés (T-409) bemenete –
 * a G-07 csoport a **kérdések közötti állapotgépet** adta (mentett állapot, egyszeri
 * válasz, előrehaladás, megszakítás), a kérdéstípusonkénti megjelenítést és a
 * visszajelzés UI-ját nem (lásd `docs/CONVENTIONS.md` a G-07 lezárásakor felvett sora).
 * A G-08 csoport (T-407/T-409) ezt egy ponton bővíti: a [Content.options] a
 * [QuizQuestion] azonosítóit (ADR-006) oldja fel megjelenítendő adattá – ezt a
 * [QuizRunViewModel] végzi a `TechniqueRepository`-n keresztül, hogy a Compose UI
 * (T-407/T-409) sosem lásson repository-hívást, csak ezt a kész modellt.
 */
sealed interface QuizRunUiState {

    /** A kezdeti bemenet (terv) feloldása még nem futott le. */
    data object Loading : QuizRunUiState

    /**
     * Védelmi háló (NFR-06): a kapott terv kérdéslistája üres. A hívó (`quiz/setup`,
     * T-403 „Kész ha") ezt rendesen nem engedi megtörténni – ez az ág csak arra az
     * esetre védekezik, ha a mentett állapot mégis ilyet hordozna (pl. régi, érvénytelen
     * folyamat-újraindítás).
     */
    data object Empty : QuizRunUiState

    /**
     * Van megjeleníthető kérdés (a [position] a [total]-on belül van).
     *
     * @property sessionId a kvízmenet azonosítója – a válasz rögzítésének kulcsa.
     * @property question az aktuális kérdés, kizárólag azonosítókkal (ADR-006); a
     *   megjelenítendő szöveget/képet a kérdés-UI (T-407/T-408) oldja fel a tudástárból.
     * @property position az aktuális kérdés 0-alapú pozíciója.
     * @property total a terv kérdésszáma.
     * @property answeredCount hány kérdésre érkezett már válasz ebben a menetben – ebből
     *   épül a „`{index}` / `{összes}`" előrehaladásjelző (FUNCTIONAL_SPEC 5.2/1.) UI-ja
     *   (T-410 „Teendők" 1., a tényleges Compose-widget egy későbbi feladat tárgya).
     * @property selectedTechniqueId az aktuális kérdésre már rögzített válasz, vagy
     *   `null`, ha ez a kérdés még megválaszolatlan.
     * @property isLastQuestion igaz, ha ez a terv utolsó kérdése – a „Következő" gomb
     *   ilyenkor az eredményre vezetne (FUNCTIONAL_SPEC 5.2/4.); az eredményképernyőre
     *   lépés maga a G-09 csoport (T-411) tárgya, ezért ebben a csoportban a
     *   [QuizRunViewModel.onNext] az utolsó kérdésen no-op (lásd ott a KDoc-ot).
     * @property closeConfirmationVisible igaz, ha a megszakítást megerősítő párbeszéd
     *   nyitva van (FUNCTIONAL_SPEC 5.2/5., T-410 „Teendők" 2–3.).
     * @property options a [question.optionTechniqueIds][QuizQuestion.optionTechniqueIds]
     *   feloldott megjelenítési adatai, **ugyanabban a sorrendben** (a seedelt keverés
     *   sorrendje, ADR-007 – nem rendezett újra). Pontosan [QuizQuestion.OPTION_COUNT]
     *   elem; a helyes opció adatai a magyarázó kártyához (T-409) ugyanitt vannak, a
     *   [question.correctTechniqueId][QuizQuestion.correctTechniqueId] alapján
     *   kikereshetők.
     */
    data class Content(
        val sessionId: SessionId,
        val question: QuizQuestion,
        val position: Int,
        val total: Int,
        val answeredCount: Int,
        val selectedTechniqueId: TechniqueId?,
        val isLastQuestion: Boolean,
        val closeConfirmationVisible: Boolean,
        val options: List<QuizOptionDisplay>,
    ) : QuizRunUiState
}

/**
 * Egy válaszopció megjelenítendő adatai, a tudástárból feloldva (T-407, T-409).
 *
 * A [QuizQuestion] csak azonosítót hordoz (ADR-006, DR-03): a megjelenítést a
 * [QuizRunViewModel] oldja fel a `TechniqueRepository`-ból, **mielőtt** a Compose UI-hoz
 * kerül – a UI réteg így sosem lát `Technique` lekérdezést vagy repository-függőséget,
 * csak ezt a kész, kis modellt (rétegszabály, ADR-001).
 *
 * @property techniqueId az opció technikaazonosítója – ugyanaz, mint a
 *   [QuizQuestion.optionTechniqueIds] egy eleme.
 * @property romaji a technika romaji neve. (A) típusnál ha ez a helyes opció, ez a
 *   kérdés tárgya; egyébként (és (B) típusnál mindig) az opció felirata.
 * @property hungarian a technika magyar jelentése. (B) típusnál ha ez a helyes opció, ez
 *   a kérdés tárgya; egyébként (és (A) típusnál mindig) az opció felirata.
 * @property illustration a technika képhivatkozása – a (C)/(D) típusok (T-408) bemenete,
 *   az (A)/(B) magyarázó kártyáján (T-409) a helyes megoldás ábrája.
 */
data class QuizOptionDisplay(
    val techniqueId: TechniqueId,
    val romaji: String,
    val hungarian: String,
    val illustration: IllustrationRef?,
)
