# T-415 · feature/quiz: lektorálatlan tartalom jelzése

| | |
|---|---|
| Epik | **E4** – Kvíz |
| User story | US-08 |
| Modul | `feature/quiz` |
| Méret | S (1–3 ó) |
| Függ | [T-404](T-404-feature-quiz-beallito-ui.md), [T-411](T-411-feature-quiz-eredmenykepernyo.md) |
| Követelmény | CR-01, CR-03, ADR-010 |
| Teszt | — |

## Cél

A kvíz végig közli, hogy lektorálatlan anyagon gyakorol a felhasználó.

## Teendők

- [x] A kvíz indítása előtt: figyelmeztető sor a beállító képernyőn.
- [x] Az eredményképernyőn: „Az eredmény lektorálatlan tananyagon alapul.”
- [x] A jelzés a `dataset_trust` értékből jön, nem felhasználói beállításból.
- [x] Ha `reviewed` tartalom érkezik, a jelzések **maguktól** eltűnnek.

## Kész, ha

- [x] A jelzés nem elrejthető és nem kikapcsolható.
- [x] `reviewed` tartalommal a képernyők jelzés nélkül jelennek meg – kódmódosítás nélkül.

## Figyelj rá

- Ez a feltétele annak, hogy lektorálatlan tartalmon egyáltalán indulhasson kvíz (ADR-010).

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
| Commit | `TBD` (lásd a következő, kizárólag a hash rögzítésére irányuló commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | Teljes projekt, 0 hiba – lásd `docs/TEST_REPORT.md` G-09 bejegyzése. |
| `./gradlew assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk` elkészült |

**Megjegyzés:** A beállító képernyő figyelmeztető sora (Teendők 1. pontja) **már a
T-404-ben elkészült** (`UnreviewedBanner(trust = state.datasetTrust)` a
`QuizSetupScreen`-ben) – ez a feladat csak az eredményképernyőn adta hozzá, ugyanazzal
a komponenssel (ADR-010). A jelzés nem elrejthető/kikapcsolható (a `UnreviewedBanner`
szerződése szerint, T-205), és `DatasetTrust.REVIEWED` mellett automatikusan eltűnik –
ezt a `QuizResultViewModelTest.quizResult_datasetTrust_reflectsContentState_T415` teszt
igazolja a `Content.datasetTrust` oldaláról, a tényleges elrejtést/megjelenést a
`UnreviewedBanner`/`UnreviewedNotice` korábbi tesztjei bizonyítják.
