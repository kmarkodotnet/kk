package hu.kyokushin.trainer.core.quiz.validation

/**
 * A validátor-tesztek közös bemenetépítői (TR-01).
 *
 * Miért közös fájl, és nem osztályonkénti privát segédek? A TR-01 készlet három
 * tesztosztályra oszlik – szabálytáblázat ([KnowledgeValidationRuleTableTest]),
 * szerződési garanciák ([KnowledgeValidatorGuaranteesTest]) és az egyedi
 * esetek ([DefaultKnowledgeValidatorTest]) –, és mindháromnak **ugyanazt** az
 * „egyébként érvényes" alapállapotot kell látnia. Ha a builder három példányban élne, egy
 * default érték megváltoztatása csendben kettéválasztaná a táblázatot az egyedi
 * esetektől, és egy szabály úgy tűnhetne lefedettnek, hogy valójában más bemeneten fut.
 *
 * Mindkét builder **szándékosan érvényes** alapértékekkel indul: a tesztek egy-egy mezőt
 * rontanak el, így a teszt neve és a módosított paraméter együtt elmondja, mit vizsgál.
 */

/**
 * Egy érvényes manifest két kategóriával (`etiquette`, `geri`).
 *
 * @param schemaVersion a `schema_version` nyers alakja; `null` és nem támogatott érték is
 *   megadható (V001).
 * @param contentVersion a `dataset_version`; `null` esetén egyetlen rekord sem képezhető
 *   le a domainre (V002, a [hu.kyokushin.trainer.core.model.Technique.datasetVersion]
 *   kötelező mező miatt).
 * @param datasetKind az `editorial_status`; csak a `reviewed` ad
 *   [hu.kyokushin.trainer.core.model.DatasetTrust.REVIEWED]-ot (CR-02).
 * @param illustrations a licencnyilvántartás; alapból üres, mert a rekordok alapból nem
 *   hivatkoznak képre.
 * @param categories a kategórialista felülírható, hogy a V004 „nincs egyetlen érvényes
 *   kategória sem" esete is leírható legyen.
 */
internal fun manifest(
    schemaVersion: String? = "1.0.0",
    contentVersion: String? = "0.1.0",
    datasetKind: String? = "draft_review_required",
    illustrations: List<IllustrationInput> = emptyList(),
    categories: List<CategoryInput> = listOf(
        CategoryInput(id = "etiquette", nameHu = "Etikett és köszönések", sortOrder = 1),
        CategoryInput(id = "geri", nameHu = "Rúgások", sortOrder = 2),
    ),
) = ManifestInput(
    schemaVersion = schemaVersion,
    contentVersion = contentVersion,
    datasetKind = datasetKind,
    categories = categories,
    illustrations = illustrations,
)

/**
 * Egy olyan technikarekord, amely minden szabályon átjut – a táblázatos esetek ezt
 * rontják el egyetlen mezőben.
 */
internal fun validEntry(
    id: String? = "KY-0001",
    categoryId: String? = "geri",
    romaji: String? = "Mae geri",
    hungarian: String? = "Előre rúgás",
    aliases: List<String?> = emptyList(),
    illustration: String? = null,
    verified: Boolean? = null,
    quizEligible: Boolean? = null,
) = TechniqueInput(
    id = id,
    categoryId = categoryId,
    romaji = romaji,
    hungarian = hungarian,
    aliases = aliases,
    illustration = illustration,
    verified = verified,
    quizEligible = quizEligible,
)
