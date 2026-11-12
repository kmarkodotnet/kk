# T-209 · feature/library: részletező képernyő

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-07, US-08 |
| Modul | `feature/library` |
| Méret | L (6–12 ó) |
| Függ | [T-207](T-207-feature-library-lista-viewmodel.md) |
| Követelmény | FR-04, FR-05 |
| Teszt | TR-06 |

## Cél

Egy technika teljes adatlapja.

## Teendők

- [ ] Nagy illusztráció, romaji, magyar név, japán íráskép (ha van), aliasok, kategóriachip, magyarázat.
- [ ] Hitelesítési blokk: státusz és `VERIFIED` esetén a forrás.
- [ ] Ha már gyakoroltam: kis összegzés + link a haladás-részletezőre.
- [ ] Üres opcionális mező → a szakasz nem jelenik meg.

## Kész, ha

- [ ] Hiányos rekordnál a „Hiányos rekord” blokk és a kvízkizárás jelzése megjelenik.
- [ ] A kategóriachipre koppintva a listára jutunk, arra a kategóriára szűrve.

## Figyelj rá

- A „nincs adat” feliratok halmozása zajos lenne – inkább ne jelenjen meg a szakasz (FUNCTIONAL_SPEC 3.2).

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
| Commit | b3ede97 |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest --rerun-tasks` | 0 | 441 teszt, 0 hiba, 0 kihagyott (434 → 441), minden task frissen futtatva |
| `./gradlew.bat assembleDebug` | 0 | `app/build/outputs/apk/debug/app-debug.apk`, `BUILD SUCCESSFUL` |

**Kész ha / Definition of Done:**

- [x] Hiányos rekordnál a „Hiányos rekord" blokk (`VerificationBadge`) és a kvízkizárás jelzése megjelenik – `LibraryDetailScreen.TechniqueStatusSection`, igazolva: `libraryDetail_incompleteRecord_missingFieldsNameTheIllustration_FR05`.
- [x] A kategóriachipre koppintva a listára jutunk, arra a kategóriára szűrve – a `navigation` modul `previousBackStackEntry.savedStateHandle` + `popBackStack()` mintája, a `LibraryListViewModel.KEY_CATEGORIES` kulcson.
- [x] `assembleDebug` exit 0.
- [x] `testDebugUnitTest` exit 0, a tesztnevek `FR04`/`FR05`/`US07`/`US08`-ra hivatkoznak.
- [x] Nem sért rétegszabályt – a `feature/library/detail` csomag nem hivatkozik Room DAO-ra/entitásra/DTO-ra (ellenőrizve: `grep` nem talált `androidx.room`/`Dao`/`Entity` hivatkozást).
- [x] Nincs közvetlen `Clock`/`Random` hívás (ellenőrizve: nincs `System.currentTimeMillis`/`Instant.now()`/`Random.Default` a modulban).
- [x] Minden felhasználói szöveg `strings.xml`-ben (`library_detail_aliases`, `library_detail_quiz_excluded_notice`, `library_detail_not_found`).
- [x] UI: `contentDescription` (a megosztott `TechniqueIllustration`/`VerificationBadge`-en keresztül), ≥48 dp célpont a kategóriachipen, `@Preview` világos + sötét témára mind a négy állapotra (8 előnézet).
- [x] `docs/IMPLEMENTATION_PLAN.md` frissítve (4.4 sor, M3 mérföldkő, changelog).
- [x] A nem futott ellenőrzések (`connectedDebugAndroidTest`, tényleges Compose UI/koppintás-bejárás, `ktlintCheck`) a `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok" sorként szerepelnek.

**Megjegyzés:** Két, a Teendők listájából deliberáltan kihagyott pont (a „Kész ha" nem
követeli meg őket): a „már gyakoroltam" összegzés (FUNCTIONAL_SPEC 3.2 11.) nincs
megírva, mert nincs `ProgressRepository` (T-301/T-401 hátra); a hitelesítési blokk nem
jelenít meg `source` mezőt `verified = true` esetén (3.2 10.), mert a kanonikus séma
(ADR-011) technikaszinten nem ad ilyen mezőt. Mindkettő dokumentálva: `docs/CONVENTIONS.md`
8. szakasz, `docs/IMPLEMENTATION_PLAN.md` 4.4 sor és „Következő lépés". Amikor a
`ProgressRepository` elkészül, az első pontot pótolni kell a `LibraryDetailUiState`-ben.
