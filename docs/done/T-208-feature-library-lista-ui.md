# T-208 · feature/library: lista UI

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-06, US-08 |
| Modul | `feature/library` |
| Méret | L (6–12 ó) |
| Függ | [T-207](T-207-feature-library-lista-viewmodel.md), [T-204](T-204-core-ui-techniqueillustration.md) |
| Követelmény | FR-01, FR-02, FR-03, FR-05 |
| Teszt | TR-06 |

## Cél

A Tudástár listaképernyő.

## Teendők

- [ ] Keresősáv törlés ikonnal; vízszintesen görgethető kategóriachip-sor találatszámmal, „Összes” chippel.
- [ ] `LazyColumn` stabil `key`-jel; listatétel: kis ábra, romaji (félkövér), magyar név, hitelesítési chip.
- [ ] Alias-találatnál: „Egyezés: `{alias}`” sor.
- [ ] Rendezés: kategória `sort_order`, azon belül romaji ábécé.

## Kész, ha

- [ ] A listatétel egy TalkBack bejárási egység, és ≥ 64 dp magas.
- [ ] `@Preview` világos + sötét + üres állapot.

## Figyelj rá

- A találatszám a chipeken azért hasznos, mert a felhasználó előre látja, melyik kategóriában van anyag.

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
| Dátum | 2026-11-12 |
| Commit | lásd a T-210-zel közös commit (G-03 csoport) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat :feature:library:testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 9 új teszt zöld (`LibraryListAccessibilityTest` 5, `LibraryEmptyStateActionTest` 4) |
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | `BUILD SUCCESSFUL` – 434 teszt, 0 hiba, 0 kihagyott (425 → 434) |
| `./gradlew.bat assembleDebug` | 0 | `BUILD SUCCESSFUL` – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:** A Compose lista felépült (`LibraryListScreen.kt`, `LibraryListAccessibility.kt`
a `feature/library/list` csomagban), és a `navigation` modulban a `library/list` útvonal
helyőrzője lecserélve a valódi `LibraryListRoute`-ra (korábban csak dokumentálva volt, hogy
ez a T-208 tárgya). A kategóriaszűrő „Összes" chipje nem önálló ViewModel-akció: a jelenleg
kijelölt kategóriákat egyenként kapcsolja ki `onToggleCategory`-n keresztül (lásd
CONVENTIONS.md 4.1 új sora). A `TechniqueIllustration` `null` hivatkozás esetén – a
komponens dokumentált szerződése szerint – a listán **nem** rajzol helyőrzőt, csak a
56 dp-es hely marad fenntartva, hogy a sorok egymáshoz igazodjanak. A pontos pixelesen
négyzet alakú 56×56 dp méret nem garantált (a komponens a kép/placeholder saját
képarányát tartja meg); ez nem automatizáltan tesztelt elvárás, csak vizuális irányadó a
FUNCTIONAL_SPEC 3.1-ben. `@Preview` világos+sötét az összes állapotra (Loading, Empty ×2,
Content, Error) elkészült. Compose UI teszt (tényleges renderelés/koppintás) nem futott –
lásd TEST_REPORT.md, B-03.
