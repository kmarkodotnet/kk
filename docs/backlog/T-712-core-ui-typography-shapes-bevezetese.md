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

## Teendők és készültségi feltételek

- [ ] `core/ui/.../theme/Theme.kt`: `LightScheme.primary` → `#17317A`
  (`DESIGN_NOTES.md` 3. szakasz); a függő tokenek (`primaryContainer` stb.) a
  tervben javasolt irányban, de a végső hexeket a `ThemeContrastTest` dönti el –
  **minden** pár ≥ 4,5:1 kell, hogy maradjon.
- [ ] Új `Shapes()` objektum (`small = 4.dp`, `medium = 12.dp`, `large = 16.dp`,
  `DESIGN_NOTES.md` 5. szakasz), átadva a `MaterialTheme(shapes = …)`-nek.
- [ ] A nyolc, ma kézzel kiírt `RoundedCornerShape(12.dp)` (`QuizResultScreen`,
  `QuizRunScreen` ×3, `ProgressTechniqueScreen`, `ProgressDiagnosticsScreen`,
  `ProgressOverviewScreen` ×2, `ProgressSettingsScreen` ×2) lecserélése
  `MaterialTheme.shapes.medium`-ra.
- [ ] A `LearnCardsScreen.CARD_CORNER_RADIUS` (ma `16.dp`, helyi konstans) lecserélése
  `MaterialTheme.shapes.large`-ra, a helyi konstans eltávolításával.
- [ ] Új `Typography()` objektum: a romaji/japán cím szerepek (`titleLarge`,
  `headlineSmall`) `fontWeight = FontWeight.Bold` felülírással a Material 3
  alapértelmezett `Medium` helyett; a magyar jelentés szerepek (`bodyLarge`,
  `bodyMedium`) változatlan súllyal, de a hívó helyeken explicit
  `color = MaterialTheme.colorScheme.onSurfaceVariant`, ha eddig `onSurface` volt.
  **Nem** vezet be új betűtípus-fájlt (`DESIGN_NOTES.md` 4. szakasz "elvetett ötlet").
- [ ] `ThemeContrastTest` (`core/ui`) minden pár újra ellenőrizve az új színekkel –
  ha egy pár 4,5:1 alá esne, a hex-et addig kell finomítani, amíg nem.
- [ ] `@Preview` frissítése az érintett komponenseken (világos + sötét), hogy a
  vizuális változás látható legyen a Preview-ban is.
- [ ] `./gradlew testDebugUnitTest assembleDebug ktlintCheck lint` zöld.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
