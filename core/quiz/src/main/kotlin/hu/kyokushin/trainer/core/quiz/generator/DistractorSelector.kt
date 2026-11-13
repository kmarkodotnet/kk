package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId

/**
 * A három téves válasz kiválasztása – a kvízhelyesség legkényesebb pontja
 * (FR-14, FR-22, CR-07, TR-02, T-109).
 *
 * A [QuizCandidateFilter] megmondja, **ki jöhet szóba** egy típusban (kategóriaszűrő +
 * ADR-010 jogosultsági kapu); ez a lépés azt dönti el, hogy egy adott helyes válasz mellé
 * összeáll-e három **elfogadható** distraktor. A kettő szándékosan két típus: a szűrés
 * technikánkénti és típusonkénti, a distraktorválasztás viszont a helyes válaszhoz
 * **relatív** (szinonimák, kategória, képazonosság), ezért nem lehet előre kiszámolt lista.
 *
 * **Prioritási sorrend** (kvízmotor skill 4. pont):
 *
 * 1. ugyanaz a kategória – ez a tanulási cél, a hasonlókat kell megkülönböztetni,
 * 2. ha a kategórián belül nincs három elfogadható jelölt: feltöltés más kategóriából,
 * 3. ha így sincs három: **a kérdés nem jön létre** ([selectDistractors] `null`-t ad).
 *
 * Nincs harmadik út: nincs háromopciós kérdés, nincs generált („hihető") opció, és nincs
 * szinonim kisegítő opció. Minden opció létező technikarekordra hivatkozik. Kevés adatnál
 * inkább kevesebb kérdés lesz, és azt a terv [Shortfall]-ként megindokolja (FR-22).
 *
 * **Kizárási szűrő minden jelöltre:**
 *
 * | Feltétel | Indok |
 * |---|---|
 * | benne van a típus opciókészletében ([QuizCandidateFilter.optionPool]) | ADR-010 kapu |
 * | nem a helyes válasz | FR-14: pontosan egy helyes |
 * | nem szinonimája a helyesnek (V006 csoportok) | CR-07 |
 * | nem szinonimája egy már kiválasztott distraktornak | CR-07 kölcsönös kizárás |
 * | (D) `JP_TO_IMAGE`: képe nem vizuálisan azonos egyik opcióéval sem | FR-14, TR-02 |
 *
 * **Determinizmus.** A bejárás rendezett: a jelöltek a [QuizCandidateFilter.optionPool]
 * azonosító szerint rendezett listájából jönnek, kategóriatérbe rendezve, és a kiválasztás
 * az első három elfogadható jelölt. `Set`-iterációs sorrendre épülő döntés nincs, RNG-t
 * ez a lépés **nem** használ (NFR-07, ADR-007). A seedelt keverés és a kiválasztás
 * randomizálása a T-110 tárgya: az erre épülő réteg az [acceptableDistractors] rendezett
 * listáját keverheti meg a seedelt RNG-vel, a kizárási szabályok újrafogalmazása nélkül.
 */
object DistractorSelector {
    /** Ennyi téves válasz kell egy kérdéshez – kevesebb nem engedett (FR-14). */
    const val DISTRACTOR_COUNT: Int = QuizQuestion.OPTION_COUNT - 1

    /**
     * Az adott helyes válasz mellé **önmagában** elfogadható distraktorjelöltek, a
     * választás prioritási sorrendjében: előbb az azonos kategóriájúak, utánuk a többi,
     * mindkét szakaszon belül technikaazonosító szerint rendezve.
     *
     * Itt csak azok a feltételek szerepelnek, amelyek **egy** jelöltre eldönthetők: a
     * típus opciókészlete, a helyes válasz kizárása, a V006 szinonimakapcsolat a helyessel,
     * és (D) típusnál a helyes válasz képével való vizuális egyezés. A jelöltek **egymás**
     * közti kizárása (két szinonim distraktor, két azonos kép) nem szűrés, hanem a
     * kiválasztás közbeni döntés – azt a [selectDistractors] végzi.
     *
     * A lista hosszából tehát **nem** következik, hogy a kérdés összeáll; erre a
     * [canBuildQuestion] a hiteles válasz.
     */
    fun acceptableDistractors(
        request: QuizRequest,
        questionType: QuestionType,
        correct: Technique,
    ): List<Technique> {
        val synonymsOfCorrect = request.candidates.synonymsOf(correct.id)
        val correctDigest = visualDigestOf(request, questionType, correct.id)
        val pool = QuizCandidateFilter.optionPool(request, questionType)
            .filter { candidate ->
                candidate.id != correct.id &&
                    candidate.id !in synonymsOfCorrect &&
                    (
                        correctDigest == null ||
                            visualDigestOf(request, questionType, candidate.id) != correctDigest
                    )
            }
        // Prioritás: azonos kategória elöl, más kategória utána (feltöltésként).
        val (sameCategory, otherCategory) = pool.partition { it.categoryId == correct.categoryId }
        return sameCategory + otherCategory
    }

    /**
     * A három téves válasz, vagy `null`, ha nem áll össze három elfogadható distraktor –
     * ilyenkor a kérdés **nem jön létre** (FR-14, FR-22).
     *
     * A bejárás az [acceptableDistractors] prioritási sorrendjét követi, és menet közben
     * zárja ki azt a jelöltet, amely egy már kiválasztott distraktor szinonimája (CR-07),
     * illetve (D) típusnál azt, amelynek képe egy már kiválasztott opció képével
     * vizuálisan azonos. Az eredmény listája soha nem rövidebb és nem hosszabb
     * [DISTRACTOR_COUNT]-nál: vagy pontosan három, vagy `null`.
     *
     * A kizárás **konzervatív**: a bejárás az első elfogadható jelöltet választja, nem
     * keres maximális párosítást. Egy ritka, egymást keresztező szinonimacsoport-elrendezés
     * esetén így elvileg kimaradhat egy elvben összeállítható kérdés – ez a biztonságos
     * irány (FR-22: kevesebb, de helyes kérdés), és mivel a [canBuildQuestion] ugyanezt a
     * függvényt kérdezi meg, a számolt és a tényleges kérdésszám nem tud elválni (T-111).
     */
    fun selectDistractors(
        request: QuizRequest,
        questionType: QuestionType,
        correct: Technique,
    ): List<Technique>? {
        val chosen = ArrayList<Technique>(DISTRACTOR_COUNT)
        val excludedIds = LinkedHashSet(request.candidates.synonymsOf(correct.id))
        val usedDigests = LinkedHashSet<String>()
        visualDigestOf(request, questionType, correct.id)?.let(usedDigests::add)

        for (candidate in acceptableDistractors(request, questionType, correct)) {
            if (candidate.id in excludedIds) continue
            val digest = visualDigestOf(request, questionType, candidate.id)
            // Hiányzó ujjlenyomat nem bizonyít egyezést, ezért nem is zár ki.
            if (digest != null && digest in usedDigests) continue

            chosen += candidate
            excludedIds += request.candidates.synonymsOf(candidate.id)
            if (digest != null) usedDigests += digest
            if (chosen.size == DISTRACTOR_COUNT) return chosen
        }
        return null
    }

    /**
     * A kérdés opcióinak azonosítói – **pontosan négy**, a helyes válaszsal együtt –, vagy
     * `null`, ha a kérdés nem jön létre (FR-14).
     *
     * A sorrend itt kanonikus (a helyes válasz elöl), nem megjelenítési sorrend: az
     * opciók seedelt keverése a T-110 feladata. Így a kiválasztás és a keverés külön
     * tesztelhető, és a keverés nem tud észrevétlenül opciót elhagyni vagy hozzáadni.
     */
    fun selectOptionIds(
        request: QuizRequest,
        questionType: QuestionType,
        correct: Technique,
    ): List<TechniqueId>? {
        val distractors = selectDistractors(request, questionType, correct) ?: return null
        return listOf(correct.id) + distractors.map { it.id }
    }

    /** Összeáll-e négyopciós kérdés ezzel a helyes válaszsal (FR-14). */
    fun canBuildQuestion(
        request: QuizRequest,
        questionType: QuestionType,
        correct: Technique,
    ): Boolean = selectDistractors(request, questionType, correct) != null

    /**
     * Azok a helyes válaszok, amelyekből az adott típusban **tényleg** összeáll kérdés,
     * azonosító szerint rendezve.
     *
     * Ez a [QuizCandidateFilter.correctAnswerPool] szűkítése: a jogosultsági kapun átjutott
     * jelöltek közül csak azok maradnak, amelyek mellé három elfogadható distraktor is
     * kijön. A T-108 `buildableQuestionCount` felső korlátja ettől válik ténylegessé –
     * a szinonimakizárás (CR-07) és a képazonosság (D) itt szűr.
     *
     * A lista egyben a kérdéssorrend alapja: a motor **nem ismétli** ugyanazt a helyes
     * technikát, amíg van fel nem használt jelölt, és utána sem – ha a kért kérdésszám
     * nagyobb a lista hosszánál, kevesebb kérdés lesz, [Shortfall]-lal (FR-22).
     */
    fun buildableCorrectAnswers(
        request: QuizRequest,
        questionType: QuestionType,
    ): List<Technique> = QuizCandidateFilter.correctAnswerPool(request, questionType)
        .filter { canBuildQuestion(request, questionType, it) }

    /**
     * A technikához tartozó illusztráció tartalmi ujjlenyomata, **ha** az adott típusnál
     * az opciók képek.
     *
     * Csak a (D) `JP_TO_IMAGE` opciói illusztrációk; a (C) `IMAGE_TO_JP` kérdése kép, de
     * az opciói romaji nevek, ezért ott két azonos kép nem zavaró – a kérdés oldalán
     * egyetlen kép van (ADR-003, ADR-011).
     */
    private fun visualDigestOf(
        request: QuizRequest,
        questionType: QuestionType,
        id: TechniqueId,
    ): String? = when (questionType) {
        QuestionType.JP_TO_IMAGE -> request.candidates.illustrationDigestOf(id)
        QuestionType.JP_TO_HU, QuestionType.HU_TO_JP, QuestionType.IMAGE_TO_JP -> null
    }
}
