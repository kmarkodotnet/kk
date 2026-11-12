package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId

/**
 * A kvízgenerátor jelöltkészlete – a tartalom oldaláról minden, amit a motor ismerhet.
 *
 * Ez a `TechniqueRepository.getQuizCandidates(...)` visszatérési típusa
 * (`docs/ARCHITECTURE.md` 5.3), és egyben a [QuizRequest] tartalmi fele. Azért önálló
 * típus, mert a motornak a technikákon **túl** két olyan ismeretre is szüksége van,
 * amit a technikarekord maga nem hordoz:
 *
 * - a [synonymGroups] a V006 kölcsönös kizárási csoportok: azonos jelentésű kifejezések
 *   nem lehetnek egymás distraktorai (CR-07). Ez a validátor kimenete
 *   (`ValidationReport.synonymGroups`) – ugyanaz a csatolási pont, nem új igazságforrás.
 * - az [illustrationDigests] a képek **azonosságának** ujjlenyomata, amiből a
 *   `JP_TO_IMAGE` típus el tudja dönteni, hogy két opció **vizuálisan azonos** képet
 *   mutatna-e (T-109). A `TechniqueRepository.getQuizCandidates` megvalósítása (T-402) ezt
 *   a csomagolt illusztrációfájl nevéből tölti – nem a kép bájttartalmának hasheléséből,
 *   mert a sémában nincs ilyen oszlop, és becsült ujjlenyomatot nem gyártunk. Az egyenlőség
 *   emiatt elegendő, de nem feltétlenül bitre pontos SHA-256: ugyanaz a fájlnév garantáltan
 *   ugyanazt a csomagolt bájtsorozatot jelenti, de két *eltérő* fájlnevű, bitre azonos kép
 *   (tartalomszerkesztési hiba) ezzel a kulccsal nem ismerhető fel – ez a
 *   `knowledgebase_transfer` konverter I003 diagnosztikájának hatóköre, nem a motoré.
 *
 * A készlet **nem** dönt kvízképességről: azt kérdéstípusonként a jogosultsági kapu
 * (`QuizEligibility`, ADR-010) mondja meg a bizalmi szint ismeretében (T-108). Ez a típus
 * csak adatot szállít, szabályt nem.
 *
 * @property techniques a szóba jöhető technikák. Tartalmazhat olyan rekordot is, amely
 *   végül egyetlen típusban sem lesz jogosult – a szűrés a motor dolga, nem a hívóé.
 * @property synonymGroups legalább két elemű, a [techniques] halmazán belüli
 *   azonosítócsoportok. Egy technika több csoportban is szerepelhet.
 * @property illustrationDigests technikaazonosító – a hozzá tartozó illusztráció
 *   azonosságának ujjlenyomata (ma: a csomagolt fájl neve, lásd fent). Csak azokra a
 *   technikákra van bejegyzés, amelyeknek valódi, validált képfájluk van; a
 *   `Placeholder` nem kap ujjlenyomatot.
 */
data class QuizCandidateSet(
    val techniques: List<Technique> = emptyList(),
    val synonymGroups: List<Set<TechniqueId>> = emptyList(),
    val illustrationDigests: Map<TechniqueId, String> = emptyMap(),
) {
    init {
        require(techniques.map { it.id }.toSet().size == techniques.size) {
            "A jelöltkészletben egy technika csak egyszer szerepelhet."
        }
        require(synonymGroups.all { it.size >= 2 }) {
            "Egy szinonimacsoportnak legalább két tagja van (V006)."
        }
        val knownIds = techniques.mapTo(HashSet()) { it.id }
        require(synonymGroups.all { group -> group.all { it in knownIds } }) {
            "A szinonimacsoportok csak a készletben lévő technikákra hivatkozhatnak (V006)."
        }
        require(illustrationDigests.keys.all { it in knownIds }) {
            "Az illusztráció-ujjlenyomatok csak a készletben lévő technikákra hivatkozhatnak."
        }
    }

    /**
     * Azok a technikaazonosítók, amelyek az [id]-val **egy fogalmat** jelölnek, és ezért
     * nem lehetnek a distraktorai (CR-07). Az [id] maga nincs benne az eredményben.
     */
    fun synonymsOf(id: TechniqueId): Set<TechniqueId> =
        synonymGroups.asSequence()
            .filter { id in it }
            .flatten()
            .filterTo(LinkedHashSet()) { it != id }

    /**
     * Az [id]-hoz tartozó illusztráció tartalmi ujjlenyomata, vagy `null`, ha nincs képe.
     *
     * Két technika képe akkor számít vizuálisan azonosnak, ha az ujjlenyomatuk megegyezik –
     * ilyen pár nem kerülhet egyszerre a `JP_TO_IMAGE` opciói közé (T-109).
     */
    fun illustrationDigestOf(id: TechniqueId): String? = illustrationDigests[id]
}
