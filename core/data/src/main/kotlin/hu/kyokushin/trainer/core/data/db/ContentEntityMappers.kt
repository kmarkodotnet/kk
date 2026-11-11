package hu.kyokushin.trainer.core.data.db

import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.text.NameNormalizer
import hu.kyokushin.trainer.core.quiz.validation.CategoryInput

/**
 * A **tartalom** oldali entitás ↔ domain leképezés (ADR-001 L3/L4, ARCHITECTURE 5.1).
 *
 * Ez a réteg az, ami miatt a Room entitás nem szivárog ki a `core/data`-ból: a
 * repository (T-201) és a tartalomimport (T-117) ugyanezt a néhány függvényt használja,
 * és minden típus `internal`. A felhasználói oldali leképezés (`quiz_attempt`,
 * `study_review`) a T-301/T-401 tárgya, szándékosan nincs itt.
 *
 * Három szabály, amit a leképezés betart:
 *
 * 1. **Nem validál és nem pótol.** A tartalmi szabályokat a `core/quiz` validátora
 *    érvényesíti, és a [Technique] domain konstruktora `require()`-rel zár. Ide már csak
 *    elfogadott rekord jut – egy hiányos sorra a visszaalakítás *dobjon*, mert az
 *    adatbázisban lévő érvénytelen sor programhiba, nem tartalmi hiba.
 * 2. **Nem emel státuszt** (CR-02): a [Technique.status] és az illusztráció kvízkapui
 *    oda-vissza változatlanul mennek át.
 * 3. **A származtatott oszlopok itt egyetlen helyen keletkeznek**: a
 *    [TechniqueAliasEntity.aliasNormalized] a [NameNormalizer] kimenete, hogy a keresés
 *    (FR-03) és a V006 szinonimavizsgálat bitre ugyanazt az alakot lássa.
 */

/** Domain → entitás. A `quizEligible` szándékosan nincs oszlop (ADR-010). */
internal fun Technique.toEntity(): TechniqueEntity = TechniqueEntity(
    id = id,
    categoryId = categoryId,
    romaji = romaji,
    romajiAscii = romajiAscii,
    hungarian = hungarian,
    japaneseScript = japaneseScript,
    descriptionHu = descriptionHu,
    status = status,
    quizExcluded = quizExcluded,
    retired = retired,
    datasetVersion = datasetVersion,
    illustrationKind = illustration.toKind(),
    illustrationRef = (illustration as? IllustrationRef.Asset)?.ref,
    illustrationFileName = (illustration as? IllustrationRef.Asset)?.fileName,
    illustrationQuizUsable = (illustration as? IllustrationRef.Asset)?.quizUsable,
    illustrationHideableOnStudyCard = (illustration as? IllustrationRef.Asset)?.hideableOnStudyCard,
)

/**
 * Entitás → domain.
 *
 * Az aliasok külön táblában élnek, ezért **paraméter**: a hívónak egyetlen
 * `technique_alias` lekérdezésből kell feltöltenie az egész listát, nem technikánként
 * külön kérdéssel (NFR-10).
 */
internal fun TechniqueEntity.toTechnique(aliases: List<String> = emptyList()): Technique = Technique(
    id = id,
    categoryId = categoryId,
    romaji = romaji,
    romajiAscii = romajiAscii,
    hungarian = hungarian,
    japaneseScript = japaneseScript,
    aliases = aliases,
    descriptionHu = descriptionHu,
    illustration = toIllustrationRef(),
    status = status,
    quizExcluded = quizExcluded,
    retired = retired,
    datasetVersion = datasetVersion,
)

/**
 * A négy illusztrációoszlop visszaalakítása az [IllustrationRef] három állapotára
 * (ADR-003).
 *
 * A [TechniqueEntity.illustrationKind] vezérel: `null` → nincs hivatkozás,
 * [IllustrationKind.PLACEHOLDER] → helyőrző, [IllustrationKind.ASSET] → valódi kép. Az
 * `ASSET` ághoz a `ref` és a `fileName` kötelező; ha hiányzik, az adatbázis sora
 * inkonzisztens, és a [IllustrationRef.Asset] konstruktora joggal dob.
 */
internal fun TechniqueEntity.toIllustrationRef(): IllustrationRef? = when (illustrationKind) {
    null -> null
    IllustrationKind.PLACEHOLDER -> IllustrationRef.Placeholder
    IllustrationKind.ASSET -> IllustrationRef.Asset(
        ref = requireNotNull(illustrationRef) {
            "A technique.illustrationRef nem lehet null ASSET illusztrációnál (id = $id)."
        },
        fileName = requireNotNull(illustrationFileName) {
            "A technique.illustrationFileName nem lehet null ASSET illusztrációnál (id = $id)."
        },
        // A kvízkapuk hiányzó értéke a szigorúbb olvasatot kapja: feliratos kép elárulná a
        // megoldást, és kapuszintet kódból nem emelünk (CR-02, ADR-010).
        quizUsable = illustrationQuizUsable == true,
        hideableOnStudyCard = illustrationHideableOnStudyCard == true,
    )
}

/** A tárolt alakhoz tartozó megkülönböztető (ADR-003). */
private fun IllustrationRef?.toKind(): IllustrationKind? = when (this) {
    null -> null
    is IllustrationRef.Asset -> IllustrationKind.ASSET
    IllustrationRef.Placeholder -> IllustrationKind.PLACEHOLDER
}

/**
 * Egy technika aliasai `technique_alias` sorokká.
 *
 * A normalizált alak itt keletkezik, mert az importnak kell egyetlen igazságforrásként
 * előállítania (FR-03). Üres alias nem kerülhet ide: azt a validátor már eldobta (V009).
 */
internal fun Technique.toAliasEntities(): List<TechniqueAliasEntity> = aliases.map { alias ->
    TechniqueAliasEntity(
        techniqueId = id,
        alias = alias,
        aliasNormalized = NameNormalizer.normalize(alias),
    )
}

/** Entitás → domain a kategórián. */
internal fun CategoryEntity.toCategory(): Category = Category(
    id = id,
    nameHu = nameHu,
    sortOrder = sortOrder,
)

/** Domain → entitás a kategórián. */
internal fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    nameHu = nameHu,
    sortOrder = sortOrder,
)

/**
 * A manifest kategórialistája `category` sorokká (DR-02, ADR-011).
 *
 * A manifest **az egyetlen hiteles kategórialista**, ezért a leképezés nem a technikák
 * `categoryId` értékeiből következtet. Két tudatos döntés:
 *
 * - **Hiányzó `sort_order`** esetén a tömbbeli sorrend érvényes (`CategoryDto`), mert
 *   a kategóriák oktatási logikát követnek, nem ábécét.
 * - **Hiányzó `name_hu`** esetén az azonosító marad a megjelenítendő név. Ez nem kitalált
 *   magyar megnevezés (CR-01), hanem látható jelzés a tartalomszerkesztőnek – és azért
 *   kell egyáltalán érték, mert a `technique.categoryId` idegen kulcsa megkívánja a
 *   kategóriasort, a [Category] domain modell pedig nem engedi a blank nevet. A
 *   kategóriamezők validációjára nincs szabálykód (V001…V010); ez javasolt következő
 *   feladat.
 *
 * Az üres azonosítójú bejegyzés kimarad: ilyen kategóriára technika sem hivatkozhat (a
 * validátor V004-gyel veti el), és a [CategoryId] sem állítható elő belőle.
 */
internal fun List<CategoryInput>.toCategoryEntities(): List<CategoryEntity> =
    mapIndexedNotNull { index, category ->
        val id = category.id?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapIndexedNotNull null
        CategoryEntity(
            id = CategoryId.of(id),
            nameHu = category.nameHu?.trim()?.takeIf { it.isNotEmpty() } ?: id,
            sortOrder = category.sortOrder ?: index,
        )
    }

/**
 * A validátor szinonimacsoportjai `synonym_group` sorokká (V006, CR-07).
 *
 * A `groupId` a lista indexe, és **nem stabil azonosító**: az importnak minden futáskor
 * ürítenie kell a táblát, mert a csoportok újraszámolódnak. Felhasználói adat nem
 * hivatkozik rá, ezért ez az egyetlen tábla, amelynél a teljes csere megengedett
 * (ADR-004).
 *
 * A csoporton belüli sorrend a [TechniqueId] szerint determinisztikus: a `Set` iterációs
 * sorrendje nem szerződés, az adatbázis tartalma viszont kétszer ugyanarra a bemenetre
 * legyen ugyanaz (NFR-07).
 */
internal fun List<Set<TechniqueId>>.toSynonymGroupEntities(): List<SynonymGroupMemberEntity> =
    flatMapIndexed { groupId, members ->
        members.sortedBy { it.value }.map { techniqueId ->
            SynonymGroupMemberEntity(groupId = groupId, techniqueId = techniqueId)
        }
    }
