# T-607 · Lint és kódformázás

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | gyökér |
| Méret | M (3–6 ó) |
| Függ | — |
| Követelmény | NFR-11 |
| Teszt | — |

## Cél

A statikus ellenőrzés bekötése és a találatok rendezése.

## Teendők

- [x] Android Lint konfigurálása, a projekt szempontjából lényeges szabályok bekapcsolása.
- [x] ktlint (vagy egyenértékű formázó) bekötése.
- [x] A meglévő találatok javítása vagy indokolt elnyomása.

## Kész, ha

- [x] `./gradlew lint` és a formázásellenőrzés exit 0.

## Figyelj rá

- Az elnyomott szabályokhoz indoklás tartozzon – a néma `@Suppress` később senkinek nem mond semmit.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára (n/a – ez egy tooling/formázási feladat, nincs új UI; a meglévő `@Preview` annotációk csak sortörést kaptak, tartalom változatlan)
- [x] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [x] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel (n/a – minden lefutott)

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | `e2114fb` (lint-javítás), `18a5b12` (ktlint bevezetése), `a1eed5f` (teljes kódbázis formázása) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew lint --rerun-tasks` | 0 | 9 Android modul, `PropertyEscape` elnyomva (indoklással), `RedundantLabel` javítva, a többi figyelmeztetés dokumentáltan meghagyva |
| `./gradlew ktlintCheck --rerun-tasks` | 0 | teljes projekt, 77 task, mindegyik tényleg futott |
| `./gradlew testDebugUnitTest --rerun-tasks` | 0 | 642 teszt, 0 hiba, teljes projekt |
| `./gradlew assembleDebug --rerun-tasks` | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` |

**Megjegyzés:** a ktlint-gradle (14.2.0) minden modulra bekötve a gyökér
`build.gradle.kts` `subprojects {}` blokkjából, a szabályok a gyökér
`.editorconfig`-ban élnek. Négy, indokolt `.editorconfig` szabály-kikapcsolás
(`function-naming` a Compose PascalCase konvencióhoz, négy ktlint 1.x sortörés-szabály,
amely nem konvergál stabilan a meglévő kódon, és `filename` négy, lezárt feladatok
dokumentációja által szó szerint idézett fájlnévhez) és három elszigetelt
`@Suppress("ktlint:standard:max-line-length")` (KDoc Markdown-táblázatok, amelyek
celláit tördelve eltörne a renderelés) – mindegyik indoklással a kódban és a
`docs/CONVENTIONS.md` 6. szakaszában. A `ktlint_code_style = android` érték
érvénytelennek bizonyult ktlint 1.5.0-ban (csendben `ktlint_official`-ra esett
vissza) – nem lett beállítva. Részletek: `docs/TEST_REPORT.md` 2026-11-13 „T-607"
szakasza.
