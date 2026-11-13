package hu.kyokushin.trainer.core.quiz.validation

import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.VerificationStatus

/**
 * Az ADR-010 kvízjogosultsági kapu – **egyetlen** helyen megfogalmazva.
 *
 * ```
 * quizEligible(technique, type, trust) =
 *      technique.status != INCOMPLETE
 *   && hasFieldsFor(technique, type)
 *   && (trust == REVIEWED || trust == PRACTICE_ON_UNREVIEWED)
 * ```
 *
 * Miért nem a [Technique] mezője ez az érték? Mert **származtatott**: a státusz, a
 * szerkesztői kizárás, a visszavonás, a kérdéstípus mezőkövetelményei és a tudástár
 * bizalmi szintje együtt döntik el. Tárolva két igazságforrás lenne, és egy elavult
 * `true` hiányos rekordot engedne kvízbe (lásd a [Technique] osztálydokumentációját).
 *
 * A kapu **csak lefelé** mozdul (CR-02): nincs olyan paraméter, amivel egy
 * [VerificationStatus.INCOMPLETE] rekord vagy egy `quizUsable = false` kép átjutna rajta.
 * A validátor ezt a formulát a V007/V008 jelentéshez használja, a kvízgenerátor (T-108)
 * pedig a jelöltszűréshez – ugyanazt a függvényt, hogy a jelentés és a tényleges
 * viselkedés ne tudjon elválni.
 */
object QuizEligibility {
    /**
     * Átjut-e a technika a kapun az adott kérdéstípusnál és bizalmi szinten.
     *
     * @param technique a már elfogadott, domainbe emelt rekord.
     * @param questionType a vizsgált kérdéstípus; a mezőkövetelmény típusonként más.
     * @param datasetTrust a tudástár bizalmi szintje a manifestből (soha nem felhasználói
     *   beállításból – ADR-010).
     */
    fun isEligible(
        technique: Technique,
        questionType: QuestionType,
        datasetTrust: DatasetTrust,
    ): Boolean {
        if (technique.status == VerificationStatus.INCOMPLETE) return false
        if (technique.quizExcluded || technique.retired) return false
        if (!hasFieldsFor(technique, questionType)) return false
        return when (datasetTrust) {
            DatasetTrust.REVIEWED, DatasetTrust.PRACTICE_ON_UNREVIEWED -> true
        }
    }

    /** Van-e legalább egy kérdéstípus, amelyben a technika használható. */
    fun eligibleTypes(
        technique: Technique,
        datasetTrust: DatasetTrust,
    ): Set<QuestionType> = QuestionType.entries
        .filter { isEligible(technique, it, datasetTrust) }
        .toSet()

    /**
     * Megvannak-e a kérdéstípushoz **szükséges mezők** (V008, kvízmotor skill 1. pont).
     *
     * A szöveges típusoknál a `romaji` és a `hungarian` elegendő – ezek a [Technique]
     * kötelező mezői, tehát minden elfogadott rekordra teljesülnek. A képes típusoknál
     * viszont validált, kvízben **használható** [IllustrationRef.Asset] kell: a
     * [IllustrationRef.Placeholder] nem számít validált képnek, és a feliratos kép
     * (`quizUsable = false`) elárulná a megoldást.
     */
    fun hasFieldsFor(technique: Technique, questionType: QuestionType): Boolean =
        when (questionType) {
            QuestionType.JP_TO_HU, QuestionType.HU_TO_JP ->
                technique.romaji.isNotBlank() && technique.hungarian.isNotBlank()

            QuestionType.IMAGE_TO_JP, QuestionType.JP_TO_IMAGE ->
                technique.romaji.isNotBlank() && hasQuizUsableIllustration(technique)
        }

    /** Kvízben használható, validált képe van-e a rekordnak (ADR-003, ADR-011 kiegészítés). */
    fun hasQuizUsableIllustration(technique: Technique): Boolean =
        when (val illustration = technique.illustration) {
            is IllustrationRef.Asset -> illustration.quizUsable
            IllustrationRef.Placeholder, null -> false
        }
}
