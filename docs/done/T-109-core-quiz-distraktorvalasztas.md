# T-109 · core/quiz: distraktorválasztás

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-02 |
| Modul | `core/quiz` |
| Méret | L (6–12 ó) |
| Függ | [T-108](T-108-core-quiz-jeloltszures-es-jogosultsagi-kapuk.md) |
| Követelmény | FR-14, CR-07 |
| Teszt | TR-02 |

## Cél

A három téves válasz kiválasztása – a kvízhelyesség legkényesebb pontja.

## Teendők

- [ ] Prioritás: (1) azonos kategória, (2) más kategória, (3) **a kérdés nem jön létre**.
- [ ] Kizárási szűrő minden jelöltre: nem a helyes, nem szinonimája (V006 csoportok), típusra jogosult.
- [ ] (D) típusnál: nincs két vizuálisan azonos illusztráció az opciók között (SHA-256 egyezés alapján).
- [ ] Helyes technika nem ismétlődik, amíg van fel nem használt jelölt; különben `shortfall`.

## Kész, ha

- [ ] Pontosan 4 opció vagy **semmi** – 3 opciós kérdés nem keletkezhet.
- [ ] A motor soha nem **gyárt** opciót: minden opció létező technikarekord.

## Figyelj rá

- A legveszélyesebb hiba itt az lenne, ha kevés adatnál a motor „kisegítene” egy szinonimával vagy generált opcióval. Inkább legyen kevesebb kérdés (FR-22).
- A kategórián belüli distraktor a tanulási cél: a hasonlókat kell megkülönböztetni.

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
| Dátum | 2026-11-11 |
| Commit | `378d6f1` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | BUILD SUCCESSFUL – 181 teszt, 0 hiba (core/model 35 + core/quiz 146; `DistractorSelectorTest` 20) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- A `DistractorSelector` a `QuizCandidateFilter.optionPool`-ra épül, nem duplikálja a
  jogosultsági kaput. Belépési pontok a T-110-hez:
  `acceptableDistractors(request, type, correct)` (rendezett jelöltlista, prioritás szerint),
  `selectDistractors(...)` (három distraktor vagy `null`),
  `selectOptionIds(...)` (négy opció kanonikus sorrendben, a helyes elöl),
  `buildableCorrectAnswers(request, type)` (a kérdéssorrend ismétlésmentes alapja).
- A `QuizCandidateFilter.buildableQuestionCount` **már nem felső korlát**: a
  `buildableCorrectAnswers` tényleges kimenetét számolja, és a `skipReason` is ebből
  következik. Aki a számot hívja, pontos értéket kap (a T-111 `dryRun` erre épülhet).
- A CR-07 kizárás **páronként** érvényes: két szinonim rekord egy kérdésbe sem kerül, nem
  csak a helyes válasz mellé nem. Ez szigorúbb, mint a kvízmotor-skill 4. invariánsa.
- A kiválasztás szándékosan **konzervatív** (első elfogadható jelölt, nem maximális
  párosítás): nem tranzitív, egymást keresztező szinonimacsoportoknál elvileg kimaradhat
  egy elvben összeállítható kérdés. FR-22 szerint ez a jó irány, és a `canBuildQuestion`
  ugyanazt a függvényt kérdezi, így a számolt és a tényleges kérdésszám nem válik el.
- A `Shortfall` tényleges előállítása **nem** készült el ebben a körben: a `QuizPlan`
  összeállítása (kérdéssorrend, opciókeverés, shortfall) a T-110 tárgya – a `QuizGenerator`
  egyelőre csak interfész, implementáció nélkül.
