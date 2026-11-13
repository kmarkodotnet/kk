package hu.kyokushin.trainer.di

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.content.ImportResult
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * A csomagolt `app/src/main/assets/knowledge/` tartalom tényleges betöltése (T-601, FR-29,
 * AC-08).
 *
 * ## Miért ez a bizonyíték, és nem csak a `HiltGraphTest`?
 *
 * A [HiltGraphTest] csak azt igazolja, hogy a [ContentImporter] *előáll* a Hilt gráfban –
 * nem hívja meg. A T-601 „Kész, ha" pontja viszont azt kéri, hogy „`assembleDebug` után az
 * app elindul és betölti a 161 rekordot" – ezt csak az [ContentImporter.importIfNeeded]
 * tényleges lefuttatásával lehet bizonyítani, a valódi, csomagolt `AssetManager`-en
 * keresztül (a Hilt `ContentModule` ezt köti: `AndroidKnowledgeAssetSource(context.assets)`).
 *
 * Emulátor/eszköz nélkül (B-03, `docs/ENVIRONMENT.md`) ez a Robolectric alatt futó teszt az
 * egyetlen elérhető, hiteles bizonyíték – ugyanaz a minta, mint a [HiltGraphTest] (teljes
 * Hilt-gráf, `isIncludeAndroidResources = true` az assetekhez). A valódi eszközön történő
 * indítást a TR-06 instrumentált tesztek fedik (T-606).
 *
 * A várt számok (`knowledgebase_transfer/README.md`): 161 rekord, `dataset_version` 0.1.0,
 * 0 elvetett rekord – ugyanaz a garancia, amit a `core/data`
 * `RealKnowledgeContentReaderTest` és `RealKnowledgeBaseValidationTest` (core/quiz) a
 * `knowledgebase_transfer/` forráson közvetlenül, fájlrendszeri eléréssel már igazol. Ez a
 * teszt azt a *hiányzó* láncszemet zárja le, hogy ugyanez a tartalom a **csomagolt
 * assetből**, a valódi Android `AssetManager`-en át is elérhető.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HiltTestApplication::class)
class KnowledgeAssetImportTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var contentImporter: ContentImporter

    @Before
    fun injectGraph() {
        hiltRule.inject()
    }

    @Test
    fun importIfNeeded_packagedKnowledgeAssets_loadsAllEntries_FR29() = runTest {
        val result = contentImporter.importIfNeeded()

        assertThat(result).isInstanceOf(ImportResult.Imported::class.java)
        val imported = result as ImportResult.Imported
        assertThat(imported.contentVersion).isEqualTo(EXPECTED_DATASET_VERSION)
        assertThat(imported.acceptedCount).isEqualTo(EXPECTED_ENTRY_COUNT)
        assertThat(imported.rejectedCount).isEqualTo(0)
        assertThat(imported.retiredCount).isEqualTo(0)
    }

    /**
     * Ugyanazon a csomagolt adathalmazon kétszer futtatva a második hívás
     * [ImportResult.UpToDate] (FR-30) – ez a verziókapu bizonyítéka a valódi assetekkel,
     * nem csak a szintetikus tesztfixtúrával (`core/data` `RoomContentImporterTest`).
     */
    @Test
    fun importIfNeeded_calledTwice_secondCallIsUpToDate_FR30() = runTest {
        contentImporter.importIfNeeded()
        val second = contentImporter.importIfNeeded()

        assertThat(second).isEqualTo(ImportResult.UpToDate(EXPECTED_DATASET_VERSION))
    }

    private companion object {
        /** `knowledgebase_transfer/README.md`: 161 rekord, `dataset_version` 0.1.0. */
        const val EXPECTED_ENTRY_COUNT = 161
        const val EXPECTED_DATASET_VERSION = "0.1.0"
    }
}
