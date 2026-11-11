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
 */
data class Category(
    val id: CategoryId,
    val nameHu: String,
    val sortOrder: Int,
) {
    init {
        require(nameHu.isNotBlank()) { "Category.nameHu nem lehet üres (DR-02)." }
    }
}
