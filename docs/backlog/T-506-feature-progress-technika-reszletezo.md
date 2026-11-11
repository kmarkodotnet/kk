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

- [ ] Azonosító adatok + link a Tudástár részletezőre.
- [ ] Próbálkozás, helyes, sikerarány, utolsó gyakorlás ideje.
- [ ] Kérdéstípus szerinti bontás.
- [ ] Önértékelési előzmény külön blokkban.
- [ ] Visszavont technikánál: „Ez a technika már nem része a tananyagnak, de a korábbi eredményeid megmaradtak.”

## Kész, ha

- [ ] A visszavont technika statisztikája elérhető marad (ADR-004).

## Figyelj rá

- A kérdéstípus-bontás megmutatja, hogy a felhasználó a felismerésben vagy a felidézésben gyengébb.

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
