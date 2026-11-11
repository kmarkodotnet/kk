package hu.kyokushin.trainer.core.data.db

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.validation.CategoryInput
import org.junit.Test

/**
 * A tartalom oldali entitás ↔ domain leképezés (ADR-003, ADR-004, ADR-010, DR-02).
 *
 * Tiszta JVM teszt, Robolectric nélkül: a leképezés nem nyúl adatbázishoz. Amit mér: a
 * körbejárás **veszteségmentes**, és az illusztráció három állapota (nincs / helyőrző /
 * valódi kép) négy nullázható oszlopon át is megőrződik.
 */
class ContentEntityMappersTest {

    @Test
    fun technique_roundTripsThroughTheEntityWithoutLoss_DR01() {
        val technique = technique(
            illustration = IllustrationRef.Asset(
                ref = "KY-0044",
                fileName = "KY-0044.jpg",
                quizUsable = true,
                hideableOnStudyCard = false,
            ),
            aliases = listOf("Zenkucu dachi"),
        )

        val restored = technique.toEntity().toTechnique(aliases = technique.aliases)

        assertThat(restored).isEqualTo(technique)
    }

    @Test
    fun technique_illustrationRefKeepsItsThreeStates_ADR003() {
        val none = technique(illustration = null).toEntity()
        val placeholder = technique(illustration = IllustrationRef.Placeholder).toEntity()
        val asset = technique(
            illustration = IllustrationRef.Asset("KY-0044", "KY-0044.jpg", quizUsable = false, hideableOnStudyCard = true),
        ).toEntity()

        assertThat(none.illustrationKind).isNull()
        assertThat(none.toIllustrationRef()).isNull()
        assertThat(placeholder.illustrationKind).isEqualTo(IllustrationKind.PLACEHOLDER)
        assertThat(placeholder.toIllustrationRef()).isEqualTo(IllustrationRef.Placeholder)
        assertThat(asset.illustrationKind).isEqualTo(IllustrationKind.ASSET)
        // A kvízkapuk nem csúsznak el a tároláson: egy téves `true` feliratos képet
        // engedne kvízbe (ADR-010).
        assertThat(asset.toIllustrationRef()).isEqualTo(
            IllustrationRef.Asset("KY-0044", "KY-0044.jpg", quizUsable = false, hideableOnStudyCard = true),
        )
    }

    @Test
    fun techniqueAlias_normalizedFormIsDerivedOnImport_FR03() {
        val entities = technique(aliases = listOf("Zenkutsu  DACHI", "Maé-geri")).toAliasEntities()

        assertThat(entities.map { it.alias }).containsExactly("Zenkutsu  DACHI", "Maé-geri")
        // Kisbetű, ékezet nélkül, összehúzott whitespace – a keresés és a V006 ugyanezt látja.
        assertThat(entities.map { it.aliasNormalized }).containsExactly("zenkutsu dachi", "mae-geri")
    }

    @Test
    fun category_roundTripsAndKeepsTheContentDrivenOrder_DR02() {
        val category = Category(id = CategoryId.of("stances"), nameHu = "Állások", sortOrder = 3)

        assertThat(category.toEntity().toCategory()).isEqualTo(category)
    }

    @Test
    fun categoryInput_missingSortOrderFallsBackToTheArrayOrder_DR02() {
        val entities = listOf(
            CategoryInput(id = "stances", nameHu = "Állások", sortOrder = 7),
            CategoryInput(id = "tsuki", nameHu = "Ütések"),
            // Azonosító nélküli bejegyzés nem képezhető le: technika sem hivatkozhat rá.
            CategoryInput(id = "  ", nameHu = "Semmi"),
            // Név nélküli kategória az azonosítóját mutatja – kitalált magyar nevet nem
            // gyártunk (CR-01).
            CategoryInput(id = "kata"),
        ).toCategoryEntities()

        assertThat(entities.map { it.id.value }).containsExactly("stances", "tsuki", "kata").inOrder()
        assertThat(entities.map { it.sortOrder }).containsExactly(7, 1, 3).inOrder()
        assertThat(entities.last().nameHu).isEqualTo("kata")
    }

    @Test
    fun synonymGroups_areStoredDeterministically_CR07() {
        val first = TechniqueId.of("KY-0002")
        val second = TechniqueId.of("KY-0001")

        val members = listOf(setOf(first, second), setOf(TechniqueId.of("KY-0009"))).toSynonymGroupEntities()

        assertThat(members.map { it.groupId to it.techniqueId.value }).containsExactly(
            0 to "KY-0001",
            0 to "KY-0002",
            1 to "KY-0009",
        ).inOrder()
    }

    private fun technique(
        illustration: IllustrationRef? = null,
        aliases: List<String> = emptyList(),
    ) = Technique(
        id = TechniqueId.of("KY-0001"),
        categoryId = CategoryId.of("stances"),
        romaji = "Zenkutsu dachi",
        romajiAscii = "Zenkutsu dachi",
        hungarian = "Elöl álló állás",
        japaneseScript = null,
        aliases = aliases,
        descriptionHu = "Hosszú, elöl súlyozott állás.",
        illustration = illustration,
        status = VerificationStatus.AWAITING_REVIEW,
        quizExcluded = false,
        retired = false,
        datasetVersion = "1.0.0",
    )
}
