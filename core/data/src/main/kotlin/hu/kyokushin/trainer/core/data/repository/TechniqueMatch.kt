package hu.kyokushin.trainer.core.data.repository

import hu.kyokushin.trainer.core.model.Technique

/**
 * Egy keresési találat: a technika + **mi alapján** került a listába (FR-03, T-202).
 *
 * A Tudástár listája alias alapján is talál (`chudan zuki` → `Seiken Chudan Tsuki`), és
 * ilyenkor a tétel alatt meg kell jelennie az „Egyezés: `{alias}`" sornak
 * (FUNCTIONAL_SPEC 3.1, US-06 3. elfogadási kritérium). Ehhez a UI-nak tudnia kell,
 * melyik alias illeszkedett – kitalálni nem tudja, mert akkor a keresés
 * mintaillesztését írná le másodszor, Compose oldalon.
 *
 * Ez a típus **nem** Room projekció: a `core/data` publikus szerződésének része, és
 * kizárólag `core/model` domain modellt hordoz (ADR-001 L4, TR-07).
 *
 * @property technique a megtalált technika, az aliasaival együtt.
 * @property matchedAlias annak az aliasnak az eredeti alakja, amely a keresőszövegre
 *   illeszkedett – vagy `null`, ha a találat a technika **saját** nevéből (romaji vagy
 *   magyar) származik, illetve ha a keresés üres volt. Több illeszkedő alias esetén az
 *   ábécé szerint első, hogy a kiírt szöveg determinisztikus legyen (NFR-07).
 */
data class TechniqueMatch(
    val technique: Technique,
    val matchedAlias: String? = null,
)
