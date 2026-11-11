package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.CategoryId
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
) = Technique(
    id = TechniqueId.of(id),
    categoryId = CategoryId.of(categoryId),
    romaji = romaji,
    romajiAscii = romaji.lowercase(),
    hungarian = hungarian,
    illustration = illustration,
    status = status,
    datasetVersion = "0.1.0",
)
