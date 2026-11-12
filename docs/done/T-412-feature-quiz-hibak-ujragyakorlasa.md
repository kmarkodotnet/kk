# T-412 · feature/quiz: hibák újragyakorlása

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-16 |
| Modul | `feature/quiz` |
| Méret | M (3–6 ó) |
| Függ | [T-411](T-411-feature-quiz-eredmenykepernyo.md) |
| Követelmény | FR-21 |
| Teszt | TR-04 |

## Cél

Célzott ismétlés a hibás kérdések technikáiból, a statisztika torzítása nélkül.

## Teendők

- [x] **Új** `QuizSession` **új** seeddel, a hibás kérdések helyes technikáiból.
- [x] A régi munkamenet zárva marad, eredménye változatlan.
- [x] A gomb csak akkor jelenik meg, ha van hibás válasz.

## Kész, ha

- [x] A statisztika nem duplázódik: az új próbálkozások önálló, jogos rekordok.

## Figyelj rá

- Az új seed szándékos: ne ugyanabban a sorrendben jöjjenek a kérdések, különben a sorrendet tanulja meg a felhasználó.

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
| Commit | `5425d1e` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | Teljes projekt, 0 hiba – lásd `docs/TEST_REPORT.md` G-09 bejegyzése. |
| `./gradlew assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk` elkészült |

**Megjegyzés:** A motornak nincs technikaszintű jelöltszűrője, ezért a
`QuizResultViewModel.onRetryWrong()` egy, **kizárólag** a hibás technikákat tartalmazó
`QuizCandidateSet`-et épít (társ-technika nélkül) – ez garantálja, hogy az új menet
kizárólag a hibás technikákra kérdez (US-16 AC-1), de a motor már dokumentált,
nem-optimista szabálya miatt (`docs/CONVENTIONS.md` 1. szakasz) 4-nél kevesebb egyedi
hibás technikával nem indul új menet (egy előzetes `dryRun()` szűri ki). Lásd
`docs/CONVENTIONS.md` új sora és `docs/TEST_REPORT.md` G-09 bejegyzése.
