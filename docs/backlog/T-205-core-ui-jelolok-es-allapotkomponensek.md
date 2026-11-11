# T-205 · core/ui: jelölők és állapotkomponensek

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-08 |
| Modul | `core/ui` |
| Méret | M (3–6 ó) |
| Függ | — |
| Követelmény | FR-05, CR-03, NFR-06, ADR-010 |
| Teszt | — |

## Cél

A hitelesítési jelölések és az üres/hiba állapotok megosztott komponensei.

## Teendők

- [ ] `VerificationBadge`: „Ellenőrzésre vár” / „Hiányos” + a hiányzó mezők.
- [ ] `UnreviewedBanner`: a nem elutasítható, lektorálatlanság-szalag (ADR-010).
- [ ] `EmptyState(ok, üzenet, akciógomb?)` – az ok alapján különböző szöveggel.
- [ ] `ErrorState(üzenet, „Újra” akció)`.

## Kész, ha

- [ ] Minden szöveg `strings.xml`-ből jön.
- [ ] A szalag nem elrejthető és nem kikapcsolható.

## Figyelj rá

- Az `EmptyState` okot kap, nem csak szöveget: a „nincs tartalom” és a „nincs találat” más üzenetet és más akciót kíván (FUNCTIONAL_SPEC 2.4).

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
