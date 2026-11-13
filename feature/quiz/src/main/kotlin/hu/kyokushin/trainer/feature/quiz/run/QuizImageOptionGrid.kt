package hu.kyokushin.trainer.feature.quiz.run

/**
 * A (D) típus 2×2 képrácsának tiszta elrendezési logikája (T-408, FUNCTIONAL_SPEC
 * 5.2/2.: „2×2 képrács, 4 illusztráció").
 *
 * Szándékosan **nem** `@Composable`: ugyanaz a „Compose döntési logika tesztelése
 * emulátor nélkül" minta, mint a [QuizOptionFeedbackResolver] (`docs/CONVENTIONS.md` 4.1
 * szakasz) – a pozíció-hozzárendelés sima JUnit teszttel bizonyítható.
 *
 * A pozíció **az [options] lista sorrendjéből** jön (1-alapú index), nem rendezi újra –
 * ez ugyanaz a seedelt keverési sorrend, amit a
 * [hu.kyokushin.trainer.core.quiz.generator.QuizQuestion.optionTechniqueIds] már hordoz
 * (ADR-007). A pozíció az
 * [hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel.QuizOption]
 * leírásának egyetlen bemenete – ha ez a sorrendtől eltérne, a TalkBack-felhasználó
 * rossz opciót azonosítana be a pozíció alapján (NFR-04).
 */
internal object QuizImageOptionGrid {
    /** Egy rácscella: a 1-alapú pozíció és a hozzá tartozó opció. */
    data class Cell(
        val position: Int,
        val option: QuizOptionDisplay,
    )

    /**
     * Az [options] listát [columns] szélességű sorokra bontja, 1-alapú pozíciókkal.
     *
     * 4 opció, 2 oszlop esetén pontosan 2×2 sort ad – ez a (D) típus egyetlen elvárt
     * bemenete (`QuizQuestion.OPTION_COUNT` = 4), de a függvény tetszőleges elemszámmal
     * és oszlopszámmal is helyesen működik (az utolsó sor rövidebb lehet).
     */
    fun rows(options: List<QuizOptionDisplay>, columns: Int = 2): List<List<Cell>> {
        require(columns >= 1) { "A rács oszlopszáma legalább 1." }
        return options
            .mapIndexed { index, option -> Cell(position = index + 1, option = option) }
            .chunked(columns)
    }
}
