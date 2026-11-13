package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlin.random.Random

/**
 * A [QuizGenerator] egyetlen implementációja – a seedelt, determinisztikus kvízösszeállítás
 * (FR-11…FR-15, FR-22, NFR-07, ADR-007, TR-02, TR-03, T-110).
 *
 * A generálás három, szándékosan szétválasztott lépésre épül, és ebből **kettőt nem itt**
 * írunk le újra:
 *
 * | Lépés | Hol él | Mit dönt el |
 * |---|---|---|
 * | 1. szűrés, jogosultsági kapu, kihagyási okok | [QuizCandidateFilter] (T-108) | ki jöhet szóba melyik típusban |
 * | 2. distraktorválasztás, szinonima- és képkizárás | [DistractorSelector] (T-109) | összeáll-e négy opció egy helyes válasz mellé |
 * | 3. sorrend: kérdéssorrend és opciósorrend | **ez az osztály** | a felhasználó mit lát, milyen sorrendben |
 *
 * Ez az osztály tehát **nem** fogalmaz újra kiválasztási szabályt: az opciók halmazát a
 * [DistractorSelector.selectOptionIds] adja, és innentől már csak permutáció történik
 * ([SeededShuffle]). Így a keverés nem tud opciót elhagyni vagy becsempészni, és a
 * „mennyi kérdés áll össze" kérdésre is egyetlen igazságforrás válaszol.
 *
 * **Determinizmus (NFR-07, ADR-007).** Minden véletlen a kérésben kapott
 * [QuizRequest.seed]-ből származik: a `generate` egyetlen `Random(seed)` példányt hoz
 * létre, és minden sorsolás ebből megy. A jelöltbejárás rendezett (a [QuizCandidateFilter]
 * azonosító szerint rendezett listái és a [QuestionType.entries] deklarációs sorrend),
 * `Set`-iterációs sorrendre épülő döntés nincs. A modulban nincs `Random.Default`, nincs
 * rendszeróra-hívás – erre külön teszt is van. Következmény: ugyanaz a kérés kétszer hívva
 * bitre ugyanazt a [QuizPlan]-t adja, az opciók sorrendjével együtt, tehát a forgatás utáni
 * állapot-visszaállítás (T-405) ugyanazt a kvízt kapja vissza.
 *
 * **Nincs ismétlés (kvízmotor-invariáns 6.).** Ugyanaz a helyes technika egy terven belül
 * legfeljebb egyszer szerepel – akkor is, ha több kérdéstípusban is használható lenne. Ha
 * a kért kérdésszám több, mint ahány különböző helyes jelölt van, a terv **kevesebb**
 * kérdést ad [Shortfall]-lal; ismétlés és kitalált opció helyett (FR-22).
 *
 * (A fenti táblázat két sora szándékosan 120 karakternél hosszabb: a cellák tördelése
 * eltörné a Markdown-renderelést, ezért a `max-line-length` ktlint-szabály alább
 * elszigetelten, indokolt elnyomva – T-607.)
 */
@Suppress("ktlint:standard:max-line-length")
object DefaultQuizGenerator : QuizGenerator {
    /**
     * Egy lehetséges kérdés a keverés **előtt**: egy kérdéstípus és egy hozzá tényleg
     * összeálló helyes válasz.
     *
     * Azért ez a keverés egysége, és nem a technika, mert ugyanaz a technika több típusban
     * is lehet helyes válasz. A keverés így a „melyik technikát, melyik típusban kérdezzük"
     * döntést egyszerre sorsolja ki – a terv nem típusonként tömbösített, hanem vegyes,
     * ahogy a felhasználó is vegyesen kapja a kérdéseket (FR-12).
     */
    private data class QuestionSlot(val type: QuestionType, val correct: Technique)

    override fun generate(request: QuizRequest): QuizPlan {
        val skippedTypes = QuizCandidateFilter.skipReasons(request)
        val random = Random(request.seed)

        // 1-3. lépés: rendezett jelöltbejárás – ugyanaz a függvény, amiből a `dryRun` is
        // számol, hogy a két ág ne tudjon elválni (FR-22, T-111).
        val slots = questionSlots(request, skippedTypes)

        // 4. lépés: seedelt Fisher–Yates keverés – ez adja a kérdéssorrendet.
        val shuffledSlots = SeededShuffle.shuffled(slots, random)

        // 5. lépés: kérdésépítés. A már felhasznált helyes technikát átlépjük (nincs
        // ismétlés), és a kért kérdésszámnál megállunk (a terv soha nem ad többet).
        val usedCorrectIds = LinkedHashSet<TechniqueId>()
        val questions = ArrayList<QuizQuestion>(request.requestedCount)
        for (slot in shuffledSlots) {
            if (questions.size == request.requestedCount) break
            if (!usedCorrectIds.add(slot.correct.id)) continue
            // A `buildableCorrectAnswers` garanciája szerint ez nem `null`; ha mégis az
            // lenne, inkább egy kérdéssel kevesebb lesz, mint hibás kérdés (FR-14).
            val optionIds = DistractorSelector.selectOptionIds(request, slot.type, slot.correct)
            if (optionIds == null) {
                usedCorrectIds.remove(slot.correct.id)
                continue
            }
            questions += QuizQuestion(
                id = questionIdFor(questions.size),
                type = slot.type,
                correctTechniqueId = slot.correct.id,
                // Az opciókeverés is a **ugyanabból** a seedelt RNG-ből megy: a helyes
                // válasz pozíciója ezért reprodukálható, és nem mindig az első (FR-14).
                optionTechniqueIds = SeededShuffle.shuffled(optionIds, random),
            )
        }

        return QuizPlan(
            questions = questions,
            skippedTypes = skippedTypes,
            shortfall = shortfallOrNull(request, skippedTypes, questions.size),
        )
    }

    /**
     * A készenléti összegzés **kérdésépítés nélkül** (FR-13, FR-22, T-111).
     *
     * A beállító képernyő élő összegzése minden kapcsolóváltásnál újraszámol, ezért ez az
     * ág nem épít kérdést: nincs `Random`, nincs keverés, nincs opcióválasztás kérdésenként,
     * és nem jön létre [QuizQuestion]. Amit elvégez, az pontosan a [generate] 1-3. lépése
     * ([questionSlots]) – ugyanaz a függvény, ugyanazzal a szűrővel és jogosultsági kapuval.
     *
     * **Miért egyezik mégis a kérdésszám?** A [generate] a keverés után a *helyes technika*
     * szerint deduplikál (nincs ismétlés), és a kért kérdésszámnál megáll; a
     * [DistractorSelector.buildableCorrectAnswers] garanciája szerint pedig minden slotból
     * tényleg összeáll kérdés. Így minden különböző helyes technika pontosan egy kérdést ad,
     * **függetlenül a keverés sorrendjétől** – vagyis a kérdésszám a kért szám és a slotok
     * különböző helyes technikáinak száma közül a kisebb. Ezt számolja ki
     * [expectedQuestionCount], és erre külön egyezési teszt van (FR-22, T-112).
     *
     * Az [QuizReadiness.availableByType] a slotokból áll össze, nem egy második számolásból:
     * típusonkénti **kapacitás** (ennyi kérdés állítható össze abból a típusból), nem a terv
     * felosztása. Ugyanaz a technika több típusban is lehet helyes válasz, ezért az értékek
     * összege nagyobb lehet, mint az [QuizReadiness.expectedQuestionCount].
     */
    override fun dryRun(request: QuizRequest): QuizReadiness {
        val skippedTypes = QuizCandidateFilter.skipReasons(request)
        val slots = questionSlots(request, skippedTypes)
        val expectedCount = expectedQuestionCount(request, slots)
        return QuizReadiness(
            requestedCount = request.requestedCount,
            expectedQuestionCount = expectedCount,
            availableByType = slots.groupingBy { it.type }.eachCount(),
            skippedTypes = skippedTypes,
            expectedShortfall = shortfallOrNull(request, skippedTypes, expectedCount),
        )
    }

    /**
     * A terv lehetséges kérdései a keverés előtt: minden meg nem hagyott típus minden olyan
     * helyes jelöltje, amely mellé tényleg összeáll négy opció.
     *
     * A [generate] és a [dryRun] **közös** bemenete – ez a két ág egyetlen találkozási
     * pontja, és ezért nem tud elválni egymástól (FR-22). A bejárás rendezett: a típusok a
     * [QuestionType] deklarációs sorrendjében, a helyes jelöltek azonosító szerint rendezve
     * (a [QuizCandidateFilter] rendezett listái), `Set`-iterációs sorrendtől függetlenül
     * (NFR-07). Kérdést nem épít, RNG-t nem használ.
     */
    private fun questionSlots(
        request: QuizRequest,
        skippedTypes: Map<QuestionType, SkipReason>,
    ): List<QuestionSlot> = QuestionType.entries
        .filter { it !in skippedTypes }
        .flatMap { type ->
            DistractorSelector.buildableCorrectAnswers(request, type)
                .map { correct -> QuestionSlot(type, correct) }
        }

    /**
     * Ennyi kérdés áll össze a slotokból: a kért kérdésszám, vagy ha kevesebb különböző
     * helyes technika van, akkor annyi (nincs ismétlés – kvízmotor-invariáns 6.).
     */
    private fun expectedQuestionCount(request: QuizRequest, slots: List<QuestionSlot>): Int =
        minOf(
            request.requestedCount,
            slots.mapTo(LinkedHashSet()) { it.correct.id }.size,
        )

    /**
     * A kérdés azonosítója a terven belüli pozíciójából (ADR-005).
     *
     * A terven belül kell egyedinek lennie – erre épül az „egy kérdésre pontosan egy
     * válasz" szabály –, ezért a sorszám elég. Nulla előtét azért van, hogy az azonosító
     * szöveges rendezése is a lejátszási sorrendet adja; a sorszámot `padStart`-tal
     * képezzük, nem `String.format`-tal, mert az utóbbi területi beállítástól függően más
     * számjegyeket írhatna – a mentett állapotnak viszont minden eszközön egyeznie kell.
     */
    private fun questionIdFor(index: Int): QuestionId =
        QuestionId.of("q-" + (index + 1).toString().padStart(2, '0'))

    /**
     * A [Shortfall] – vagy `null`, ha pontosan a kért kérdésszám jött ki (FR-22).
     *
     * Az ok sorrendje szándékos, a leghasznosabb magyar üzenet felé rendezve:
     *
     * 1. [ShortfallReason.ALL_TYPES_SKIPPED] – egyetlen kérdés sem jött össze. Ez pontosan
     *    akkor igaz, ha minden kérdéstípus kimaradt: egy meg nem hagyott típusból
     *    definíció szerint áll össze legalább egy kérdés.
     * 2. [ShortfallReason.NOT_ENOUGH_IMAGES] – egy engedélyezett képes típus **egésze**
     *    kiesett képhiány miatt; ez a legnagyobb egyszeri veszteség, és konkrét javaslatot
     *    ad (kapcsold ki a képes típusokat, vagy válassz más kategóriát).
     * 3. [ShortfallReason.NOT_ENOUGH_DISTRACTORS] – volt a kapun átjutott helyes jelölt,
     *    amely mellé nem állt össze három elfogadható distraktor (CR-07, T-109).
     * 4. [ShortfallReason.NOT_ENOUGH_CANDIDATES] – egyébként: kevesebb kvízképes technika
     *    volt, mint a kért kérdésszám.
     */
    private fun shortfallOrNull(
        request: QuizRequest,
        skippedTypes: Map<QuestionType, SkipReason>,
        actualCount: Int,
    ): Shortfall? {
        if (actualCount >= request.requestedCount) return null
        return Shortfall(
            requestedCount = request.requestedCount,
            actualCount = actualCount,
            reason = shortfallReason(request, skippedTypes, actualCount),
        )
    }

    private fun shortfallReason(
        request: QuizRequest,
        skippedTypes: Map<QuestionType, SkipReason>,
        actualCount: Int,
    ): ShortfallReason {
        if (actualCount == 0) return ShortfallReason.ALL_TYPES_SKIPPED

        // Rendezett bejárás: az engedélyezett típusok a deklarációs sorrendben.
        val enabledTypes = QuestionType.entries.filter { it in request.enabledTypes }
        if (enabledTypes.any { skippedTypes[it] == SkipReason.NOT_ENOUGH_IMAGES }) {
            return ShortfallReason.NOT_ENOUGH_IMAGES
        }
        val lostToDistractors = enabledTypes.any { type ->
            QuizCandidateFilter.correctAnswerPool(request, type).size >
                DistractorSelector.buildableCorrectAnswers(request, type).size
        }
        return if (lostToDistractors) {
            ShortfallReason.NOT_ENOUGH_DISTRACTORS
        } else {
            ShortfallReason.NOT_ENOUGH_CANDIDATES
        }
    }
}
