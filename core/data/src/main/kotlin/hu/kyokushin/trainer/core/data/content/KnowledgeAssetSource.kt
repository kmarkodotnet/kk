package hu.kyokushin.trainer.core.data.content

import android.content.res.AssetManager
import java.io.IOException

/**
 * A csomagolt tudástár fájljainak elérése (FR-29).
 *
 * Miért interfész, és miért nem közvetlenül [AssetManager]? Mert a tartalombeolvasás és a
 * leképezés szabályai a kanonikus sémáról szólnak (ADR-011), nem az Android
 * assetkezeléséről. Így a [KnowledgeContentReader] tiszta JVM unit teszttel ellenőrizhető
 * – emulátor, Robolectric és csomagolt APK nélkül –, az Android-specifikus rész pedig
 * egyetlen vékony delegáló osztályra zsugorodik.
 *
 * A megvalósítás hibát **kivétellel** jelez ([IOException]); a kivételt a
 * [KnowledgeContentReader] fogja el, és `V010` észrevétellé fordítja. Ez a modul belső
 * típusa (ADR-001, L3): a `core/data`-ból nem szivárog ki.
 */
internal interface KnowledgeAssetSource {
    /**
     * A [relativePath] fájl teljes szöveges tartalma UTF-8 kódolással.
     *
     * @throws IOException ha a fájl nem létezik vagy nem olvasható.
     */
    fun readText(relativePath: String): String

    /**
     * A [relativeDirectory] könyvtárban található bejegyzések neve, útvonal nélkül.
     *
     * A validátor `availableIllustrationFiles` bemenete ebből készül, és a V005 szabály a
     * manifest `file` értékének az utolsó útvonalszegmensét hasonlítja ide – ezért itt
     * **csupasz fájlnév** a szerződés, nem útvonal.
     *
     * Nem létező könyvtár üres halmazt ad, nem kivételt: a „nincs csomagolt kép" állapot a
     * V005 szabály rendes bemenete (hiányzó fájl → `Placeholder`), nem betöltési hiba.
     */
    fun listFileNames(relativeDirectory: String): Set<String>
}

/**
 * A csomagolt tudástár fájlelrendezése (ADR-011).
 *
 * A fájlnevek a kapott átadási csomag elnevezését követik (`entries.json`, nem
 * `techniques.json`), mert az `app/src/main/assets/knowledge/` a
 * `knowledgebase_transfer/` másolata, és nincs konverziós réteg a kettő között.
 */
internal object KnowledgeAssetLayout {
    /** A tudástár gyökérkönyvtára az assetsben. */
    const val ROOT: String = "knowledge"

    /** Sémaverzió, tartalomverzió, kategóriák, illusztráció-nyilvántartás. */
    const val MANIFEST_FILE: String = "manifest.json"

    /** A technikarekordok (a kanonikus sémában 161 darab). */
    const val ENTRIES_FILE: String = "entries.json"

    /** A csomagolt képfájlok könyvtára. */
    const val ILLUSTRATIONS_DIRECTORY: String = "illustrations"
}

/**
 * Az [KnowledgeAssetSource] Android-megvalósítása a csomagolt assetek felett.
 *
 * Szándékosan logikamentes: egyetlen felelőssége a [rootDirectory] alatti útvonal
 * összeállítása és a bájtok UTF-8 szöveggé olvasása. Minden döntés – mit jelent a hiányzó
 * fájl, mi a `V010`, hogyan képződik le a séma – a [KnowledgeContentReader]-ben él.
 *
 * @property assets az alkalmazás assetkezelője.
 * @property rootDirectory a tudástár gyökere az assetsben; üres string esetén az assets
 *   gyökere.
 */
internal class AndroidKnowledgeAssetSource(
    private val assets: AssetManager,
    private val rootDirectory: String = KnowledgeAssetLayout.ROOT,
) : KnowledgeAssetSource {
    override fun readText(relativePath: String): String =
        assets.open(resolve(relativePath)).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }

    override fun listFileNames(relativeDirectory: String): Set<String> =
        try {
            // Az `AssetManager.list` alkönyvtárnevet is visszaadhat. A tudástár
            // elrendezésében az illusztrációk könyvtára csak fájlokat tartalmaz, egy
            // esetleges extra név pedig ártalmatlan: a V005 szabály a manifestből
            // *várt* fájlneveket keresi ebben a halmazban, nem fordítva.
            (assets.list(resolve(relativeDirectory)) ?: emptyArray()).toSet()
        } catch (error: IOException) {
            // Hiányzó könyvtár nem betöltési hiba (lásd az interfész szerződését).
            emptySet()
        }

    private fun resolve(relativePath: String): String =
        if (rootDirectory.isEmpty()) relativePath else "$rootDirectory/$relativePath"
}
