# T-101 · core/model: azonosítók és enumok

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-01 |
| Modul | `core/model` |
| Méret | S (1–3 ó) |
| Függ | — |
| Követelmény | DR-01, DR-03, DR-04 |
| Teszt | — |

## Cél

A domain alaptípusai. Minden más modul ezekre épül, ezért ez a legelső darab.

## Teendők

- [ ] `@JvmInline value class`: `TechniqueId`, `CategoryId`, `SessionId`, `QuestionId` – mind `String` köré.
- [ ] Enumok: `QuestionType { JP_TO_HU, HU_TO_JP, IMAGE_TO_JP, JP_TO_IMAGE }`, `SelfRating { KNEW, NEEDS_PRACTICE }`, `VerificationStatus { VERIFIED, AWAITING_REVIEW, INCOMPLETE }`, `DatasetTrust { REVIEWED, PRACTICE_ON_UNREVIEWED }`.
- [ ] KDoc minden típusra: mit jelent, melyik ADR vagy követelmény indokolja.

## Kész, ha

- [ ] A `core/model` modul lefordul, és **nincs benne egyetlen `android.*` vagy `androidx.*` import sem**.
- [ ] A value class-ok elrejtik a nyers `String`-et: nincs publikus konstruktor-megkerülés.

## Figyelj rá

- A value class azért kell, mert a `TechniqueId` és a `CategoryId` összekeverése fordítási hiba legyen, ne futásidejű bug (DR-03).
- A `DatasetTrust` az ADR-010 kapuja: a bizalmi szint a tartalomból jön, nem felhasználói beállításból.

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
| Commit | `36a8a75` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew testDebugUnitTest` | 0 | 13 teszt, 0 hiba (`:core:model:test`), `BUILD SUCCESSFUL` |
| `./gradlew assembleDebug` | 0 | `BUILD SUCCESSFUL`, `app/build/outputs/apk/debug/app-debug.apk` (19 599 736 bájt) |

**Megjegyzés:**

1. **Privát konstruktor + `of()` gyár.** A „nincs publikus konstruktor-megkerülés" pontot
   úgy teljesítettük, hogy a négy value class konstruktora privát, és a példányt a
   `TechniqueId.of("…")` gyár adja, amely trimmel és elutasítja az üres azonosítót.
   **A következő taskok ezért `TechniqueId("x")` helyett `TechniqueId.of("x")`-et írjanak.**
2. **`testDebugUnitTest` alias a Kotlin JVM modulban.** A `:core:model` nem AGP-modul, így
   nem volt `testDebugUnitTest` taskja: a globális DoD parancsa kihagyta volna a tesztjeit.
   A `core/model/build.gradle.kts` most regisztrál egy azonos nevű gyűjtőtaskot a `test`-re.
   **A `:core:quiz` ugyanezt igényli majd, amint tesztet kap (T-104–T-106).**
3. **Tesztnév-azonosítók.** A task „Teszt" mezője üres, TR-kód nem tartozik hozzá, ezért a
   tesztnevek a fejlécben hivatkozott követelményekre mutatnak (DR-01, DR-03, DR-04), az
   enumok esetében FR-10, FR-12, CR-01 és CR-02-re.
4. **Nincs új task-javaslat** – menet közben nem találtunk javítandó hibát.
