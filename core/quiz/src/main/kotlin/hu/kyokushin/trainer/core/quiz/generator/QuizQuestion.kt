@file:UseSerializers(QuestionIdSerializer::class, TechniqueIdSerializer::class)

package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.TechniqueId
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Egy generált kvízkérdés – **kizárólag azonosítókkal** (ADR-006, DR-03).
 *
 * A kérdés nem tárol megjelenítendő szöveget, képet, `contentDescription`-t vagy bármi
 * mást, ami a tartalom másolata lenne. A megjelenítést a UI oldja fel a tudástárból a
 * [correctTechniqueId] és az [optionTechniqueIds] alapján. Ennek három következménye van,
 * és mindhárom szándékos:
 *
 * 1. A válasz kiértékelése mindig `selected == correctTechniqueId`, tehát
 *    **azonosító-összehasonlítás**. Ha a kérdés kész opciószövegeket hordozna, két
 *    azonos magyar jelentésű technikánál a szöveges egyezés hamis találatot adna (DR-03).
 * 2. A kérdés szerializálható és **kicsi**, ezért a teljes kvízállapot elfér a
 *    `SavedStateHandle`-ben (ADR-007, T-405) anélkül, hogy a tartalmat cipelné.
 * 3. Tartalomfrissítés után egy félbehagyott kvíz a **friss** szövegekkel jelenik meg,
 *    miközben a helyesség változatlan.
 *
 * Az invariánsokat a konstruktor kényszeríti ki, ezért visszafejtéskor is érvényesek: egy
 * sérült vagy manipulált mentett állapotból nem áll elő három opciós vagy „két helyes
 * válaszú" kérdés.
 *
 * @property id a kérdés azonosítója a munkameneten belül. Erre épül az „egy kérdésre
 *   pontosan egy válasz" szabály mindhárom védelmi vonala (ADR-005), ezért a kérdést az
 *   azonosítója azonosítja, nem a megjelenített tartalma.
 * @property type a kérdéstípus; ez dönti el, hogy a UI nevet vagy illusztrációt rendereljen.
 * @property correctTechniqueId az egyetlen helyes válasz. Mindig szerepel az
 *   [optionTechniqueIds] között.
 * @property optionTechniqueIds pontosan [OPTION_COUNT] darab, páronként **különböző**
 *   technikaazonosító, a megjelenítés sorrendjében (a sorrend a seedelt keverés eredménye,
 *   ezért a mentett állapot része – T-110).
 */
@Serializable
data class QuizQuestion(
    val id: QuestionId,
    val type: QuestionType,
    val correctTechniqueId: TechniqueId,
    val optionTechniqueIds: List<TechniqueId>,
) {
    init {
        require(optionTechniqueIds.size == OPTION_COUNT) {
            "A kérdésnek pontosan $OPTION_COUNT opciója lehet (FR-14), " +
                "${optionTechniqueIds.size} helyett. Elégtelen adat esetén a kérdés nem jön létre."
        }
        require(optionTechniqueIds.toSet().size == OPTION_COUNT) {
            "Az opciók páronként különböző technikák kell legyenek (FR-14, TR-02)."
        }
        require(correctTechniqueId in optionTechniqueIds) {
            "A helyes válasznak szerepelnie kell az opciók között (FR-14)."
        }
    }

    /**
     * A helyes válasz indexe az [optionTechniqueIds] listában.
     *
     * Kényelmi származtatás a teszteknek és a UI-nak; **nem** tárolt mező, mert két
     * igazságforrás lenne a helyes válaszra, és egy elcsúszott index néma hibát adna.
     */
    val correctOptionIndex: Int
        get() = optionTechniqueIds.indexOf(correctTechniqueId)

    /** A három téves válasz azonosítója, a megjelenítés sorrendjében. */
    val distractorTechniqueIds: List<TechniqueId>
        get() = optionTechniqueIds.filter { it != correctTechniqueId }

    companion object {
        /** Minden kérdés pontosan ennyi opciót kínál – kevesebb nem engedett (FR-14). */
        const val OPTION_COUNT: Int = 4
    }
}
