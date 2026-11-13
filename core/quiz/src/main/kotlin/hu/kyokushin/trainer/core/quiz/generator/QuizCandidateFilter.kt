package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.quiz.validation.QuizEligibility

/**
 * A kvízgenerátor **jelöltszűrése**: melyik technika melyik kérdéstípusban használható
 * (FR-11, FR-13, FR-14, CR-05, TR-03).
 *
 * Ez a lépés a generálás első fele: a kategóriaszűrő és az ADR-010 jogosultsági kapu
 * alkalmazása, a típusonkénti jelöltszám kiszámítása, és ebből a kihagyott típusok
 * [SkipReason]-ja. A helyes válaszhoz **relatív** döntések – a distraktorválasztás és a
 * szinonimakizárás – a [DistractorSelector]-ban élnek (T-109); a típusonkénti kérdésszám
 * azt kérdezi meg, hogy ne becslés legyen. A seedelt keverés (T-110) és a készenléti
 * összegzés (T-111) erre a kettőre épül.
 *
 * **Egyetlen jogosultsági formula.** A kvízképességet itt nem fogalmazzuk meg újra:
 * minden döntés a [QuizEligibility] kapun megy át (ADR-010). A validátor V007/V008
 * jelentése ugyanazt a függvényt használja, így a jelentés és a tényleges viselkedés nem
 * tud elválni egymástól. Ennek a típusnak a feladata a kapu **alkalmazása** a kérés
 * szűrőivel együtt, nem a kapu kiegészítése.
 *
 * **Determinizmus.** Minden visszaadott lista a technikaazonosító szerint rendezett,
 * `Set`-iterációs sorrendtől független (NFR-07, ADR-007). A szűrés önmagában nem sorsol:
 * a seedelt RNG csak a kiválasztásnál és a keverésnél jön képbe (T-110).
 */
object QuizCandidateFilter {
    /**
     * A kérés kategóriaszűrőjén átjutó technikák, azonosító szerint rendezve (FR-11).
     *
     * Üres [QuizRequest.categoryFilter] jelentése **minden kategória** – ez a beállító
     * képernyő kijelölésmentes állapota, nem „semmi". A jogosultsági kapu itt még nem
     * fut le: a kategóriaszűrés tartalmi hatókör, a kvízképesség típusonkénti kérdés.
     */
    fun inCategoryScope(request: QuizRequest): List<Technique> =
        request.candidates.techniques
            .filter { request.allCategories || it.categoryId in request.categoryFilter }
            .sortedBy { it.id.value }

    /**
     * Azok a technikák, amelyek az adott típusban **helyes válaszként** szerepelhetnek.
     *
     * A kapu típusonként más mezőket követel meg (`QuizEligibility.hasFieldsFor`):
     * az (A)/(B) típushoz romaji és magyar név, a (C)/(D) típushoz ezen felül validált,
     * `quizUsable = true` [hu.kyokushin.trainer.core.model.IllustrationRef.Asset] kép.
     * A feliratos kép (`quizUsable = false`) és a `Placeholder` tehát sem helyesként, sem
     * distraktorként nem jöhet szóba (CR-05, ADR-003, ADR-011).
     */
    fun correctAnswerPool(request: QuizRequest, questionType: QuestionType): List<Technique> =
        inCategoryScope(request)
            .filter { QuizEligibility.isEligible(it, questionType, request.datasetTrust) }

    /**
     * Azok a technikák, amelyek az adott típusban **opcióként** (helyesként vagy
     * distraktorként) megjeleníthetők.
     *
     * A kérdés és az opciók oldala nem ugyanazt követeli meg, ezért két külön készlet:
     *
     * | Típus | Kérdés oldala | Opciók oldala |
     * |---|---|---|
     * | (A) `JP_TO_HU` | romaji név | magyar nevek |
     * | (B) `HU_TO_JP` | magyar név | romaji nevek |
     * | (C) `IMAGE_TO_JP` | **illusztráció** | romaji nevek |
     * | (D) `JP_TO_IMAGE` | romaji név | **illusztrációk** |
     *
     * A (C) típusnál tehát a distraktornak **nem** kell képe legyen – csak a helyesnek –,
     * a (D) típusnál viszont mindegyik opciónak kvízképes képe kell legyen. Ezt a
     * különbséget az [optionRequirement] mezőkövetelménye hordozza, nem egy második
     * jogosultsági szabály.
     */
    fun optionPool(request: QuizRequest, questionType: QuestionType): List<Technique> =
        correctAnswerPool(request, optionRequirement(questionType))

    /**
     * Melyik kérdéstípus mezőkövetelményét kell az **opcióknak** teljesíteniük.
     *
     * A (C) `IMAGE_TO_JP` opciói romaji nevek, ezért a szöveges követelményt kapják: az
     * illusztráció a kérdés oldalán van, nem a válaszokon. A többi típusnál az opciók
     * követelménye megegyezik a kérdés típusáéval.
     */
    fun optionRequirement(questionType: QuestionType): QuestionType =
        when (questionType) {
            QuestionType.IMAGE_TO_JP -> QuestionType.JP_TO_HU
            QuestionType.JP_TO_HU, QuestionType.HU_TO_JP, QuestionType.JP_TO_IMAGE -> questionType
        }

    /**
     * Hány teljes értékű, négyopciós kérdés állítható össze az adott típusból.
     *
     * Annyi, ahány helyes jelölt mellé **tényleg** kijön három elfogadható distraktor
     * ([DistractorSelector.buildableCorrectAnswers]) – a motor ugyanazt a helyes technikát
     * nem ismétli egy kvízen belül, ezért a helyes jelöltek száma a felső korlát.
     *
     * Ez a szám a T-109 óta nem becslés: a V006 szinonimakizárás (CR-07) és a (D)
     * `JP_TO_IMAGE` képazonossági szabálya is beszámít. Négy különböző, egymást ki nem
     * záró opció nélkül a kérdés nem jön létre – három opciós kérdés nincs (FR-14).
     */
    fun buildableQuestionCount(request: QuizRequest, questionType: QuestionType): Int {
        if (questionType !in request.enabledTypes) return 0
        return DistractorSelector.buildableCorrectAnswers(request, questionType).size
    }

    /**
     * Típusonkénti kérdésszám **minden** kérdéstípusra, a letiltottakkal együtt (ott 0).
     *
     * A teljes kép a diagnosztikához és a tartalomellenőrzéshez: a `dryRun` készenléti
     * összegzése ennél szűkebb képet ad – csak a nem kihagyott típusokat –, és azt a
     * [DefaultQuizGenerator] a `generate`-tel közös slotlistából számolja, hogy a két ág
     * ne tudjon elválni (T-111).
     */
    fun buildableQuestionCounts(request: QuizRequest): Map<QuestionType, Int> =
        QuestionType.entries.associateWith { buildableQuestionCount(request, it) }

    /**
     * Miért maradt ki egy-egy kérdéstípus a tervből (FR-22, TR-03).
     *
     * Csak a **kihagyott** típusok szerepelnek az eredményben; amelyikből kérdés
     * állítható össze, az nincs benne. A sorrend a [QuestionType] deklarációs sorrendje,
     * hogy a kimenet determinisztikus legyen.
     *
     * Az okok elhatárolása:
     * - [SkipReason.TYPE_DISABLED]: a felhasználó nem engedélyezte (FR-12). Ez előbbre
     *   való minden tartalmi oknál – a letiltott típusról nem állítunk adathiányt.
     * - [SkipReason.NOT_ENOUGH_IMAGES]: képes típus, és nincs elég validált, kvízben
     *   használható, vizuálisan különböző illusztráció (FR-13, CR-05).
     * - [SkipReason.NOT_ENOUGH_CANDIDATES]: nincs elég kvízképes technika a négy
     *   opcióhoz, vagy egyetlen helyes jelölt sem jutott át a kapun (FR-14).
     */
    fun skipReasons(request: QuizRequest): Map<QuestionType, SkipReason> =
        QuestionType.entries.mapNotNull { type ->
            skipReason(request, type)?.let { type to it }
        }.toMap()

    /** Egyetlen típus kihagyási oka, vagy `null`, ha a típusból áll össze kérdés. */
    fun skipReason(request: QuizRequest, questionType: QuestionType): SkipReason? {
        if (questionType !in request.enabledTypes) return SkipReason.TYPE_DISABLED
        if (buildableQuestionCount(request, questionType) > 0) return null
        val imageSideMissing = when (questionType) {
            // (C): a kérdés képe a helyes válaszé – ha nincs kvízképes kép, az képhiány.
            QuestionType.IMAGE_TO_JP -> correctAnswerPool(request, questionType).isEmpty()
            // (D): mind a négy opciónak vizuálisan különböző képe kell.
            QuestionType.JP_TO_IMAGE ->
                distinctOptionCount(request, questionType) < QuizQuestion.OPTION_COUNT
            QuestionType.JP_TO_HU, QuestionType.HU_TO_JP -> false
        }
        return if (imageSideMissing) SkipReason.NOT_ENOUGH_IMAGES else SkipReason.NOT_ENOUGH_CANDIDATES
    }

    /**
     * Hány **megkülönböztethető** opció áll rendelkezésre az adott típusnál.
     *
     * Szöveges opcióknál ez az opciókészlet mérete. Képes opcióknál (`JP_TO_IMAGE`) a
     * vizuálisan különböző képek száma: az ujjlenyomat (`QuizCandidateSet`) azonossága
     * azt jelenti, hogy a felhasználó két azonos képet látna, ami nem négy valódi opció.
     * Ujjlenyomat nélküli, de kvízképes kép önálló opciónak számít – a hiányzó
     * ujjlenyomat nem bizonyít egyezést.
     */
    private fun distinctOptionCount(request: QuizRequest, questionType: QuestionType): Int {
        val pool = optionPool(request, questionType)
        if (questionType != QuestionType.JP_TO_IMAGE) return pool.size
        val digests = LinkedHashSet<String>()
        var withoutDigest = 0
        pool.forEach { technique ->
            val digest = request.candidates.illustrationDigestOf(technique.id)
            if (digest == null) withoutDigest++ else digests += digest
        }
        return digests.size + withoutDigest
    }
}
