package hu.kyokushin.trainer.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A tartalom domain modelljének ([Technique], [Category], [IllustrationRef]) szerződése.
 *
 * Lefedett követelmények: DR-01 (technikarekord mezői), DR-02 (kategória),
 * CR-05 / FR-13 (kép-alapú kvíztípus csak valódi, kvízben használható képpel),
 * ADR-002 (háromállapotú státusz), ADR-010 (a `quizEligible` nem tárolt adat).
 */
class ContentModelTest {
    private fun technique(
        illustration: IllustrationRef? = null,
        status: VerificationStatus = VerificationStatus.AWAITING_REVIEW,
    ) = Technique(
        id = TechniqueId.of("technique-uke-0001"),
        categoryId = CategoryId.of("uke"),
        romaji = "gedan barai",
        romajiAscii = "gedan barai",
        hungarian = "lefelé söprő védés",
        illustration = illustration,
        status = status,
        datasetVersion = "0.1.0-demo",
    )

    @Test
    fun technique_carriesTheRequiredAndOptionalFields_DR01() {
        val t = technique()

        assertThat(t.id.value).isEqualTo("technique-uke-0001")
        assertThat(t.categoryId.value).isEqualTo("uke")
        assertThat(t.romaji).isEqualTo("gedan barai")
        assertThat(t.hungarian).isEqualTo("lefelé söprő védés")
        // Az opcionális mezők alapértelmezése nem „üres string", hanem null / üres lista.
        assertThat(t.japaneseScript).isNull()
        assertThat(t.descriptionHu).isNull()
        assertThat(t.aliases).isEmpty()
        assertThat(t.datasetVersion).isEqualTo("0.1.0-demo")
    }

    @Test
    fun technique_rejectsBlankMandatoryFields_DR01() {
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(romaji = "   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(hungarian = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(romajiAscii = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(datasetVersion = "")
        }
        // V009: üres alias nem kerülhet a domainbe.
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(aliases = listOf("uchi uke", " "))
        }
    }

    @Test
    fun technique_statusIsThreeStateNotBoolean_ADR002() {
        // A státusz típusa az enum, nem Boolean: a „hiányos" eset külön állapot.
        assertThat(technique(status = VerificationStatus.INCOMPLETE).status)
            .isEqualTo(VerificationStatus.INCOMPLETE)
        assertThat(VerificationStatus.entries).hasSize(3)
    }

    @Test
    fun technique_doesNotStoreQuizEligibility_ADR010() {
        // ADR-010: a kvízjogosultság származtatott érték. Ha tárolt mező lenne, két
        // igazságforrás keletkezne, és egy elavult `true` hiányos rekordot engedne kvízbe.
        val fieldNames = Technique::class.java.declaredFields.map { it.name }
        assertThat(fieldNames).doesNotContain("quizEligible")
        assertThat(fieldNames).doesNotContain("verified")
    }

    @Test
    fun technique_defaultsToTheConservativeContentIntegrityState_CR01() {
        val t = technique()
        assertThat(t.status).isEqualTo(VerificationStatus.AWAITING_REVIEW)
        assertThat(t.quizExcluded).isFalse()
        assertThat(t.retired).isFalse()
    }

    @Test
    fun imageQuizEligibility_isASingleTypeCheck_FR13() {
        val usable = technique(
            illustration = IllustrationRef.Asset(
                ref = "uke-gedan-barai",
                fileName = "uke-gedan-barai.webp",
                quizUsable = true,
                hideableOnStudyCard = true,
            ),
        )
        val captioned = technique(
            illustration = IllustrationRef.Asset(
                ref = "uke-gedan-barai-captioned",
                fileName = "uke-gedan-barai-captioned.webp",
                quizUsable = false,
                hideableOnStudyCard = false,
            ),
        )
        val placeholder = technique(illustration = IllustrationRef.Placeholder)
        val none = technique(illustration = null)

        // Ez a kifejezés a kép-alapú kvíztípusok (C, D) egyetlen kapuja (CR-05, FR-13).
        fun Technique.imageQuizUsable(): Boolean =
            illustration is IllustrationRef.Asset && illustration.quizUsable

        assertThat(usable.imageQuizUsable()).isTrue()
        assertThat(captioned.imageQuizUsable()).isFalse()
        assertThat(placeholder.imageQuizUsable()).isFalse()
        assertThat(none.imageQuizUsable()).isFalse()
    }

    @Test
    fun illustrationRef_hasExactlyThreeRepresentableCases_ADR003() {
        // A sealed hierarchia két altípusa + a nullable mező adja a három esetet;
        // a `when` kimerítő, ezért új altípus felvétele fordítási hibát ad a hívóknál.
        val cases: List<IllustrationRef?> = listOf(
            null,
            IllustrationRef.Placeholder,
            IllustrationRef.Asset("ref", "file.webp", quizUsable = true, hideableOnStudyCard = true),
        )
        val labels = cases.map {
            when (it) {
                null -> "none"
                IllustrationRef.Placeholder -> "placeholder"
                is IllustrationRef.Asset -> "asset"
            }
        }
        assertThat(labels).containsExactly("none", "placeholder", "asset").inOrder()
    }

    @Test
    fun illustrationAsset_rejectsBlankRefOrFileName_ADR003() {
        assertThrows(IllegalArgumentException::class.java) {
            IllustrationRef.Asset(" ", "file.webp", quizUsable = true, hideableOnStudyCard = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IllustrationRef.Asset("ref", "", quizUsable = true, hideableOnStudyCard = true)
        }
    }

    @Test
    fun illustrationPlaceholder_isASingleton_ADR003() {
        // `data object`: az egyenlőség azonosság, nincs példányonként eltérő helyőrző.
        assertThat(IllustrationRef.Placeholder).isSameInstanceAs(IllustrationRef.Placeholder)
    }

    @Test
    fun category_carriesContentOwnedHungarianNameAndSortOrder_DR02() {
        val category = Category(id = CategoryId.of("uke"), nameHu = "Védések", sortOrder = 3)

        assertThat(category.id.value).isEqualTo("uke")
        assertThat(category.nameHu).isEqualTo("Védések")
        assertThat(category.sortOrder).isEqualTo(3)
        // A magyar kategórianév a tartalomból jön (ADR-011), ezért adat, nem erőforrás-id.
        assertThrows(IllegalArgumentException::class.java) {
            Category(id = CategoryId.of("uke"), nameHu = "  ", sortOrder = 3)
        }
    }

    @Test
    fun technique_localizedName_fallsBackToHungarianWhenTranslationMissing_FR43() {
        val t = technique().copy(nameTranslations = mapOf("en" to "lower sweeping block"))

        assertThat(t.localizedName("en")).isEqualTo("lower sweeping block")
        // Nincs német fordítás -> magyar visszaesés, nem üres és nem angol.
        assertThat(t.localizedName("de")).isEqualTo(t.hungarian)
        // A "hu" explicit lekérdezése is a magyar mezőre esik, hiszen a map sosem
        // tartalmazza a "hu" kulcsot.
        assertThat(t.localizedName("hu")).isEqualTo(t.hungarian)
    }

    @Test
    fun technique_localizedDescription_fallsBackToHungarianDescription_FR43() {
        val t = technique().copy(
            descriptionHu = "leírás magyarul",
            descriptionTranslations = mapOf("en" to "description in English"),
        )

        assertThat(t.localizedDescription("en")).isEqualTo("description in English")
        assertThat(t.localizedDescription("de")).isEqualTo("leírás magyarul")

        // Hiányzó magyar leírás esetén a visszaesés is null, nem üres string.
        val withoutDescription = technique().copy(descriptionHu = null)
        assertThat(withoutDescription.localizedDescription("de")).isNull()
    }

    @Test
    fun technique_rejectsHungarianKeyInTranslationMaps_ADR013() {
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(nameTranslations = mapOf("hu" to "duplikált magyar"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            technique().copy(descriptionTranslations = mapOf("hu" to "duplikált magyar leírás"))
        }
    }

    @Test
    fun category_localizedName_fallsBackToHungarianWhenTranslationMissing_FR43() {
        val category = Category(
            id = CategoryId.of("uke"),
            nameHu = "Védések",
            sortOrder = 3,
            nameTranslations = mapOf("en" to "Blocks"),
        )

        assertThat(category.localizedName("en")).isEqualTo("Blocks")
        assertThat(category.localizedName("de")).isEqualTo("Védések")
        assertThrows(IllegalArgumentException::class.java) {
            category.copy(nameTranslations = mapOf("hu" to "Védések"))
        }
    }

    @Test
    fun contentModel_isImmutable_DR01() {
        // Minden konstruktorparaméter `val`: a modell példányai nem mutálhatók, a
        // tartalomfrissítés új példányt állít elő (ADR-004).
        val mutableSetters = (Technique::class.java.methods + Category::class.java.methods)
            .map { it.name }
            .filter { it.startsWith("set") }
        assertThat(mutableSetters).isEmpty()
    }
}
