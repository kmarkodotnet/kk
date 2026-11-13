# T-603 · TR-08: manifest permission teszt

| | |
|---|---|
| Epik | **E6** – Minőség, akadálymentesség, átadás |
| User story | US-21 |
| Modul | `app` |
| Méret | S (1–3 ó) |
| Függ | — |
| Követelmény | NFR-01 |
| Teszt | TR-08 |

## Cél

Az offline ígéret gépi ellenőrzése.

## Teendők

- [x] Teszt, amely az összevont manifestben ellenőrzi, hogy **nincs** `android.permission.INTERNET`.
- [x] Az AndroidX által automatikusan hozzáadott `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` **megengedett**.

## Kész, ha

- [x] A teszt nem „nulla permission”-t vár, hanem konkrétan az INTERNET hiányát.

## Figyelj rá

- A 2026-11-11-i build igazolta, hogy az AndroidX hozzáad egy saját, aláírásszintű permissiont – egy „nulla permission” assertion hamisan bukna (lásd `docs/TEST_REPORT.md`).

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [x] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára (n/a – nincs UI ebben a feladatban)
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
| Commit | `46d15fe` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew :app:testDebugUnitTest --tests "hu.kyokushin.trainer.ManifestPermissionTest" --rerun-tasks` | 0 | 1 teszt, 0 hiba |
| `./gradlew testDebugUnitTest --rerun-tasks` (teljes projekt, a T-607 lezárásakor együtt) | 0 | 642 teszt, 0 hiba |
| `./gradlew assembleDebug --rerun-tasks` (teljes projekt, a T-607 lezárásakor együtt) | 0 | `app-debug.apk`, `BUILD SUCCESSFUL` |

**Megjegyzés:** a teszt szándékosan a sima `Application`-t használja Robolectric alatt,
nem a `HiltTestApplication`-t (mint a `HiltGraphTest`) – a valódi `KyokushinApplication`
`@HiltAndroidApp`, aminek felépítése a teljes Hilt gráfot igényelné, de ennek a
tesztnek csak az összevont manifestre van szüksége. Részletek:
`docs/TEST_REPORT.md` 2026-11-13 „T-603" szakasza.
