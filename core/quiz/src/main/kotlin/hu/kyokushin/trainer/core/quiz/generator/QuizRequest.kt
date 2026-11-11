package hu.kyokushin.trainer.core.quiz.generator

import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType

/**
 * A kvízgenerátor teljes bemenete (FR-11…FR-15, NFR-07).
 *
 * A kérés **önhordó**: a motornak ezen kívül semmilyen forrása nincs – nincs repository,
 * nincs óra, nincs `Random.Default`. Ez az, ami a determinizmust egyáltalán értelmezhetővé
 * teszi: ugyanez a `QuizRequest` ugyanazt a [QuizPlan]-t adja, bitre, az opciók
 * sorrendjével együtt (ADR-007, T-110).
 *
 * @property candidates a szóba jöhető technikák a szinonimacsoportokkal és a
 *   képujjlenyomatokkal (`TechniqueRepository.getQuizCandidates`).
 * @property enabledTypes a felhasználó által engedélyezett kérdéstípusok (FR-12). Ami nem
 *   szerepel benne, az a tervben [SkipReason.TYPE_DISABLED] okkal jelenik meg. Üres halmaz
 *   érvényes bemenet: a kvíz ekkor nem indítható, és ezt a terv megindokolja – nem kivétel.
 * @property categoryFilter a választott kategóriák; **üres halmaz = minden kategória**
 *   (FR-11). Azért üres halmaz a „mind", és nem `null`, mert a beállító képernyő
 *   kijelölésmentes állapota pontosan ezt jelenti.
 * @property requestedCount a kért kérdésszám (5 / 10 / 20 – FR-13). Felső korlát, nem
 *   garancia: ha kevesebb jön össze, a terv [Shortfall]-t hordoz (FR-22).
 * @property seed a seedelt RNG magja. A hívó **injektált** RNG-ből sorsolja, és elteszi a
 *   munkamenetbe és a mentett állapotba is (ADR-007); a motor soha nem állít elő seedet.
 * @property datasetTrust a betöltött tudástár bizalmi szintje a manifestből, a jogosultsági
 *   kapu bemenete (ADR-010). **Soha nem felhasználói beállítás:** a tartalom dönti el,
 *   ezért a kérés is a validátor jelentéséből kapja, nem a beállító képernyőről.
 */
data class QuizRequest(
    val candidates: QuizCandidateSet,
    val enabledTypes: Set<QuestionType>,
    val categoryFilter: Set<CategoryId> = emptySet(),
    val requestedCount: Int,
    val seed: Long,
    val datasetTrust: DatasetTrust,
) {
    init {
        require(requestedCount > 0) {
            "A kért kérdésszám pozitív; nulla kérdésre nincs értelmes terv (FR-13)."
        }
    }

    /** Minden kategória szóba jön (a beállító képernyőn nincs kijelölt kategória). */
    val allCategories: Boolean
        get() = categoryFilter.isEmpty()
}
