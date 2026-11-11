package hu.kyokushin.trainer.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A domain enumok szerződése. Ezek a halmazok és a nevek a perzisztencia és a UI
 * szerződésének részei, ezért tesztelt invariánsok, nem puszta deklarációk.
 *
 * Lefedett követelmények: FR-12 (négy kérdéstípus), FR-10 (önértékelés két állapota),
 * CR-01/CR-02 (hitelesítési státusz nem emelhető kódból), DR-01.
 */
class DomainEnumsTest {

    @Test
    fun questionType_hasExactlyTheFourSpecifiedTypes_FR12() {
        assertThat(QuestionType.entries.map { it.name })
            .containsExactly("JP_TO_HU", "HU_TO_JP", "IMAGE_TO_JP", "JP_TO_IMAGE")
    }

    @Test
    fun selfRating_hasExactlyTwoStates_FR10() {
        // Két állapot: nincs sokfokozatú skála és nincs ismétlésütemezés az MVP-ben.
        assertThat(SelfRating.entries.map { it.name })
            .containsExactly("KNEW", "NEEDS_PRACTICE")
    }

    @Test
    fun verificationStatus_hasThreeStates_CR01() {
        assertThat(VerificationStatus.entries.map { it.name })
            .containsExactly("VERIFIED", "AWAITING_REVIEW", "INCOMPLETE")
    }

    @Test
    fun datasetTrust_hasExactlyTwoLevels_CR01() {
        assertThat(DatasetTrust.entries.map { it.name })
            .containsExactly("REVIEWED", "PRACTICE_ON_UNREVIEWED")
    }

    @Test
    fun enums_roundTripByNameNotByOrdinal_DR01() {
        // A Room oszlopok az enum NEVÉT tárolják (ADR-002), ezért a név alapú
        // visszaalakításnak minden értékre működnie kell.
        QuestionType.entries.forEach { assertThat(QuestionType.valueOf(it.name)).isEqualTo(it) }
        SelfRating.entries.forEach { assertThat(SelfRating.valueOf(it.name)).isEqualTo(it) }
        VerificationStatus.entries.forEach {
            assertThat(VerificationStatus.valueOf(it.name)).isEqualTo(it)
        }
        DatasetTrust.entries.forEach { assertThat(DatasetTrust.valueOf(it.name)).isEqualTo(it) }
    }

    @Test
    fun verificationStatus_hasNoCodePathThatRaisesStatus_CR02() {
        // CR-02: a hitelesítési státusz kódból nem emelhető. A domain típus ezért
        // viselkedés nélküli: nincs rajta egyetlen olyan deklarált tagfüggvény sem,
        // amely új státuszt adna vissza (pl. markVerified / promote).
        val declaredReturningSelf = VerificationStatus::class.java.declaredMethods
            .filter { it.returnType == VerificationStatus::class.java }
            .map { it.name }
            .filterNot { it == "valueOf" || it == "values" }

        assertThat(declaredReturningSelf).isEmpty()
    }
}
