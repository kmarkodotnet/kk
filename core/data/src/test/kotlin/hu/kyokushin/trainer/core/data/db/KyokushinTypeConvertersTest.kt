package hu.kyokushin.trainer.core.data.db

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

/**
 * A Room oszlop-konverterek tesztjei (DR-08).
 *
 * A konverterek sima Kotlin osztályt alkotnak, ezért Android nélkül, JVM-en tesztelhetők –
 * és ez a réteg az, ahol a séma és a domain között adat romolhat.
 */
class KyokushinTypeConvertersTest {
    private val converters = KyokushinTypeConverters()

    // --- Instant ↔ Long -----------------------------------------------------------

    @Test
    fun converter_instant_roundTripsThroughEpochMilli_DR08() {
        val instant = Instant.ofEpochMilli(1_762_000_000_000L)

        val stored = converters.fromInstant(instant)

        assertThat(stored).isEqualTo(1_762_000_000_000L)
        assertThat(converters.toInstant(stored)).isEqualTo(instant)
    }

    @Test
    fun converter_instant_nullStaysNullForUnfinishedSession_FR19() {
        // A megszakított kvíz munkamenete befejezetlen marad: a `finishedAt` NULL.
        assertThat(converters.fromInstant(null)).isNull()
        assertThat(converters.toInstant(null)).isNull()
    }

    @Test
    fun converter_instant_epochAndNegativeValuesSurvive_DR08() {
        val epoch = Instant.EPOCH
        val beforeEpoch = Instant.ofEpochMilli(-1_000L)

        assertThat(converters.toInstant(converters.fromInstant(epoch))).isEqualTo(epoch)
        assertThat(converters.toInstant(converters.fromInstant(beforeEpoch))).isEqualTo(beforeEpoch)
    }

    @Test
    fun converter_instant_subMillisecondPrecisionIsIntentionallyTruncated_DR08() {
        // Az epoch milli ábrázolás tudatosan veszíti a nanoszekundumos részt: a tárolt
        // érték a milliszekundumra csonkolt időpont, nem kerekített.
        val withNanos = Instant.ofEpochSecond(1_762_000_000L, 123_456_789L)

        val restored = converters.toInstant(converters.fromInstant(withNanos))

        assertThat(restored).isEqualTo(Instant.ofEpochSecond(1_762_000_000L, 123_000_000L))
    }

    // --- Enum ↔ String (a `name`, nem az `ordinal`) -------------------------------

    @Test
    fun converter_enums_arePersistedByNameNotOrdinal_DR08() {
        // Ha az ordinal mennénk ki, egy enum-konstans átrendezése csendben átértelmezné a
        // már eltárolt sorokat. A tárolt alak ezért mindig a konstans neve.
        assertThat(converters.fromQuestionType(QuestionType.IMAGE_TO_JP)).isEqualTo("IMAGE_TO_JP")
        assertThat(converters.fromSelfRating(SelfRating.NEEDS_PRACTICE)).isEqualTo("NEEDS_PRACTICE")
        assertThat(converters.fromVerificationStatus(VerificationStatus.AWAITING_REVIEW))
            .isEqualTo("AWAITING_REVIEW")
        assertThat(converters.fromDatasetTrust(DatasetTrust.PRACTICE_ON_UNREVIEWED))
            .isEqualTo("PRACTICE_ON_UNREVIEWED")
        assertThat(converters.fromIllustrationKind(IllustrationKind.PLACEHOLDER))
            .isEqualTo("PLACEHOLDER")

        // Egyik tárolt alak sem szám: ordinal-ra visszaállás fordítási hiba nélkül is kibukik.
        listOf("IMAGE_TO_JP", "NEEDS_PRACTICE", "AWAITING_REVIEW", "PRACTICE_ON_UNREVIEWED")
            .forEach { assertThat(it.toIntOrNull()).isNull() }
    }

    @Test
    fun converter_enums_everyConstantRoundTrips_DR08() {
        QuestionType.entries.forEach {
            assertThat(converters.toQuestionType(converters.fromQuestionType(it))).isEqualTo(it)
        }
        SelfRating.entries.forEach {
            assertThat(converters.toSelfRating(converters.fromSelfRating(it))).isEqualTo(it)
        }
        VerificationStatus.entries.forEach {
            assertThat(converters.toVerificationStatus(converters.fromVerificationStatus(it)))
                .isEqualTo(it)
        }
        DatasetTrust.entries.forEach {
            assertThat(converters.toDatasetTrust(converters.fromDatasetTrust(it))).isEqualTo(it)
        }
        IllustrationKind.entries.forEach {
            assertThat(converters.toIllustrationKind(converters.fromIllustrationKind(it)))
                .isEqualTo(it)
        }
    }

    @Test
    fun converter_enums_unknownStoredValueThrowsInsteadOfSilentDefault_CR02() {
        // Egy olvashatatlan státuszt nem szabad engedékenyebb értékre értelmezni: a
        // hitelesítési szintet kódból soha nem emeljük (CR-02).
        val failure = assertThrows(IllegalArgumentException::class.java) {
            converters.toVerificationStatus("VERIFIED_BY_MISTAKE")
        }
        assertThat(failure).hasMessageThat().contains("VerificationStatus")
        assertThat(failure).hasMessageThat().contains("VERIFIED_BY_MISTAKE")

        assertThrows(IllegalArgumentException::class.java) { converters.toQuestionType("JP_TO_XX") }
        assertThrows(IllegalArgumentException::class.java) { converters.toSelfRating("MAYBE") }
        assertThrows(IllegalArgumentException::class.java) { converters.toDatasetTrust("TRUSTED") }
        assertThrows(IllegalArgumentException::class.java) { converters.toIllustrationKind("PHOTO") }
    }

    @Test
    fun converter_illustrationKind_nullMeansNoIllustrationReference_ADR003() {
        // A három állapot közül a „nincs hivatkozás" a NULL oszlopérték (ADR-003).
        assertThat(converters.fromIllustrationKind(null)).isNull()
        assertThat(converters.toIllustrationKind(null)).isNull()
    }

    // --- Azonosító value classok ---------------------------------------------------

    @Test
    fun converter_identifiers_roundTripThroughTheirFactory_DR03() {
        val techniqueId = TechniqueId.of("KY-0042")
        val categoryId = CategoryId.of("kicks")
        val sessionId = SessionId.of("session-1")
        val questionId = QuestionId.of("q-1")

        assertThat(converters.fromTechniqueId(techniqueId)).isEqualTo("KY-0042")
        assertThat(converters.toTechniqueId("KY-0042")).isEqualTo(techniqueId)
        assertThat(converters.toCategoryId(converters.fromCategoryId(categoryId)))
            .isEqualTo(categoryId)
        assertThat(converters.toSessionId(converters.fromSessionId(sessionId))).isEqualTo(sessionId)
        assertThat(converters.toQuestionId(converters.fromQuestionId(questionId)))
            .isEqualTo(questionId)
    }

    @Test
    fun converter_identifiers_blankStoredValueIsRejectedByTheFactory_DR01() {
        // A value class konstruktora privát: a visszaolvasás is az `of()` gyáron megy át,
        // így üres azonosító nem kerülhet be a domainbe.
        assertThrows(IllegalArgumentException::class.java) { converters.toTechniqueId("   ") }
        assertThrows(IllegalArgumentException::class.java) { converters.toCategoryId("") }
        assertThrows(IllegalArgumentException::class.java) { converters.toSessionId("") }
        assertThrows(IllegalArgumentException::class.java) { converters.toQuestionId("\t") }
    }
}
