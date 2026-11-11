package hu.kyokushin.trainer.core.data.content

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Memóriában tartott tudástár a beolvasó tesztjeihez.
 *
 * Azért van rá szükség, mert a [KnowledgeContentReader] szerződése a *tartalomról* szól,
 * nem az Android assetkezeléséről: így a sémaleképezés és a `V010` viselkedés tiszta JVM
 * unit tesztben ellenőrizhető, emulátor és csomagolt APK nélkül.
 *
 * @property files útvonal → fájltartalom. Ami nincs benne, arra a valódi assetkezelőhöz
 *   hasonlóan [IOException] jön.
 * @property illustrationFiles a „csomagolt" képfájlok csupasz nevei.
 */
internal class FakeKnowledgeAssetSource(
    private val files: Map<String, String>,
    private val illustrationFiles: Set<String> = emptySet(),
) : KnowledgeAssetSource {

    override fun readText(relativePath: String): String =
        files[relativePath] ?: throw FileNotFoundException(relativePath)

    override fun listFileNames(relativeDirectory: String): Set<String> =
        if (relativeDirectory == KnowledgeAssetLayout.ILLUSTRATIONS_DIRECTORY) illustrationFiles else emptySet()
}

/**
 * A **valódi** átadott tudástár (`knowledgebase_transfer/`) mint tudástárforrás.
 *
 * Az útvonal a `kyo.repoRoot` rendszertulajdonságból jön (a `core/data/build.gradle.kts`
 * állítja be), nem a teszt munkakönyvtárából: így a teszt akkor is megtalálja a tartalmat,
 * ha a Gradle más könyvtárból indítja a JVM-et.
 *
 * A csomagolt assets (`app/src/main/assets/knowledge/`) a T-601 feladat tárgya; addig a
 * beolvasót ezen az azonos szerkezetű, valódi tartalmon lehet és kell ellenőrizni.
 */
internal class RealContentAssetSource : KnowledgeAssetSource {

    override fun readText(relativePath: String): String = contentFile(relativePath).readText(Charsets.UTF_8)

    override fun listFileNames(relativeDirectory: String): Set<String> =
        (contentFile(relativeDirectory).list() ?: emptyArray()).toSet()

    private fun contentFile(name: String): File {
        val repoRoot = requireNotNull(System.getProperty("kyo.repoRoot")) {
            "A kyo.repoRoot rendszertulajdonság nincs beállítva – lásd core/data/build.gradle.kts."
        }
        val file = File(File(repoRoot, "knowledgebase_transfer"), name)
        // Hiányzó tartalom nem „üres tudástár", hanem elbukó teszt: a garancia éppen az,
        // hogy a leképezés a *valódi* 161 rekordon fut le.
        assertThat(file.exists()).isTrue()
        return file
    }
}
