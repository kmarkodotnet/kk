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

- [ ] Teszt, amely az összevont manifestben ellenőrzi, hogy **nincs** `android.permission.INTERNET`.
- [ ] Az AndroidX által automatikusan hozzáadott `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` **megengedett**.

## Kész, ha

- [ ] A teszt nem „nulla permission”-t vár, hanem konkrétan az INTERNET hiányát.

## Figyelj rá

- A 2026-11-11-i build igazolta, hogy az AndroidX hozzáad egy saját, aláírásszintű permissiont – egy „nulla permission” assertion hamisan bukna (lásd `docs/TEST_REPORT.md`).

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
