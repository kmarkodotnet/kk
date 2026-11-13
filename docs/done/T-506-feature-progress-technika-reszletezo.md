# T-506 · feature/progress: technika-részletező

| | |
|---|---|
| Epik | **E5** – Haladás és beállítások |
| User story | US-18 |
| Modul | `feature/progress` |
| Méret | M (3–6 ó) |
| Függ | [T-503](T-503-feature-progress-attekinto-kepernyo.md) |
| Követelmény | FR-25, DR-07 |
| Teszt | — |

## Cél

Egy technika saját statisztikája.

## Teendők

- [x] Azonosító adatok + link a Tudástár részletezőre.
- [x] Próbálkozás, helyes, sikerarány, utolsó gyakorlás ideje.
- [x] Kérdéstípus szerinti bontás.
- [x] Önértékelési előzmény külön blokkban.
- [x] Visszavont technikánál: „Ez a technika már nem része a tananyagnak, de a korábbi eredményeid megmaradtak.”

## Kész, ha

- [x] A visszavont technika statisztikája elérhető marad (ADR-004).

## Figyelj rá

- A kérdéstípus-bontás megmutatja, hogy a felhasználó a felismerésben vagy a felidézésben gyengébb.

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
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 606 teszt, 0 hiba (a G-10 csoport együttes hozzájárulása: 587 → 606, +19) |
| `./gradlew.bat assembleDebug` | 0 | `app-debug.apk` (`app/build/outputs/apk/debug/`) |

**Megjegyzés:** új `feature/progress/technique` csomag: `ProgressTechniqueViewModel`
(ugyanaz a minta, mint a `feature/library` `LibraryDetailViewModel`-nél:
`SavedStateHandle`-ből olvasott `techniqueId`, `TechniqueRepository.getTechnique()`,
amely a visszavont technikát is visszaadja) + `ProgressTechniqueScreen`. A
`ProgressRepository` egy új, olvasó taggal bővült:
`observeStudyReviewHistory(techniqueId): Flow<List<StudyReview>>` (a
`StudyReviewDao.observeForTechnique` új, `Flow`-visszatérésű testvérmetódusára épül,
a meglévő `suspend getForTechnique` érintetlen maradt) – mind az öt
`ProgressRepository` tesztdublőr frissült. Bekötve a `KyokushinNavHost`
`progress/technique/{techniqueId}` ágába (a `PlaceholderScreen` helyén).
