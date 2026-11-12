# T-302 · feature/learn: tanulás beállítása

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09, US-10 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-203](T-203-di-hilt-modulok.md), [T-206](T-206-navigation-navhost-es-bottom-navigation.md) |
| Követelmény | FR-06, FR-07 |
| Teszt | — |

## Cél

Kategória- és elrejtési mód választása a kártyák indítása előtt.

## Teendők

- [x] Többszörös kategóriaválasztás, kategóriánkénti darabszámmal.
- [x] „Japán nevet rejtse el” / „Magyar nevet rejtse el” választó.
- [x] „Kártyák indítása” gomb – letiltva, ha nincs megjeleníthető technika, indoklással.
- [x] Az utolsó választás megjegyzése.

## Kész, ha

- [x] A tanuláshoz **nem** kell `VERIFIED` státusz: a lektorálatlan tartalom is böngészhető, jelöléssel.

## Figyelj rá

- A kvíztől eltérően itt nincs `quizEligible` kapu – a tanulás célja az átnézés.

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
| Commit | `ca81ded` (kód), `2bdacbd` (docs) – közös a T-303 lezárásával (G-04 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 20 új teszt (`LearnSetupViewModelTest` 13, `LearnCardsViewModelTest` 7) |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **471 teszt, 0 hiba, 0 kihagyott** (451 → 471) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A `learn/setup` ViewModel (`LearnSetupViewModel`) és a Compose UI
(`LearnSetupScreen.kt`, Route + stateless screen + 4×2 `@Preview`) elkészült. A kijelölés
itt **nem** szűrő (szemben a Tudástár és a kvíz kategóriaszűrőjével): az üres kijelölés
letiltja a „Kártyák indítása" gombot, mert a FUNCTIONAL_SPEC 4.1/4. pontja ezt
kifejezetten megköveteli. A FUNCTIONAL_SPEC 4.1/2. pontjának „a kvízképes és összes
rekord száma" szövege ellentmond a rögtön utána következő mondatnak (nem kell
`quizEligible`/`verified`) – az implementáció a **teljes megjeleníthető** (nem visszavont)
technikaszámot mutatja, nem a kvízképeset; a doksi-eltérést lásd `docs/CONVENTIONS.md`
8. szakasz. Az „utolsó választás megjegyzése" `SavedStateHandle`-ben él (nincs
DataStore-függőség a version catalogban, és a CLAUDE.md tiltja az új, nem tervezett
függőség bevezetését) – ez forgatást és mentett állapotból történő folyamat-
újraindítást túlél, de nem egy teljes app-bezárást; lásd `docs/CONVENTIONS.md` 1.
szakasz. A `navigation` modul **nincs** megváltoztatva: a „Kártyák indítása" gomb a
kijelölt kategóriákat és az elrejtési módot lambdaparaméterként adja kifelé
(`onStartCards: (Set<CategoryId>, HideMode) -> Unit`), de a `learn/setup` képernyő
**nincs bekötve** a `KyokushinNavHost`-ba, és a `learn/cards` útvonalminta nem kapott
argumentumot – ez a tényleges navigációs bekötés (route-argumentumok, a `learn/cards`
útvonal bővítése) a kártya UI-t építő T-304 feladat tárgya, amikor a célképernyő
UI-ja is elkészül. Lásd a T-303 lezárását a kártyaállapot részleteiért.
