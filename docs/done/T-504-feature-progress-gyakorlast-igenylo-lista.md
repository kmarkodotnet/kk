# T-504 · feature/progress: gyakorlást igénylő lista

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-18 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-26 |
| Teszt | — |

## Cél

A gyenge pontok kiemelése.

## Teendők

- [x] Legfeljebb 10 technika, a T-501 rendezése szerint.
- [x] Tételenként: romaji, magyar név, kis ábra, sikerarány, utolsó gyakorlás.
- [x] Tétel kattintható → technika-részletező.
- [x] Üres eset: „Még nincs elég adat a javaslathoz.”

## Kész, ha

- [x] Csak legalább egy próbálkozással rendelkező technika kerül be.

## Figyelj rá

- A még nem gyakorolt technika nem „gyenge pont” – azt a kvíz fogja behozni.

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
| Commit | `172afae` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 606 teszt, 0 hiba (a G-10 csoport együttes hozzájárulása: 587 → 606, +19) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** a `ProgressOverviewViewModel` a `ProgressRepository.observeNeedsPractice(10)`-et
a `TechniqueRepository.observeTechniques()`-ből feloldott romaji/magyar/ábra hármassal
párosítja, `techniqueId` szerint. Egy visszavont, de korábban gyakorolt technika
**kiesik** ebből a listából (az `observeTechniques()` csak az aktívakat adja) – a teljes
historikus adat a T-506 technika-részletezőn marad elérhető (DR-07). Új CONVENTIONS-sor
erről a szándékos kizárásról.
