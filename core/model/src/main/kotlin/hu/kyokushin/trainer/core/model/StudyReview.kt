package hu.kyokushin.trainer.core.model

import java.time.Instant

/**
 * Egy tanulókártyán adott önértékelés – felhasználói adat (DR-04).
 *
 * A tartalomtól külön életciklust követ (DR-07): tartalomfrissítés nem törli, és nincs
 * cascade a technikáról (ADR-004). A „gyakorlást igénylő" lista ezekből a rekordokból
 * származik, ezért [TechniqueId]-ra hivatkozik, nem beágyazott technikára.
 *
 * Minden önértékelés **külön sor**, nem felülírt állapot: a haladás időbeli alakulása
 * csak így olvasható ki, és a legutóbbi ítélet is mindig a legfrissebb [reviewedAt]
 * alapján adódik (DR-09 – a statisztika levezetett, nem duplikált számláló).
 *
 * @property id a Room által generált elsődleges kulcs; `0`, amíg a rekord nincs beszúrva.
 * @property techniqueId melyik technikára szól az önértékelés.
 * @property selfRating a kétértékű önértékelés ([SelfRating]). Nincs sokfokozatú skála
 *   és nincs ismétlésütemezés – a spaced repetition MOST nem implementálandó.
 * @property reviewedAt az önértékelés időpontja, **injektált** `Clock`-ból (NFR-07).
 */
data class StudyReview(
    val id: Long,
    val techniqueId: TechniqueId,
    val selfRating: SelfRating,
    val reviewedAt: Instant,
) {
    init {
        require(id >= 0L) { "StudyReview.id nem lehet negatív (0 = még nem perzisztált rekord)." }
    }
}
