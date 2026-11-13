# T-611 · A kész funkcióképernyők navigációs integrációja

| | |
|---|---|
| Modul | navigation, feature/quiz, feature/library, feature/progress |
| Függ | T-404, T-405, T-411, T-412, T-507, T-508 |
| Követelmény | FR-11, FR-16…FR-20, FR-32, FR-35, NFR-06 |
| Teszt | TR-06, kvízállapot-visszaállítási regressziós teszt |

A 2026-11-13-i folytatáskor a funkciómodulok elkészültek, de a futó appban a kvíz és
a beállítások helyőrzők voltak. Ez az integráció az emulátoros elfogadási tesztek előfeltétele.

## Teendők és készültségi feltételek

- [x] Beállítás → kvíz → eredmény → új kvíz / hibák újragyakorlása / haladás működik.
- [x] A menet terve és seedje a ViewModel mentett állapotába kerül; visszaállítás nem törli a válaszokat.
- [x] A tanulásból érkező kategóriakijelölés megmarad.
- [x] Beállítások és kizárólag debugban elérhető diagnosztika bekötve.
- [x] Teljes unit teszt, debug build, lint és formázás sikeres.
- [x] Emulátoros navigációs és teljes kvízteszt sikeres.
- [x] Tényleges eredmények és állás a TEST_REPORT / IMPLEMENTATION_PLAN fájlokban.

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-13 |
| Commit | (a lezáró commit után rögzítve) |

Setup/run/result/settings/diagnostics bekötve. Explicit SavedStateHandle-bemenet, újragyakorlás, megszakítás, tanulási kategóriák megőrzése; unit és eszközteszt igazolja.

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest assembleDebug lint ktlintCheck connectedDebugAndroidTest --console=plain` | 0 | 647 unit teszt, 6 eszközteszt, 0 hiba; debug APK, lint és formázás sikeres |
| `.\gradlew.bat :app:connectedDebugAndroidTest ktlintCheck --console=plain` | 0 | Végső kártyaforgatási kiegészítéssel is 6 eszközteszt, 0 hiba |

Részletes bizonyítékok és NEM FUTOTT tételek: [TEST_REPORT](../TEST_REPORT.md),
[elfogadási jegyzőkönyv](../ACCEPTANCE.md). Nincs új Clock/Random hívás,
repository-réteghatár nem sérült; a meglévő magyar strings.xml és világos/sötét
Preview-k megmaradtak. A teszt-only magyar feliratok a valós UI-folyamatot azonosítják.
