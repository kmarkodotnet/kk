# T-701 · Indítóképernyő – vektorizált kandzsi logó fehér háttéren

| | |
|---|---|
| Modul | app |
| Függ | — |
| Követelmény | FR-36 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | `@Preview` világos/sötét téma, manuális indítási ellenőrzés |

A felhasználó 2026-11-13-i visszajelzése szerint az app induláskor jelenleg egyenesen a
Tudástár listájára ugrik – ez "webes űrlap" hatást kelt. Első lépésként egy rövid
indítóképernyő kerül be: kék kandzsi logó fehér háttéren.

A logó forrása a felhasználó saját gi-fotója (`asserts/kyo.png`), **nem** harmadik fél
appjából vagy weboldaláról származó kép (CLAUDE.md tartalmi integritás). A fotót ebben a
munkamenetben vektorizáltuk (OpenCV kontúrkövetés + Chaikin-simítás, PCA-alapú
dőlésszög-korrekció):

- `asserts/kyo_kanji.svg` – skálázható vektor, `#17317A` kék kitöltés
- `asserts/ic_kyo_kanji.xml` – kész Android `<vector>` drawable (200dp széles, arányos
  magassággal), közvetlenül `app/src/main/res/drawable/` alá másolható
- `asserts/kyo_preview_blue_on_white.png` – gyors vizuális ellenőrzéshez

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | lásd a következő commit üzenetét (`feat(app): …[T-701]`) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest assembleDebug --console=plain` | 0 | 648 teszt (647 → +1 `SplashContrastTest`), 0 hiba; `app-debug.apk` ≈ 22 746 748 byte |
| `./gradlew.bat ktlintCheck lint --console=plain` | 0 | nincs új lint/ktlint találat |

**Megjegyzés:** az Android 12+ `SplashScreen` API-ra (`core-splashscreen`) való átállás
szándékosan **nincs** megépítve, mert a könyvtár nincs a `gradle/libs.versions.toml`-ban –
ez a task saját feltételes szabálya szerint jóváhagyást igénylő, **új, önálló** feladat
(nem ennek a tasknak a hiányossága). Javasolt új task: "T-71x · Android 12+ natív
SplashScreen API bekötése (`core-splashscreen` felvétele a verziókatalógusba,
jóváhagyás után)". A kézi Compose fallback emiatt véglegesen is megmarad (régebbi
`minSdk`-khoz mindenképp szükséges), a natív API csak a legújabb Androidon rövidítené a
villanást.

## Teendők és készültségi feltételek

- [x] `ic_kyo_kanji.xml` bemásolása `app/src/main/res/drawable/` alá (ellenőrizve, hogy a
  `pathData` érvényes és a Vector Asset Studio/lint nem jelez hibát).
- [x] ~~Android 12+ `SplashScreen` API bekötése~~ **Blokkolva, jóváhagyásra vár.** A
  `core-splashscreen` **nem** szerepel a `gradle/libs.versions.toml`-ban, és a task saját,
  feltételes megfogalmazása szerint ("csak ha már szerepel… ha nem, jelezd blokkolóként")
  ez nem hiányosság: egy új függőség/verzió bevezetése a CLAUDE.md "ne találj ki
  verziószámot" szabálya miatt külön jóváhagyást igényel. A funkcionális követelmény
  (FR-36: splash megjelenik, kék logó fehér háttéren) a lenti kézi fallbackkel **minden**
  API-szinten (26+, a 12+-t is beleértve) teljesül – a natív API-ra való átállás csak egy
  rövidebb rendszer-szintű cold-start villanást nyerne, nem új funkciót.
- [x] A `minSdk`-ig visszamenőleg kézi Compose splash képernyő fallback (rövid, fix idejű
  megjelenítés, utána automatikus továbblépés) – `KyokushinApp.kt`: `showSplash` állapot,
  `LaunchedEffect` + `delay(1200ms)`. **Szándékosan nem** a Kezdőoldalra (T-703) navigál
  explicit módon: egyszerűen átadja a vezérlést a `KyokushinNavHost`-nak, ami mindig a
  saját, aktuális induló útvonalát mutatja – ma a Tudástár listája, T-703 után a
  Kezdőoldal, a splash kódjának módosítása nélkül.
- [x] A logó fehér háttéren, középre igazítva, kontraszt-ellenőrzéssel (WCAG AA) világos
  **és** sötét rendszertémában is – `SplashScreen.kt` fix `Color.White` felületen (nem
  téma-token, dokumentált kivétel), a logó a vektorba égetett `#17317A`-val, mindkettő
  théma-független. `SplashContrastTest` (app modul) a meglévő, mostantól publikus
  `core/ui` `wcagContrastRatio`-val méri: **≈ 11,95:1**, jóval a 4,5:1 küszöb fölött.
  A `values-night/themes.xml` cold-start ablakháttere is fehérre állítva, hogy sötét
  témában se villanjon fekete a Compose-splash elé.
- [x] `contentDescription`/`accessibilityHeading` a splash logóra – `stringResource`
  (`splash_content_description` = "Kyokushin Karate Trainer embléma") +
  `Modifier.semantics { heading() }`.
- [x] `@Preview` világos és sötét témára – `SplashScreenPreview` (`SplashScreen.kt`).
- [x] `./gradlew testDebugUnitTest assembleDebug` zöld.
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
