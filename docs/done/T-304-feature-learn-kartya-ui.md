# T-304 · feature/learn: kártya UI

| | |
|---|---|
| Epik | **E3** – Tanulás tanulókártyákkal |
| User story | US-09, US-10 |
| Modul | `feature/learn` |
| Méret | L (6–12 ó) |
| Függ | [T-303](T-303-feature-learn-kartyaallapot.md), [T-204](T-204-core-ui-techniqueillustration.md) |
| Követelmény | FR-07, FR-08 |
| Teszt | TR-06 |

## Cél

A tanulókártya megjelenítése és felfedése.

## Teendők

- [x] Felső sáv: „`{n}` / `{összes}`”, kategória, bezárás.
- [x] Kártya: ábra, a nem elrejtett név nagy betűvel, az elrejtett helyén „Koppints a felfedéshez”.
- [x] Felfedés után **mindhárom** látszik: romaji + magyar név + ábra.
- [x] Alsó sáv: Előző / Következő.

## Kész, ha

- [x] `stateDescription`: felfedés előtt „elrejtve”, utána „felfedve”.
- [x] Nagy érintési felület a felfedéshez (≥ 48 dp).

## Figyelj rá

- A felfedés explicit interakció legyen – a véletlen felfedés elrontja a tanulást (FR-07).

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
| Commit | `ca1e230` (kód), közös a T-305/T-307 lezárásával (G-05 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:learn:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `LearnCardPoliciesTest` (8) + `LearnCardsViewModelTest` (10, nettó +3) |
| `./gradlew.bat :navigation:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – `NavigationGraphTest` +2 teszt |
| `./gradlew.bat test testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – **484 teszt, 0 hiba** (471 → 484) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** Lásd a T-307 lezárását a G-05 csoport teljes részletezéséért
(`LearnCardsScreen.kt`, a `navigation` modul bekötése, a feliratos kép elrejtésének
akadálymentességi felismerése). Ide a legfontosabb T-304-specifikus pont: a felfedés
előtt az ábra `IllustrationLabel`-je **`Decorative`**, nem a technika neve – különben a
`TechniqueIllustration` `contentDescription`-je kimondaná a rejtett nevet, és ez
ugyanúgy elárulná a választ, mint egy véletlen felfedés (FR-07, NFR-04). Ezt a
`LearnCardIllustrationLabelPolicy` tiszta függvénye adja, `LearnCardPoliciesTest`
bizonyítja.
