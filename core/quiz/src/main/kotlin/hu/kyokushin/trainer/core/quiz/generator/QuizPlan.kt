package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.QuestionType
import kotlinx.serialization.Serializable

/**
 * A generátor kimenete: a lefuttatható kvíz terve (FR-14, FR-22).
 *
 * A motor **nem dob kivételt** elégtelen adatra, és nem állít elő hibás kérdést. Minden
 * „nem sikerült" eset ebben a típusban jelenik meg, strukturáltan: a [skippedTypes] és a
 * [shortfall] nem hibajelzés, hanem a UI-nak szóló **indoklás**, amiből konkrét magyar
 * üzenet és javaslat születik (FR-22). A szövegeket a UI állítja elő `strings.xml`-ből –
 * a motor szövegfüggetlen.
 *
 * A terv szerializálható, mert a félbehagyott kvíz a kérdéslistával és a [shortfall]-lal
 * együtt a `SavedStateHandle`-be kerül (ADR-007, T-405).
 *
 * @property questions a kérdések a lejátszás sorrendjében; legfeljebb annyi, amennyit a
 *   kérés kért. Üres lista esetén a kvíz **nem indítható** – lásd [canStart].
 * @property skippedTypes melyik engedélyezett kérdéstípus maradt ki és miért. Csak
 *   kihagyott típus szerepel benne; a sikeresen felhasznált típusok nem.
 * @property shortfall `null`, ha pontosan a kért kérdésszám összeállt; egyébként a kért és
 *   a tényleges szám az okkal együtt.
 */
@Serializable
data class QuizPlan(
    val questions: List<QuizQuestion> = emptyList(),
    val skippedTypes: Map<QuestionType, SkipReason> = emptyMap(),
    val shortfall: Shortfall? = null,
) {
    init {
        require(questions.map { it.id }.toSet().size == questions.size) {
            "A kérdésazonosítók egy terven belül egyediek (ADR-005)."
        }
    }

    /**
     * Elindítható-e ebből kvíz.
     *
     * Egy kérdés is elég kvíznek; a nulla nem. Ha `false`, a UI nem indít kvízt, hanem a
     * [skippedTypes] és a [shortfall] alapján magyaráz és javaslatot ad (FR-22).
     */
    val canStart: Boolean
        get() = questions.isNotEmpty()

    /** A tényleges kérdésszám – a százalékszámítás alapja nem ez, hanem a megválaszoltak száma. */
    val questionCount: Int
        get() = questions.size
}

/**
 * Miért maradt ki egy kérdéstípus a tervből (FR-22).
 *
 * Azért enum és nem szabad szöveg, mert a UI magyar üzenete és javaslata típusonként más,
 * és a szöveg a `strings.xml`-ben él, nem a motorban. Új ok hozzávétele így fordítási
 * hiba lesz minden olyan helyen, ahol a `when` nem kezeli.
 */
enum class SkipReason {

    /**
     * A szűrt jelöltkészletben nincs elég kvízképes technika ehhez a típushoz: a helyes
     * válasz mellé nem áll össze három elfogadható distraktor (FR-14).
     */
    NOT_ENOUGH_CANDIDATES,

    /**
     * Képes típus (`IMAGE_TO_JP`, `JP_TO_IMAGE`), és nincs elég validált, kvízben
     * használható, vizuálisan különböző illusztráció. A `quizUsable = false` (feliratos)
     * kép és a `Placeholder` nem számít képnek – ADR-003, ADR-011.
     */
    NOT_ENOUGH_IMAGES,

    /** A felhasználó nem engedélyezte ezt a típust a kvízbeállító képernyőn (FR-12). */
    TYPE_DISABLED,
}

/**
 * Kevesebb kérdés állt össze, mint amennyit a kérés kért (FR-22).
 *
 * Ez **nem hiba**: a motor szándékosan ad kevesebb, de helyes kérdést ahelyett, hogy
 * ismételne vagy hibás kérdést gyártana (kvízmotor-invariáns 6. és 8. pont). A UI ebből
 * tájékoztat: „10 kérdést kértél, 6 állt össze" + javaslat.
 *
 * @property requestedCount a kért kérdésszám (5 / 10 / 20).
 * @property actualCount a tényleges kérdésszám; mindig kisebb a kértnél.
 * @property reason a legfőbb ok, amit a UI megmagyaráz.
 */
@Serializable
data class Shortfall(
    val requestedCount: Int,
    val actualCount: Int,
    val reason: ShortfallReason,
) {
    init {
        require(requestedCount > 0) { "A kért kérdésszám pozitív." }
        require(actualCount >= 0) { "A tényleges kérdésszám nem lehet negatív." }
        require(actualCount < requestedCount) {
            "Shortfall csak akkor létezik, ha a tényleges kérdésszám kisebb a kértnél; " +
                "egyébként a QuizPlan.shortfall értéke null."
        }
    }

    /** Hány kérdés hiányzik a kérthez. */
    val missingCount: Int
        get() = requestedCount - actualCount
}

/**
 * A hiányzó kérdések legfőbb oka (FR-22).
 *
 * Külön enum a [SkipReason]-tól, mert más a hatóköre: a [SkipReason] egy **kérdéstípusra**
 * mondja meg, miért nem használtuk, a [ShortfallReason] pedig a **teljes tervre**, miért
 * nem jött ki a kért kérdésszám.
 */
enum class ShortfallReason {

    /**
     * Kevesebb kvízképes technika volt a szűrt készletben, mint a kért kérdésszám. A motor
     * nem ismétli ugyanazt a helyes technikát, amíg van fel nem használt jelölt, és
     * utána sem – inkább kevesebb kérdést ad.
     */
    NOT_ENOUGH_CANDIDATES,

    /**
     * Volt még fel nem használt helyes jelölt, de mellé nem állt össze három elfogadható
     * (nem szinonim, típusra jogosult) distraktor, ezért az a kérdés nem jött létre
     * (CR-07, T-109).
     */
    NOT_ENOUGH_DISTRACTORS,

    /** Képes típusnál nem volt elég validált, vizuálisan különböző illusztráció. */
    NOT_ENOUGH_IMAGES,

    /**
     * Minden engedélyezett kérdéstípus kimaradt, ezért egyetlen kérdés sem jött létre.
     * Ilyenkor a [Shortfall.actualCount] nulla, és a `QuizPlan.canStart` hamis.
     */
    ALL_TYPES_SKIPPED,
}
