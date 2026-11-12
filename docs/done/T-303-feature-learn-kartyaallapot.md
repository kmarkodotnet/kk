# T-303 · feature/learn: kártyaállapot

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-302](T-302-feature-learn-tanulas-beallitasa.md) |
| Követelmény | FR-09, NFR-07 |
| Teszt | — |

## Cél

A kártyasorrend és a pozíció megőrzése.

## Teendők

- [x] Seedelt keverés a munkamenet elején, egyszer sorsolva.
- [x] A sorrend, az aktuális index és a **kártyánkénti felfedettség** `SavedStateHandle`-ben.
- [x] Előző/következő léptetés határokkal.

## Kész, ha

- [x] Forgatás után ugyanaz a kártya, ugyanazzal a sorrenddel és felfedettséggel.

## Figyelj rá

- A felfedettség kártyánként tárolandó: visszalépéskor a már felfedett kártya felfedve marad.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára – **nem alkalmazható**: ez a feladat kizárólag a ViewModelt adja (lásd Megjegyzés), UI nélkül, ugyanúgy, mint a T-207
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
| Commit | lásd a T-302 lezárásával egy közös commit (G-04 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 20 új teszt (`LearnSetupViewModelTest` 13, `LearnCardsViewModelTest` 7) |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **471 teszt, 0 hiba, 0 kihagyott** (451 → 471) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** `LearnCardsViewModel` (`feature/learn/cards`) készült el, Compose UI
nélkül – ugyanaz a kettéválasztás, mint a `feature/library` lista ViewModelje (T-207) és
a hozzá tartozó Compose UI (T-208) között; a tényleges kártya UI-t a T-304 építi fel
ennek a ViewModelnek a felszínére. A bemenetet (kijelölt kategóriák, elrejtési mód) a
`SavedStateHandle` `categoryIds`/`hideMode` kulcsain várja, ugyanazzal a mintával, mint a
`LibraryDetailViewModel.techniqueId` – a kulcsnevek a jövőbeli navigációs argumentummal
csak **névben** egyeznek majd (a `navigation` modul importálása nélkül, L5); a tényleges
route-bekötés a T-304 tárgya, mert a `learn/cards` útvonalminta még nem kapott
argumentumot (lásd T-302 Megjegyzés). A seedet a konstruktor sorsolja egyszer az
injektált `Random`-ból, és azonnal a `SavedStateHandle`-be menti (`ADR-007` mintája, mint
a `DefaultQuizGenerator`-nál) – innentől `techniques.sortedBy{id}.shuffled(Random(seed))`
mindig ugyanazt a sorrendet adja. A felfedettséget **pozíció** szerint tároljuk (nem
technikaazonosító szerint): egy munkameneten belül a sorrend nem változik, tehát a kettő
egyenértékű, és a pozíció-alapú tárolás egyszerűbb. 7 teszt bizonyítja: a seed az
injektált RNG-ből jön (NFR-07, nem `Random.Default`), azonos seed azonos sorrendet ad
újraépülés után is, a pozíció és a felfedettség túléli az újraépülést (ugyanazzal a
`SavedStateHandle`-lel, mint forgatás/mentett állapotból visszaállított folyamat
esetén), az „Előző"/„Következő" a határokon no-op, a felfedettség kártyánkénti (nem
globális), és a technika nélküli kijelölés `Empty` állapotot ad, nem omlik össze.
