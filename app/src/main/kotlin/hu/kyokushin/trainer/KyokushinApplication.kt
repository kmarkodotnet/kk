package hu.kyokushin.trainer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt gyökér. A tudástár importja NEM itt indul: az importot a Tudástár képernyő
 * ViewModelje kéri a [hu.kyokushin.trainer.core.data] rétegtől, hogy a UI valódi
 * Loading állapotot tudjon mutatni (FUNCTIONAL_SPEC 7.1).
 */
@HiltAndroidApp
class KyokushinApplication : Application()
