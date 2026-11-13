package hu.kyokushin.trainer

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TR-08 (NFR-01): az offline ígéret gépi ellenőrzése – az **összevont** (merge-elt)
 * manifestben nem szabad megjelennie az `android.permission.INTERNET`-nek.
 *
 * ## Miért nem "nulla permission" az elvárás?
 *
 * A 2026-11-11-i build igazolta, hogy az AndroidX könyvtárak (jelen esetben a Hilt
 * transitive manifest merge-je) saját, aláírásszintű permissiont adnak hozzá az
 * összevont manifesthez (`...DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) – lásd
 * `docs/TEST_REPORT.md` és `docs/CONVENTIONS.md`. Egy „nincs semmilyen permission"
 * assertion ezért **hamisan bukna**, miközben az offline ígéret (NFR-01) ettől nem
 * sérül. A teszt ezért kifejezetten az INTERNET hiányát ellenőrzi, nem a permissionök
 * számát.
 *
 * ## Miért nem a `HiltGraphTest` mintáját (HiltTestApplication) követi?
 *
 * A valódi `KyokushinApplication` `@HiltAndroidApp`, aminek Robolectric alatti
 * felépítése a teljes Hilt gráfot igényelné. Ennek a tesztnek viszont csak az
 * `AndroidManifest.xml`-ek Gradle által már összevont eredményére van szüksége – a
 * `PackageManager` ezt a Hilt gráf felépítése nélkül is visszaadja egy sima
 * `Application`-nel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManifestPermissionTest {
    @Test
    fun mergedManifest_doesNotRequestInternetPermission_TR08() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )

        // Az AndroidX által automatikusan hozzáadott, aláírásszintű permission (lásd
        // a fenti KDoc) szándékosan NEM kizárt itt – csak az INTERNET konkrét hiánya
        // a vizsgálat tárgya (TR-08).
        val requestedPermissions = packageInfo.requestedPermissions?.toList().orEmpty()

        assertThat(requestedPermissions).doesNotContain(Manifest.permission.INTERNET)
    }
}
