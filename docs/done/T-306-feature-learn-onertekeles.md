# T-306 · feature/learn: önértékelés

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-11 |
| Modul | `feature/learn` |
| Méret | M (3–6 ó) |
| Függ | [T-301](T-301-core-data-progressrepository-tanulasi-resz.md), [T-304](T-304-feature-learn-kartya-ui.md) |
| Követelmény | FR-10, FR-28 |
| Teszt | TR-06 |

## Cél

„Tudtam” / „Gyakorolnom kell” rögzítése.

## Teendők

- [x] A két gomb **csak felfedés után** aktív.
- [x] Megnyomásra `StudyReview` mentése és automatikus továbblépés.
- [x] Önértékelés nélküli továbblépés is megengedett (ekkor nincs rekord).

## Kész, ha

- [x] A letiltott állapot oka TalkBackkel is bejelentve.
- [x] A mentés az injektált `Clock` idejével történik.

## Figyelj rá

- Az automatikus továbblépés gyorsítja a gyakorlást, de ne legyen visszavonhatatlan: az Előző gomb visszavisz.

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
| Commit | `f69422b` (közös a T-308 lezárásával, G-06 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `feature/learn`: 36 teszt, 0 hiba |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 489 teszt, 0 hiba (484 → 489) |
| `./gradlew.bat assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** a `LearnCardsViewModel` a `ProgressRepository`-t kapta meg konstruktor-
paraméterként (a Hilt kötés már megvolt a G-01 csoport óta); az `onSelfRate(rating)` a
meglévő `onNext()`-et hívja a mentés után, ezért az utolsó kártyán is a záróképernyőre
(`LearnCardsUiState.Finished`) vezet, külön eset nélkül. Az időbélyeg a
`RoomProgressRepository`-ban keletkezik (NFR-07), a ViewModel nem kapott saját `Clock`-ot.
A letiltás okát (felfedés előtt) egy mindig-látható magyarázó szöveg mondja ki a
TalkBacknek, ugyanazzal a mintával, mint a `learn/setup` letiltott gombjánál. Lásd
`docs/CONVENTIONS.md` 4. szakasz új sorai.
