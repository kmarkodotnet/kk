# T-409 · feature/quiz: válasz-visszajelzés

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-14 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-407](T-407-feature-quiz-a-es-b-kerdes-ui.md) |
| Követelmény | FR-17, NFR-04 |
| Teszt | TR-06 |

## Cél

Azonnali, akadálymentes visszajelzés és a helyes megoldás bemutatása.

## Teendők

- [x] A kiválasztott opció megjelölve: pipa + „Helyes” vagy × + „Helytelen” – **ikon + szöveg + szín**
      – `QuizOptionRow` + `QuizOptionFeedbackResolver.SELECTED_CORRECT`/`SELECTED_INCORRECT`.
- [x] A helyes opció mindig kiemelve – `CORRECT_UNSELECTED` ág, **ugyanazzal** a
      pipa+„Helyes”+szín jelzéssel, mint a kiválasztott helyes opció (nem csak színbeli
      kiemelés – lásd „Kész, ha”).
- [x] Magyarázó kártya: romaji + magyar név + ábra, link a részletezőre – `QuizExplanationCard`,
      `onClick` → `onOpenTechnique(techniqueId)` kifelé adott lambda (L5, a navigáció a
      hívó dolga).
- [x] „Következő” gomb (az utolsónál „Eredmény”) – a válasz megadása után megjelenő
      `Button`, felirata `content.isLastQuestion`-től függ. A gomb funkciója (a tényleges
      befejezés/eredményképernyőre lépés) a G-09 csoport (T-411) tárgya – itt a `onNext()`
      az utolsó kérdésen dokumentáltan no-op marad (lásd `QuizRunViewModel` KDoc-ja).

## Kész, ha

- [x] A helyesség nem **csak** színnel jelenik meg (színtévesztés) – minden kiemelt
      opció (`SELECTED_CORRECT`/`SELECTED_INCORRECT`/`CORRECT_UNSELECTED`) pipa/kereszt
      ikont **és** „Helyes”/„Helytelen” szöveget is kap a szín mellett.
- [x] `stateDescription` = „helyes” / „helytelen” – `QuizOptionRow` a `Modifier.selectable`
      (`selected` szemantika) után `Modifier.semantics { stateDescription = … }`-t
      láncol a `SELECTED_CORRECT`/`CORRECT_UNSELECTED` (→ „helyes”) és a
      `SELECTED_INCORRECT` (→ „helytelen”) ágon – FUNCTIONAL_SPEC 9. szándékosan
      kisbetűs szövegei, elkülönítve a látható, nagybetűs „Helyes”/„Helytelen” felirattól.

## Figyelj rá

- A helyes megoldás hármas bemutatása (név–jelentés–ábra) a tanulási cél magja (FR-17).

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
| Commit | *(a következő commit rögzíti)* |

**Futtatott parancsok** (Bash, `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.21.9-hotspot"`):

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **535 teszt, 0 hiba** (528 → 535) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Mi készült:** lásd a T-407 lezárását a közös „Mi készült” leírásért (egy fájlban,
egy menetben készültek, G-08 csoport). Ennek a feladatnak a tárgya a
`QuizOptionRow` válasz-visszajelzése (ikon + szöveg + szín + `stateDescription`) és a
`QuizExplanationCard` (a helyes technika romaji + magyar + ábra hármasa, kattintható,
`onOpenTechnique(techniqueId)` lambdán kifelé adva – US-14/3. kritérium). Az
„ikon + szöveg” szabály egységesen minden kiemelt opcióra vonatkozik: a `CORRECT_UNSELECTED`
ág (a helyes, de nem választott opció) **ugyanazt** a pipa+„Helyes” jelzést kapja, mint a
tényleg kiválasztott helyes opció – ez szigorúbb, mint amit a FUNCTIONAL_SPEC 5.2/4.
szó szerint kér (ott csak „mindig kiemelve” áll, ikon/szöveg nélkül), de konzisztens az
NFR-04 „nem csak szín” elvével, és nem mond ellent a specifikációnak.

**Megjegyzés:** a (C)/(D) képes kérdéstípusok visszajelzése (T-408 UI-ja felett) és az
eredményképernyőre lépés (`finishSession`, G-09/T-411) nincs ebben a feladatban – lásd a
T-407 Megjegyzését.
