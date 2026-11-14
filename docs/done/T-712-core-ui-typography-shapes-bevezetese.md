# T-712 · `core/ui`: egyedi `Typography` és `Shapes` bevezetése (DESIGN_NOTES.md alapján)

| | |
|---|---|
| Modul | core/ui, feature/learn, feature/quiz, feature/progress |
| Függ | T-702 |
| Követelmény | FR-36 (`docs/REQUIREMENTS.md` 3.6), CR-08 |
| Teszt | `ThemeContrastTest` (bővítve/újrafuttatva), `@Preview` érintett komponenseken |

A `docs/DESIGN_NOTES.md` (T-702) három konkrét, kódba még át nem ültetett döntést ad:
egy a T-701 splash-logóból (`#17317A`) levezetett `primary` szín, egy megnevezett
`Shapes` objektum (a ma 8 fájlban külön-külön leírt `RoundedCornerShape(12.dp)`
helyett), és egy explicit tipográfiai szerepkiosztás (romaji **Bold** cím, magyar
jelentés alárendelt törzsszöveg). Ez a task ezeket **ténylegesen** beépíti a
`core/ui` témájába és a hívó helyekbe.

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | lásd a következő commit üzenetét (`feat(core-ui): …[T-712]`) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL`, 659 teszt, 0 hiba |
| `./gradlew.bat ktlintCheck lint --console=plain` | 1 → 0 | Első futás `feature/learn:ktlintMainSourceSetCheck` hibával bukott (`Needless blank line(s)` a `CARD_CORNER_RADIUS` eltávolítása után maradt dupla üres sor miatt) – a sor törlése után `BUILD SUCCESSFUL`, minden érintett modul lint-jelentése 0 találat |

**Megjegyzés:** a `docs/DESIGN_NOTES.md`/task "nyolc fájl" becslése pontatlan volt – a
tényleges duplikált `RoundedCornerShape(12.dp)` hívási helyek száma **10** (a task saját
felsorolása is 10-et sorol fel, csak az összegzés mondott nyolcat). A `@Preview`
Teendő nem igényelt tényleges kódszerkesztést, mert a szín/forma/tipográfia a megosztott
`KyokushinTrainerTheme`-en át automatikusan érvényesül minden meglévő előnézetben.

## Teendők és készültségi feltételek

- [x] `core/ui/.../theme/Theme.kt`: `LightScheme.primary` → `#17317A`
  (`DESIGN_NOTES.md` 3. szakasz); `primaryContainer` → `#D9E2F7` (a tervben
  javasolt irány szerint). A `ThemeContrastTest` mindkét új párra lefutott:
  `onPrimary`/`primary` ≈ **11,95:1**, `onPrimaryContainer`/`primaryContainer`
  ≈ **12,91:1** – mindkettő jóval a 4,5:1 küszöb fölött, nem kellett finomítani.
- [x] Új `Shapes()` objektum (`small = 4.dp`, `medium = 12.dp`, `large = 16.dp`,
  `DESIGN_NOTES.md` 5. szakasz), átadva a `MaterialTheme(shapes = …)`-nek
  (`KyokushinShapes`, `core/ui` privát).
- [x] A kézzel kiírt `RoundedCornerShape(12.dp)` (`QuizResultScreen` ×1,
  `QuizRunScreen` ×3, `ProgressTechniqueScreen` ×1, `ProgressDiagnosticsScreen` ×1,
  `ProgressOverviewScreen` ×2, `ProgressSettingsScreen` ×2 – **ténylegesen 10
  hívási hely**, nem 8, a Teendő becslése pontatlan volt) lecserélve
  `MaterialTheme.shapes.medium`-ra, a felesleges `RoundedCornerShape` importokkal
  együtt.
- [x] A `LearnCardsScreen.CARD_CORNER_RADIUS` (`16.dp`, helyi konstans) lecserélve
  `MaterialTheme.shapes.large`-ra, a helyi konstans és a felesleges import törölve.
- [x] Új `Typography()` objektum (`KyokushinTypography`, `core/ui` privát): a
  `titleLarge`/`headlineSmall` szerepek `fontWeight = FontWeight.Bold`
  felülírással a Material 3 alapértelmezett `Medium` helyett; minden más szerep
  (`bodyLarge`/`bodyMedium` is) változatlan. **Nem** vezetett be új
  betűtípus-fájlt – a Material 3 saját `Typography()` alapértékeiből indul, csak
  két szerep súlya módosul.
- [x] `ThemeContrastTest` (`core/ui`) mindkét módosult pár újra ellenőrizve – lásd
  az első pont.
- [x] `@Preview` – nem igényelt kódváltozást: a `Shapes`/`Typography`/színváltás a
  `KyokushinTrainerTheme`-en keresztül **automatikusan** érvényesül minden
  meglévő `@Preview`-ban (azok mind ezt a theme wrappert hívják) – nem kellett
  egyenként szerkeszteni a (már létező, világos/sötét) előnézeteket.
- [x] `./gradlew testDebugUnitTest assembleDebug ktlintCheck lint` zöld (659
  teszt, 0 hiba; lint/ktlint minden érintett modulban 0 találat).
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
