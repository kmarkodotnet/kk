package hu.kyokushin.trainer.core.ui.illustration

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException

/**
 * Az illusztrációk betöltése a csomagolt assetekből (`assets/knowledge/illustrations/`).
 *
 * Az app **offline-first** (NFR-01): képbetöltő könyvtár (Coil/Glide) és hálózat nincs,
 * a kép a csomagban van, ezért a betöltés egyetlen `AssetManager.open` + dekódolás.
 *
 * A tudástár képei nagyok (a jelenlegi készlet 1122×1402 px): teljes felbontásban
 * dekódolva egy kép ~6,3 MB-ot foglalna a heapen, miközben a megjelenítés néhány száz
 * pixel széles. Ezért a dekódolás **kétlépéses**: először csak a méret (`inJustDecodeBounds`),
 * majd a kettő hatvány léptékű ritkítás ([inSampleSizeFor]).
 */
internal object IllustrationAssetLoader {
    /** A tudástár illusztrációinak könyvtára az assetsben. */
    const val ILLUSTRATION_DIRECTORY: String = "knowledge/illustrations"

    /**
     * A dekódolt kép leghosszabb oldalának felső korlátja képpontban.
     *
     * A legnagyobb megjelenítési felület a részletező képernyő teljes szélességű ábrája;
     * egy 1024 px-es él a legsűrűbb (xxxhdpi) kijelzőn is elég, és a jelenlegi
     * 1122×1402-es készletnél `inSampleSize = 2`-t, azaz negyedannyi memóriát jelent.
     */
    private const val MAX_DIMENSION_PX: Int = 1024

    /**
     * Beolvassa és a megjelenítéshez méretezve dekódolja a képet.
     *
     * @return a dekódolt bitmap, vagy `null`, ha a fájl nincs a csomagban, vagy nem
     *   dekódolható. Nem dob kivételt: a hívó ilyenkor helyőrzőre vált, ahogy a
     *   hibakezelési mátrix (FUNCTIONAL_SPEC 10., „Hiányzó képfájl") előírja –
     *   **nincs összeomlás**.
     */
    fun load(assets: AssetManager, fileName: String): Bitmap? {
        val path = "$ILLUSTRATION_DIRECTORY/$fileName"
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = inSampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
            }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: IOException) {
            null
        } catch (_: OutOfMemoryError) {
            // Szándékosan elkapva: egy nagy, hibás méretadatú kép sem viheti el az appot.
            null
        }
    }
}

/**
 * A `BitmapFactory.Options.inSampleSize` értéke: a legkisebb kettőhatvány, amellyel a
 * ritkított kép leghosszabb oldala már nem nagyobb [maxDimension]-nél.
 *
 * Tiszta függvény, hogy a méretezési szabály Android nélkül tesztelhető legyen.
 * Ismeretlen méret (a `BitmapFactory` hibánál −1-et ad) vagy nem pozitív korlát esetén
 * `1`, azaz nincs ritkítás – rosszabb memóriahasználat, mint a hibás kép.
 */
internal fun inSampleSizeFor(sourceWidth: Int, sourceHeight: Int, maxDimension: Int): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || maxDimension <= 0) return 1
    var sampleSize = 1
    var longestEdge = maxOf(sourceWidth, sourceHeight)
    while (longestEdge > maxDimension) {
        longestEdge /= 2
        sampleSize *= 2
    }
    return sampleSize
}
