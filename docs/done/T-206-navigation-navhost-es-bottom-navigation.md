# T-206 · navigation: NavHost és bottom navigation

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05 |
| Modul | `navigation` |
| Méret | M (3–6 ó) |
| Függ | [T-205](T-205-core-ui-jelolok-es-allapotkomponensek.md) |
| Követelmény | NFR-03 |
| Teszt | TR-06 |

## Cél

A négy fő cél és a képernyők közti váltás.

## Teendők

- [ ] Útvonalak: `library/list`, `library/detail/{id}`, `learn/setup`, `learn/cards`, `quiz/setup`, `quiz/run/{sessionId}`, `quiz/result/{sessionId}`, `progress/overview`, `progress/technique/{id}`, `settings`.
- [ ] Bottom nav: Tudástár, Tanulás, Kvíz, Haladás – ikonnal és magyar felirattal.
- [ ] A bottom nav **elrejtve** a `quiz/run` és `quiz/result` képernyőn.
- [ ] Az `app` modul `KyokushinApp()` composable-je ezt használja a Fázis 1 helyőrző helyett.

## Kész, ha

- [ ] Mind a négy cél elérhető, az állapot megmarad fülváltáskor.
- [ ] A kvíz futás közben nem hagyható el véletlenül.

## Figyelj rá

- Ez a task cseréli le a Fázis 1-ben készült ideiglenes kezdőképernyőt.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [ ] `./gradlew assembleDebug` exit 0
- [ ] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [ ] Nem sért rétegszabályt (ADR-001, L1–L6)
- [ ] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [ ] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [ ] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [ ] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [ ] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-11 |
| Commit | `e39c078` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :navigation:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 15 új teszt zöld |
| `.\gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 408 teszt, 0 hiba, 0 kihagyott (393 → 408) |
| `.\gradlew.bat assembleDebug --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (19 925 550 byte) |

**Megjegyzés:**

- **Az útvonalak egyetlen hiteles forrása a `KyokushinScreen` enum** (`navigation` modul):
  útvonalminta + magyar cím + szakasz + „fülkezdő-e" + „látszik-e a bottom nav" egy
  helyen. A következő feladatok **ne** írjanak új útvonal-sztringet: a `composable { … }`
  blokk tartalmát cseréljék le a `PlaceholderScreen` helyén. Az argumentumnevek a
  `NavArgs` objektumban (`techniqueId`, `sessionId`), az argumentumos útvonalakat a
  `KyokushinRoutes` építi.
- **A kvíz elrejtése adat, nem elszórt `if`:** a `quiz/run` és `quiz/result` képernyőn a
  `showsBottomBar = false`, ezért a `KyokushinNavHost` nem állítja össze a `bottomBar`
  lambdát, és a felső sávban sincs vissza nyíl. A **T-410** ehhez tegye hozzá az explicit
  „Kvíz megszakítása" megerősítést – az útvonalakat és a rejtést nem kell átírni.
- **A képernyők nyíltan megjelölt helyőrzők:** „Ez a képernyő még fejlesztés alatt áll."
  Nem állítjuk, hogy a funkció kész. A helyőrző azonosítók (`placeholder-technique`,
  `placeholder-session`) nem tartalom: nem a tudástárból jönnek, és nem állítanak semmit
  egy technikáról.
- **Szövegkulcs-átrendezés:** a `nav_library`/`nav_learn`/`nav_quiz`/`nav_progress` kulcsok
  az `app` modulból a `navigation` modul `strings.xml`-jébe kerültek, mert a bottom nav is
  ott van; az `app` `strings.xml`-ben már csak az `app_name` áll. Egy szövegkulcs egy
  modulhoz tartozik, különben az erőforrás-összefűzés csendben felülírná az egyiket.
- **Eltérés a FUNCTIONAL_SPEC 1. ábrájától:** az ábra a tanulókártyákat
  `learn/cards?categories=..&hide=..` alakban mutatja, a T-206 teendőlistája viszont
  `learn/cards`-ot ír. A paraméter nélküli alak készült el; a tanulás beállítását a
  **T-302** `SavedStateHandle`-ben tartja (FUNCTIONAL_SPEC 4.1: „a beállítás megjegyzése a
  munkamenetek között"), ami nem igényel útvonal-paramétert. Ha a T-302/T-303 mégis
  útvonalon akarja átadni, a `KyokushinScreen.LEARN_CARDS` mintáját kell bővíteni, és a
  `navGraph_declaresExactlyTheSpecifiedRoutes_NFR03` teszt azonnal jelzi az eltérést.
- **Nem futott:** `connectedDebugAndroidTest` és a fülállapot megmaradásának tényleges
  bejárása (B-03, nincs emulátor) – a **T-606** tárgya. A `saveState`/`restoreState`/
  `launchSingleTop` beállítás kódban megvan, de eszköz nélkül nem mérhető.
