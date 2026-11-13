# T-509 · feature/progress: ViewModel tesztek

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-17, US-18, US-19 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-507](T-507-feature-progress-beallitasok.md) |
| Követelmény | FR-26, FR-27, FR-35 |
| Teszt | — |

## Cél

A haladás logikájának lefedése.

## Teendők

- [x] „Nincs adat” esetek (0 próbálkozás összesen, 0 egy kategóriában).
- [x] A „gyakorlást igénylő” rendezés helyessége.
- [x] Törlés hatása: a statisztika nullázódik, a tartalom megmarad.
- [x] Az önértékelés nem szivárog a kvízsikerarányba.

## Kész, ha

- [x] Fake repository, fix `Clock`.

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
| Dátum | 2026-11-13 |
| Commit | (lásd a következő, dokumentációs commit) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 613 teszt, 0 hiba (a G-11 csoport együttes hozzájárulása: 606 → 613, +7) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** `ProgressSettingsViewModelTest` (6 eset, fix tartalmú
`FakeProgressSettingsRepository`/`FakeProgressTechniqueRepository`-val – nincs
`Clock` injektálva, mert a `ClearProgressStep` állapotgépnek nincs időfüggő logikája):
„nincs adat” importált/nem importált tartalominfóra, a kétlépcsős megerősítés
sorrendje (a repository csak a végső megerősítés után híva, egyszer), „Mégsem”
bármelyik lépésben, és a törlés után is változatlan tartalominfó. Egy dedikált eset
(`FakeResettableProgressRepository`, egyetlen megosztott példány egy
`ProgressOverviewViewModel` és egy `ProgressSettingsViewModel` között) bizonyítja,
hogy a Beállításokon megerősített törlés az áttekintő statisztikáját nullázza, a
tartalominfót nem. A „gyakorlást igénylő” rendezés **megőrzését** (a tényleges
rendezés a T-501 SQL `@Query`-je) egy új `ProgressOverviewViewModelTest` eset fedi
(`progressOverview_needsPractice_preservesRepositoryOrder_FR26`); a „0 egy
kategóriában” eset és az önértékelés-függetlenség a G-10 csoport már meglévő
teszteiben megvolt – nem kellett megduplázni.
