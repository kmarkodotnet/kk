# T-308 · feature/learn: ViewModel tesztek

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09, US-10, US-11 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-306](T-306-feature-learn-onertekeles.md) |
| Követelmény | FR-07, FR-09, FR-10 |
| Teszt | — |

## Cél

A tanulási logika lefedése.

## Teendők

- [x] Seedelt sorrend determinizmusa.
- [x] Felfedési állapot megőrzése léptetés és visszalépés után.
- [x] Önértékelés rögzítése és az automatikus továbblépés.
- [x] Feliratos képnél a felfedés előtti elrejtés.

## Kész, ha

- [x] Fake repository és fix `Clock`; a tesztek determinisztikusak.

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
| Commit | `f69422b` (közös a T-306 lezárásával, G-06 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `feature/learn`: 36 teszt, 0 hiba |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 489 teszt, 0 hiba (484 → 489) |
| `./gradlew.bat assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** a négy Teendő tételből három (seedelt sorrend, felfedési állapot
megőrzése, feliratos kép elrejtése) már a T-303/T-305 tesztkészletében lefedve volt –
ezekhez nem kellett új teszt. Az „önértékelés rögzítése és automatikus továbblépés"
tételhez 5 új teszt került a `LearnCardsViewModelTest`-be, egy új
`FakeLearnProgressRepository` tesztdublőrrel (`LearnTestFixtures.kt`): a felfedett
kártyán mentés + továbblépés, a letiltott (fel nem fedett) kártyán védelmi háló (no-op),
az önértékelés nélküli „Következő" nem ír rekordot, és az utolsó kártyán az önértékelés
is a záróképernyőre vezet. A „fix `Clock`" a meglévő `FIXED_CLOCK` fixtúra – a
`LearnCardsViewModel` maga nem igényel `Clock`-ot, mert a `ProgressRepository`
megvalósítása (`RoomProgressRepository`) már injektált `Clock`-kal dolgozik.
