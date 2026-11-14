package hu.kyokushin.trainer.core.model

/**
 * Egy technikakategória a tudástárból (DR-02).
 *
 * A magyar kategórianév **a tartalomból** jön (`manifest.json` → `categories`), nem a
 * kódból és nem a `strings.xml`-ből: új kategória felvétele tartalomcsere, nem
 * kódmódosítás (ADR-011). Ezért a [nameHu] itt adat, nem erőforrás-hivatkozás.
 *
 * A [sortOrder] a tartalom által diktált megjelenítési sorrend; a lista rendezése nem
 * ábécé szerinti, mert a kategóriák oktatási logikát követnek (állások → ütések → …).
 *
 * @property nameTranslations a [nameHu] további nyelvű fordításai, ISO 639-1
 *   nyelvkóddal kulcsolva – tetszőleges számú nyelv, nem fix mezőkészlet (FR-43,
 *   ADR-013). A `"hu"` kulcs sosem szerepel benne; hiányzó fordításra a
 *   [localizedName] mindig a [nameHu]-ra esik vissza.
 */
data class Category(
    val id: CategoryId,
    val nameHu: String,
    val sortOrder: Int,
    val nameTranslations: Map<String, String> = emptyMap(),
) {
    init {
        require(nameHu.isNotBlank()) { "Category.nameHu nem lehet üres (DR-02)." }
        require(!nameTranslations.containsKey("hu")) {
            "Category.nameTranslations nem tartalmazhat 'hu' kulcsot – a nameHu mező az alapnyelv (ADR-013)."
        }
    }
}

/** Lásd [Technique.localizedName] – ugyanaz a visszaesési szabály a kategórianévre (ADR-013). */
fun Category.localizedName(languageTag: String): String =
    nameTranslations[languageTag] ?: nameHu
