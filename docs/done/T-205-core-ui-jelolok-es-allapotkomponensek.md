# T-205 · core/ui: jelölők és állapotkomponensek

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-08 |
| Modul | `core/ui` |
| Méret | M (3–6 ó) |
| Függ | — |
| Követelmény | FR-05, CR-03, NFR-06, ADR-010 |
| Teszt | — |

## Cél

A hitelesítési jelölések és az üres/hiba állapotok megosztott komponensei.

## Teendők

- [ ] `VerificationBadge`: „Ellenőrzésre vár” / „Hiányos” + a hiányzó mezők.
- [ ] `UnreviewedBanner`: a nem elutasítható, lektorálatlanság-szalag (ADR-010).
- [ ] `EmptyState(ok, üzenet, akciógomb?)` – az ok alapján különböző szöveggel.
- [ ] `ErrorState(üzenet, „Újra” akció)`.

## Kész, ha

- [ ] Minden szöveg `strings.xml`-ből jön.
- [ ] A szalag nem elrejthető és nem kikapcsolható.

## Figyelj rá

- Az `EmptyState` okot kap, nem csak szöveget: a „nincs tartalom” és a „nincs találat” más üzenetet és más akciót kíván (FUNCTIONAL_SPEC 2.4).

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
| Commit | `e73ac8e` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:ui:testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` – 28 teszt (18 új) zöld |
| `.\gradlew.bat testDebugUnitTest` | 0 | `BUILD SUCCESSFUL` |
| `.\gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 393 teszt, 0 hiba, 0 kihagyott |
| `.\gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` (20 296 639 byte) |

**Megjegyzés:**

- A szalag neve **`UnreviewedBanner`**, nem `DemoBanner`: a FUNCTIONAL_SPEC 2.1 még a
  `datasetKind = "demo"` olvasatot írja, az ADR-010 viszont `dataset_trust`/`editorial_status`
  alapú jelzést döntött el, és az ADR felülírja a korábbi szövegezést. A szalag szövege az
  ADR-010 szerinti: „Lektorálatlan gyakorlóanyag – a tartalom szakmai ellenőrzésre vár."
  **Javasolt új task:** a FUNCTIONAL_SPEC 2.1 és a 8. szövegtáblázat `content.demoBanner`
  sorának átvezetése az ADR-010-re (a T-608 és a T-610 dokumentációs körébe illik).
- Az `UnreviewedBanner`-nek **nincs** elrejtő paramétere – az ADR-010 elrejthetetlensége az
  API alakjába van zárva. A T-415 ezt a komponenst használja, nem újat ír.
- Az `EmptyStateReason` négy okot ismer: `LibraryNoContent`, `LibraryNoResult(query)`,
  `ProgressNoData`, `QuizNotEnoughData`. A T-210 (üres és hibaállapotok) és a T-503
  ezekre épít; új magyar szöveget a képernyőkön nem kell írni.
- A `VerificationBadge` a hiányzó mezőket `TechniqueField` enumként kéri, nem stringként.
  A `Technique` domain modell **nem** hordozza, melyik mezője hiányzik, ezért a hívónak a
  validációs jelentésből kell előállítania. A gyakorlatban a betöltött `INCOMPLETE` rekord
  oka a lógó képhivatkozás (V005), tehát `TechniqueField.ILLUSTRATION`.
- A `core/ui` `androidx-core-ktx` függősége `implementation` → `api` lett, mert a publikus
  API `@StringRes` annotált azonosítókat hordoz.
