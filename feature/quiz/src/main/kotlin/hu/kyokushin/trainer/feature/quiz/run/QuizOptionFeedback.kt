package hu.kyokushin.trainer.feature.quiz.run

import hu.kyokushin.trainer.core.model.TechniqueId

/**
 * Egy válaszopció vizuális/akadálymentességi állapota, a válasz megadása UTÁN (T-409,
 * FR-17, NFR-04, FUNCTIONAL_SPEC 5.2/4., FUNCTIONAL_SPEC 9.).
 *
 * Szándékosan **nem** `Boolean` pár (isSelected/isCorrect): a Compose réteg (és a
 * Preview-k) egyetlen, kimerítő `when`-nel dönthetnek az ikonról, a szövegről, a
 * színről és a `stateDescription`-ről, egy helyen, négy eset helyett.
 */
internal enum class QuizOptionFeedback {
    /** Még nincs válasz ebben a kérdésben – semleges, kattintható opció. */
    UNANSWERED,

    /** Ez volt a kiválasztott ÉS helyes opció (FUNCTIONAL_SPEC 5.2/4.: „pipa + Helyes"). */
    SELECTED_CORRECT,

    /** Ez volt a kiválasztott, de helytelen opció („× + Helytelen"). */
    SELECTED_INCORRECT,

    /**
     * Nem ezt választották, de ez a helyes válasz – „a helyes opció mindig kiemelve"
     * (FUNCTIONAL_SPEC 5.2/4.), ugyanazzal az ikon+szöveg+szín jelzéssel, mint a
     * [SELECTED_CORRECT], hogy a kiemelés ne csak színnel történjen (NFR-04).
     */
    CORRECT_UNSELECTED,

    /** Nem ezt választották, és nem is helyes – visszafogott, letiltott megjelenés. */
    NEUTRAL,
}

/**
 * Pure döntés: melyik [QuizOptionFeedback] jár egy opciónak (T-409 „Kész ha" – a
 * `docs/CONVENTIONS.md` 4.1 „Compose döntési logika tesztelése emulátor nélkül" mintája:
 * a döntést egy `@Composable`-mentes `object`-be emeljük, hogy sima JUnit teszttel,
 * Robolectric/Compose-renderelés nélkül bizonyítható legyen).
 */
internal object QuizOptionFeedbackResolver {
    fun resolve(
        optionId: TechniqueId,
        correctTechniqueId: TechniqueId,
        selectedTechniqueId: TechniqueId?,
    ): QuizOptionFeedback = when {
        selectedTechniqueId == null -> QuizOptionFeedback.UNANSWERED
        optionId == selectedTechniqueId && optionId == correctTechniqueId -> QuizOptionFeedback.SELECTED_CORRECT
        optionId == selectedTechniqueId -> QuizOptionFeedback.SELECTED_INCORRECT
        optionId == correctTechniqueId -> QuizOptionFeedback.CORRECT_UNSELECTED
        else -> QuizOptionFeedback.NEUTRAL
    }
}
