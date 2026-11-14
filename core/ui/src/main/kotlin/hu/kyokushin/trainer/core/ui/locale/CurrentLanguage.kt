package hu.kyokushin.trainer.core.ui.locale

import java.util.Locale

/**
 * A jelenleg aktív nyelv ISO 639-1 kódja (FR-43, ADR-013) a
 * [hu.kyokushin.trainer.core.model.localizedName]/
 * [hu.kyokushin.trainer.core.model.localizedDescription] hívásaihoz.
 *
 * A rendszer- vagy alkalmazásnyelv-beállítást követi: Android a JVM alapértelmezett
 * [Locale]-ját ennek megfelelően tartja szinkronban, ezért nincs szükség saját
 * `Resources`/`LocaleList` kezelésre vagy állapot-perzisztálásra – a felhasználó
 * explicit a rendszerbeállítás-követést kérte, nincs saját in-app nyelvválasztó.
 *
 * Szándékosan egyszerű, ambiens olvasás, nem Hilt-injektált függőség: ellentétben az
 * órával/RNG-vel (NFR-07), ez nem tárolt adat helyességét vagy reprodukálhatóságát
 * befolyásolja, csak megjelenítési szöveget választ ki – ugyanolyan ambiens állapot,
 * mint amit az Android saját erőforrás-feloldása is olvas minden `getString` hívásnál.
 */
fun currentLanguageTag(): String = Locale.getDefault().language
