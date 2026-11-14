# T-705 · Tanulás – kategória-ikonok checkbox helyett, technikalista drill-down

| | |
|---|---|
| Modul | feature/learn, core/ui |
| Függ | — |
| Követelmény | FR-39 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | ViewModel teszt a kategória-kiválasztásra/drill-down állapotra, `@Preview` |

A jelenlegi `learn/setup` képernyő checkbox-okkal választatja ki a kategóriákat
(US-09/US-10 eredeti elfogadási köre). A felhasználó ehelyett ikonnal jelölt
kategóriakártyákat kér, amire koppintva a kategória technikái **egyenként**, böngészhető
listában jelennek meg – azaz a kategóriaválasztás és a konkrét technika-kiválasztás két
külön lépés lesz checkbox-tömb helyett.

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | lásd a következő commit üzenetét (`feat(learn): …[T-705]`) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL`, 656 teszt összesen (648 → +8: 5 ViewModel + 3 `CategoryIconTest`), 0 hiba |
| `./gradlew.bat ktlintCheck lint --console=plain` | 0 | `BUILD SUCCESSFUL`; `core/ui` és `feature/learn` lint-jelentése is üres (0 találat) |

**NEM FUTOTT:** emulátoros vizuális ellenőrzés (nem volt futó emulátor ebben a körben,
és a task saját `Teszt` mezője csak ViewModel tesztet és `@Preview`-t kér, nem
eszközpróbát) – a tényleges megjelenést a `@Preview`-k és a lint/ktlint tiszta
futása igazolja, nem kézi/eszközös vizuális átnézés.

**Megjegyzés:** lásd a fenti "Tervezési döntés" és "dokumentált eltérés" bekezdéseket
a kijelölés szintjéről (kategória, nem technika) és a kategória-ikon
`contentDescription`-jéről (dekoratív, nem duplikált).

## Teendők és készültségi feltételek

- [x] `core/ui`-ban kategóriánkénti ikonkészlet – `categoryIcon(categoryId)`
  (`core/ui/.../category/CategoryIcon.kt`): a 9 valós kategória mindegyike egy
  **semleges, geometrikus** Material-ikont kap (kéznyújtás, hirdetés, mérés, pajzs,
  kéz, irány-nyíl, fókuszpont, szám, útvonal – `docs/DESIGN_NOTES.md` 6–7. szakasza és
  CR-08 szerint, **nem** mozdulatot ábrázoló piktogram). Ismeretlen kategória a
  semleges `Icons.Filled.Category` helyőrzőt kapja. `CategoryIconTest` (3 teszt)
  bizonyítja, hogy minden valós kategória egyedi ikont kap, és egyik sem esik egybe a
  helyőrzővel.
- [x] A `learn/setup` ViewModel kategória-kiválasztási állapota: checkbox-tömb helyett
  kategóriakártya-rács + `drillDown: LearnCategoryDrillDownUiState?` állapot
  (`LearnSetupViewModel.onOpenCategory`/`onCloseDrillDown`), ami megnyitja az adott
  kategória technikalistáját.
- [x] Új állapot (nem új route, lásd a KDoc-ban leírt indoklást): a kiválasztott
  kategórián belüli technikák böngészhető listája. **Tervezési döntés (dokumentálva
  a `LearnCategoryDrillDownUiState` KDoc-jában):** a kijelölés **kategória-szinten**
  marad (nem vezettem be technika-szintű kijelölést) – a drill-down egy "Kiválasztás/
  Kiválasztva a tanuláshoz" gombbal a TELJES kategóriát kapcsolja ki/be, a
  technikalista böngészésre szolgál. Ennek két oka: (1) a task fejléce a
  `feature/learn, core/ui` modulhatárt adja – technika-szintű kijelölés a
  `LearnCardsViewModel` és a `navigation` route szerződését is megváltoztatta volna;
  (2) a "kattints a kategóriára, lásd egyesével a tananyagokat" kérést a böngészhető
  lista önmagában teljesíti. Az US-09 "több kategória egyszerre" folytonossága:
  változatlanul `selectedCategories`-ben él, a felhasználó több kategóriát is
  bejárhat egymás után a drill-down gombjával.
- [x] Forgatás: a böngészött kategória és a kijelölés is `SavedStateHandle`-ben él
  (`KEY_DRILLDOWN_CATEGORY`, a meglévő `KEY_SELECTED_CATEGORIES` mintáját követve) –
  `learnSetup_drillDown_survivesSavedStateHandleRestore_FR39` bizonyítja.
- [x] `contentDescription` – **dokumentált eltérés** a szó szerinti Teendőtől: az ikon
  `contentDescription = null` (dekoratív), mert a kártya `Surface(onClick = …)`
  összevont TalkBack-egység, és a látható `category.nameHu` szöveg már ugyanott
  kimondásra kerül – egy azonos szövegű ikon-leírás duplán mondaná be a nevet
  (ugyanaz a hiba-kategória, amit a bottom nav ikonjai is dokumentáltan elkerülnek).
  A kijelölt állapotot `stateDescription` jelzi, nem egy duplikált névvel ellátott
  ikon. Érintési célpont ≥ 48 dp mind a kártyán, mind a drill-down vissza gombján és
  a kiválasztás-gombon.
- [x] `@Preview` világos és sötét témára: kategóriarács, drill-down (kijelölve),
  drill-down (üres kategória), letiltott indítás, betöltés, hiba.
- [x] ViewModel tesztek: 5 új teszt a kategória-megnyitásra, a kijelölt-állapot
  tükrözésére, a bezárásra, a forgatás utáni megőrzésre és az alapértelmezett
  (nincs megnyitva) állapotra.
- [x] `./gradlew testDebugUnitTest assembleDebug` zöld (656 teszt, 0 hiba).
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
