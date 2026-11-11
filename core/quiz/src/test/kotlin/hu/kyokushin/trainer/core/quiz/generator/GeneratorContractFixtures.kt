package hu.kyokushin.trainer.core.quiz.generator

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus

/**
 * A generátorszerződés tesztjeinek közös építői (T-107, TR-02).
 *
 * Minden builder **érvényes** alapértékekkel indul, a tesztek egy-egy paramétert
 * rontanak el – így a teszt neve és a módosított paraméter együtt elmondja, mit vizsgál.
 * Ugyanaz az indok, mint a validátor-készletnél: ha a builder több példányban élne, egy
 * default érték megváltoztatása csendben elválasztaná egymástól a tesztosztályokat.
 */

/** Négy technikaazonosító – pontosan annyi, amennyi egy kérdéshez kell (FR-14). */
internal fun ids(vararg raw: String): List<TechniqueId> = raw.map(TechniqueId::of)

/** Egy érvényes, négyopciós kérdés; a helyes válasz alapból az első opció. */
internal fun question(
    id: String = "Q-1",
    type: QuestionType = QuestionType.JP_TO_HU,
    correct: String = "KY-0001",
    options: List<String> = listOf("KY-0001", "KY-0002", "KY-0003", "KY-0004"),
) = QuizQuestion(
    id = QuestionId.of(id),
    type = type,
    correctTechniqueId = TechniqueId.of(correct),
    optionTechniqueIds = options.map(TechniqueId::of),
)

/**
 * Egy elfogadott technikarekord. A szöveges mezők szándékosan felismerhető, egyedi
 * értékek: az ADR-006 tesztje azt állítja, hogy ezek egyike **sem** jelenik meg a
 * szerializált kérdésben.
 */
internal fun technique(
    id: String = "KY-0001",
    categoryId: String = "geri",
    romaji: String = "Mae geri",
    hungarian: String = "Előre rúgás",
    illustration: IllustrationRef? = null,
    status: VerificationStatus = VerificationStatus.AWAITING_REVIEW,
    quizExcluded: Boolean = false,
    retired: Boolean = false,
) = Technique(
    id = TechniqueId.of(id),
    categoryId = CategoryId.of(categoryId),
    romaji = romaji,
    romajiAscii = romaji.lowercase(),
    hungarian = hungarian,
    illustration = illustration,
    status = status,
    quizExcluded = quizExcluded,
    retired = retired,
    datasetVersion = "0.1.0",
)

/**
 * Egy kvízben használható illusztráció (ADR-003). A [quizUsable] `false` értéke a
 * **feliratos** képet modellezi: szakmailag hiteles, de elárulná a megoldást (ADR-011).
 */
internal fun asset(
    ref: String = "KY-0001",
    quizUsable: Boolean = true,
) = IllustrationRef.Asset(
    ref = ref,
    fileName = "$ref.webp",
    quizUsable = quizUsable,
    hideableOnStudyCard = !quizUsable,
)

/** Négy technikából álló, szöveges típusokra teljesen kvízképes alapkészlet. */
internal fun textCandidates(categoryId: String = "geri"): List<Technique> =
    (1..4).map { index ->
        technique(
            id = "KY-000$index",
            categoryId = categoryId,
            romaji = "Technique $index",
            hungarian = "Technika $index",
        )
    }

/** Egy kérés a megadott jelöltekkel; minden egyéb paraméter érvényes alapérték. */
internal fun request(
    techniques: List<Technique> = textCandidates(),
    enabledTypes: Set<QuestionType> = QuestionType.entries.toSet(),
    categoryFilter: Set<String> = emptySet(),
    requestedCount: Int = 10,
    seed: Long = 42L,
    datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    synonymGroups: List<Set<String>> = emptyList(),
    illustrationDigests: Map<String, String> = emptyMap(),
) = QuizRequest(
    candidates = QuizCandidateSet(
        techniques = techniques,
        synonymGroups = synonymGroups.map { group -> group.mapTo(LinkedHashSet(), TechniqueId::of) },
        illustrationDigests = illustrationDigests.mapKeys { TechniqueId.of(it.key) },
    ),
    enabledTypes = enabledTypes,
    categoryFilter = categoryFilter.mapTo(LinkedHashSet(), CategoryId::of),
    requestedCount = requestedCount,
    seed = seed,
    datasetTrust = datasetTrust,
)

/**
 * A kvízmotor **nyolc invariánsának** ellenőrzése egy kész terven (T-112, TR-02, TR-03).
 *
 * Azért közös függvény, és nem egy tesztosztály privát metódusa, mert két készlet futtatja
 * ugyanezt: a szintetikus mátrix ([QuizPlanInvariantTest]) és a valódi tartalom
 * ([RealContentQuizGenerationTest]). Ha két példányban élne, az egyik csendben elavulna, és
 * a valódi tartalomra már nem ugyanazt a garanciát bizonyítanánk.
 *
 * Az ellenőrzések sorrendje a kvízmotor skill 3. pontját követi, hogy a bukó állítás
 * azonnal megnevezze a megsértett szabályt. A [label] csak az üzenet olvashatóságát segíti.
 */
internal fun assertQuizPlanInvariants(label: String, request: QuizRequest, plan: QuizPlan) {
    val known = request.candidates.techniques.associateBy { it.id }
    val categoryScope = QuizCandidateFilter.inCategoryScope(request).mapTo(HashSet()) { it.id }

    // 6/a. A terv soha nem ad több kérdést a kértnél, és a shortfall ezzel egyezik.
    assertThat(plan.questionCount).isAtMost(request.requestedCount)
    assertThat(plan.shortfall == null).isEqualTo(plan.questionCount == request.requestedCount)
    assertThat(plan.shortfall?.actualCount ?: plan.questionCount).isEqualTo(plan.questionCount)

    // 6/b. Ugyanaz a helyes technika egy terven belül legfeljebb egyszer szerepel.
    val correctIds = plan.questions.map { it.correctTechniqueId }
    assertThat(correctIds.toSet()).hasSize(correctIds.size)

    plan.questions.forEach { question ->
        val options = question.optionTechniqueIds

        // 1. Pontosan négy opció.
        assertThat(options).hasSize(QuizQuestion.OPTION_COUNT)
        // 2. Pontosan egy opció a helyes válasz.
        assertThat(options.count { it == question.correctTechniqueId }).isEqualTo(1)
        // 3. A három distraktor egymástól és a helyestől is különböző.
        assertThat(options.toSet()).hasSize(QuizQuestion.OPTION_COUNT)
        assertThat(question.distractorTechniqueIds).hasSize(DistractorSelector.DISTRACTOR_COUNT)

        // 4. Egyetlen opciópár sem szinonima egymásnak (V006, CR-07) – páronként, nem csak
        // a helyes válaszhoz mérve.
        options.forEachIndexed { index, first ->
            options.drop(index + 1).forEach { second ->
                assertThat(request.candidates.synonymsOf(first)).doesNotContain(second)
                assertThat(request.candidates.synonymsOf(second)).doesNotContain(first)
            }
        }

        // 5. (D) típusnál nincs két vizuálisan azonos illusztráció az opciók között.
        if (question.type == QuestionType.JP_TO_IMAGE) {
            val digests = options.mapNotNull { request.candidates.illustrationDigestOf(it) }
            assertThat(digests.toSet()).hasSize(digests.size)
        }

        // 8. A motor nem gyárt opciót: minden opció létező, a típusra jogosult, a
        // kategóriaszűrőn belüli technikarekord.
        val optionPool = QuizCandidateFilter.optionPool(request, question.type)
            .mapTo(HashSet()) { it.id }
        options.forEach { id ->
            assertThat(known.keys).contains(id)
            assertThat(categoryScope).contains(id)
            assertThat(optionPool).contains(id)
        }
        assertThat(QuizCandidateFilter.correctAnswerPool(request, question.type).map { it.id })
            .contains(question.correctTechniqueId)

        // A kérdés típusa engedélyezett, és nincs a kihagyott típusok között (FR-12).
        assertThat(request.enabledTypes).contains(question.type)
        assertThat(plan.skippedTypes).doesNotContainKey(question.type)
    }

    // A terv kérdésazonosítói egyediek és a lejátszási sorrendet követik (ADR-005).
    val questionIds = plan.questions.map { it.id.value }
    assertThat(questionIds.toSet()).hasSize(questionIds.size)
    assertThat(questionIds).isInOrder()

    assertThat(label).isNotEmpty()
}
