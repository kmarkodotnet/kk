# T-307 · feature/learn: záróképernyő

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09 |
| Modul | `feature/learn` |
| Méret | S (1–3 ó) |
| Függ | [T-304](T-304-feature-learn-kartya-ui.md) |
| Követelmény | FR-06 |
| Teszt | — |

## Cél

A kártyasor végén összegzés és továbblépési lehetőségek.

## Teendők

- [x] „Végigértél `{n}` kártyán.”
- [x] Gombok: Újrakezdés / Vissza a beállításokhoz / Kvíz indítása ezekkel a kategóriákkal.

## Kész, ha

- [x] A „Kvíz indítása” átadja a kiválasztott kategóriákat a kvízbeállítónak.

## Figyelj rá

- A tanulás → kvíz átvezetés a természetes tanulási út: előbb átnézem, aztán ellenőrzöm magam.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-12 |
| Commit | közös a T-304/T-305 lezárásával (G-05 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `learnCards_nextAfterLastCard_showsFinishedScreen_FR06`, `learnCards_restartAfterFinished_returnsToFirstCardWithoutReveal_FR06` |
| `./gradlew.bat :navigation:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `learnCards_routeArgumentNames_matchTheViewModelKeys_NFR03`, `routeBuilders_learnCards_fillsCategoryIdsAndHideMode_NFR03` |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **484 teszt, 0 hiba, 0 kihagyott** (471 → 484), minden task frissen futtatva |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- **A `LearnCardsViewModel` (T-303) bővült egy `LearnCardsUiState.Finished` ággal.** A
  `buildState` immár a `position >= techniques.size` esetet nem szorítja vissza az
  utolsó kártyára (a korábbi, T-303-as viselkedés), hanem a záróképernyőt adja – ez egy
  **szándékos, dokumentált** viselkedésváltás a FUNCTIONAL_SPEC 4.2 „Az utolsó kártya
  után záróképernyő" pontja miatt (`docs/CONVENTIONS.md` 5. szakasz új sora). A meglévő
  `learnCards_nextAndPrevious_respectBounds_FR09` teszt emiatt két tesztre bomlott:
  `learnCards_previousAtStart_isNoOp_FR09` (a határvédelem első fele marad) és
  `learnCards_nextAfterLastCard_showsFinishedScreen_FR06` (az új elvárás). Új
  `onRestart()` metódus: pozíció 0-ra, felfedettség törölve, **a seed nem változik**
  (ugyanazon a sorrenden megy újra végig a felhasználó).
- **A „Kvíz indítása ezekkel a kategóriákkal" csak a modul saját határáig van bekötve.**
  A `LearnCardsRoute` `onStartQuiz: (Set<CategoryId>) -> Unit` lambdán kifelé **átadja**
  a kiválasztott kategóriákat (ez a „Kész ha" pont, a feature modul szerződésének
  szintjén teljesítve, L5: a modul nem navigál magától). A `quiz/setup` (T-403/T-404)
  még nem létezik, ezért a `navigation` modul ma csak átnavigál a helyőrzőre, a
  kategóriahalmazt nem továbbítja tovább – ugyanaz a döntés, mint a T-302/T-303 párnál a
  `learn/cards` argumentumaival. Dokumentált nyitott ügy: `docs/CONVENTIONS.md`
  8. szakasz. Amikor a T-403/T-404 elkészül, a `KyokushinNavHost` LEARN_CARDS ágának
  `onStartQuiz` lambdáját kell kibővíteni.
- **A `navigation` modul bekötötte a `learn/setup`-ot és a `learn/cards`-ot** (eddig
  helyőrzők voltak). A `learn/cards` route két lekérdezőparamétert kapott
  (`?categoryIds={categoryIds}&hideMode={hideMode}`); a paraméternevek a
  `LearnCardsViewModel` már létező, nem-`private` kulcs-konstansaiból jönnek
  (`KEY_CATEGORY_IDS`/`KEY_HIDE_MODE`), nem egy új, duplikált `NavArgs`-sztringből – a
  Navigation Compose a route argumentumokat automatikusan a cél `SavedStateHandle`-jébe
  teszi ugyanazzal a névvel. Lásd `docs/CONVENTIONS.md` 5. szakasz új sora.
- **A `LearnCardsScreen.kt` döntési logikája `@Composable`-mentes `object`-ekbe van
  kiemelve** (`LearnCardImagePolicy`, `LearnCardIllustrationLabelPolicy`,
  `LearnCardStateDescription`, `feature/learn/cards/LearnCardPolicies.kt`), ugyanazzal a
  mintával, mint a `core/ui` `IllustrationContentDescription`/`VerificationBadgeText`-je
  vagy a `feature/library` `LibraryListItemAccessibility`-je: sima JUnit/Robolectric
  teszttel bizonyíthatók, Compose-renderelés és emulátor nélkül (8 teszt,
  `LearnCardPoliciesTest`). Ez fedi a T-304 „Kész ha" `stateDescription` pontját és a
  T-305 kép-elrejtési szabályát is.
