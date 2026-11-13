package hu.kyokushin.trainer.core.model

/**
 * A tanulókártyán adott önértékelés.
 *
 * A DR-04 szerinti `StudyReview` rekord hordozza; szándékosan kétértékű. Nincs
 * sokfokozatú skála és nincs ismétlésütemezés sem: a spaced repetition a mesterprompt
 * 10. pontja szerint MOST nem implementálandó, így a modell sem nyit rá kaput.
 */
enum class SelfRating {
    /** A felhasználó szerint tudta a technikát. */
    KNEW,

    /** A felhasználó szerint gyakorolnia kell még – ez a „gyakorlást igénylő" lista alapja. */
    NEEDS_PRACTICE,
}
