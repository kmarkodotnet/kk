# T-117 · core/data: ContentImporter

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-115](T-115-core-data-a-kanonikus-sema-beolvasasa.md), [T-116](T-116-core-data-room-sema.md) |
| Követelmény | FR-29, FR-30, FR-31, FR-33 |
| Teszt | TR-05 |

## Cél

A betöltési folyamat: DTO → validátor → upsert, az előrehaladás sérülése nélkül.

## Teendők

- [x] `importIfNeeded()`: csak akkor fut, ha a csomagolt `dataset_version` ≠ a tárolt.
- [x] **Upsert**, nem `DELETE ALL` + `INSERT`.
- [x] A csomagban már nem szereplő technika: `retired = true`, **nem** fizikai törlés.
- [x] A `ValidationReport` eltárolása a debug diagnosztikához (`lastValidationReport()`).
- [x] Háttérszálon fut (injektált `@IoDispatcher`); a `Loading` állapot megjelenítése a hívó UI dolga (a `suspend` szerződés ezt teszi lehetővé), UI ebben a feladatban nincs.

## Kész, ha

- [x] Ugyanazt a verziót kétszer betöltve nincs duplikáció.
- [x] A `quiz_attempt` és `study_review` sorok száma változatlan marad egy frissítés után.

## Figyelj rá

- Ez a task a FR-33 garancia megvalósítása. A TR-05 teszt (T-118) pontosan ezt méri.
- A `retired` jelölés miatt minden tartalomlekérdezésnek szűrnie kell rá – a repository felelőssége (T-201).

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [x] `./gradlew assembleDebug` exit 0
- [x] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [x] Nem sért rétegszabályt (ADR-001, L1–L6)
- [x] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [x] Minden felhasználói szöveg `strings.xml`-ben, magyarul – **nem keletkezett** felhasználói szöveg (a jelentés szövegei fejlesztői diagnosztika)
- [–] UI esetén: **nem értelmezhető** (nincs UI ebben a feladatban) – `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
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
| Dátum | 2026-11-11 |
| Commit | ef3bc14 |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | 68 teszt, 0 hiba (16 új) |
| `.\gradlew.bat testDebugUnitTest` | 0 | **330 teszt, 0 hiba, 0 kihagyott** |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL – `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- A publikus szerződés: `ContentImporter` + `ImportResult` (`UpToDate` / `Imported` /
  `Rejected`); a `RoomContentImporter` `internal`. A `core/data` → `core/quiz` függés
  `implementation`-ből **`api`**-ra változott, mert a `lastValidationReport()`
  `ValidationReport`-ot ad vissza (ARCHITECTURE 5.3).
- A FR-33 garancia szerkezeti: az importáló sehol nem hivatkozik a felhasználói táblákra,
  és a technikára csak `@Upsert` + `markRetired` megy. Az **elvetett** rekord is `retired`
  jelölést kap – érvénytelen tartalmat nem mutatunk meg, de a sor és a rá hivatkozó
  előrehaladás megmarad.
- Elutasított adathalmaz (V001 / V010) esetén a tartalomtáblák érintetlenek, és a
  `content_state` is csak akkor íródik, ha még egy sikeres betöltés sem volt – különben a
  kijelzett verzió elcsúszna a táblák tartalmától.
- **T-203-nak:** a `RoomContentImporter` `@Inject` konstruktort kapott, de kötése nincs.
  A `@IoDispatcher` qualifier a `core/data` `di` csomagjában él (a `di` modul a
  `core/data`-ra függ, nem fordítva), és mivel a `KyokushinDatabase` meg a
  `KnowledgeContentReader` `internal`, ezeknek a `@Provides`/`@Binds` kötéseknek a
  `core/data`-ban kell élniük, nem a `di` modulban – ez eltérés az ARCHITECTURE 9.
  táblázatától, és a T-203-nak kell eldöntenie.
- **T-201-nek:** a tartalom oldali entitás ↔ domain mapper kész
  (`db/ContentEntityMappers.kt`: technika, alias, kategória, illusztráció, szinonimacsoport).
  A `ContentState` **domain** modellje még nincs meg (a `core/model`-ben sincs típus rá) –
  az az `observeContentState()` feladata.
- **Javasolt új task:** a manifest kategóriamezőinek nincs validációs szabálykódja
  (V001…V010 egyike sem fedi a `name_hu` hiányát). Jelenleg a név nélküli kategória az
  azonosítóját mutatja megjelenítendő névként – kitalált magyar nevet nem gyártunk (CR-01),
  de a helyes válasz egy új szabálykód a validátorban.
- **Nyitott viselkedés:** ha egy értelmezhető csomag **minden** rekordja elvetődik
  (`datasetRejected = false`, `accepted` üres), az import minden korábbi technikát
  visszavon. A felhasználói adat ettől sem sérül, de a tudástár kiürül – érdemes lehet
  külön kapu (új task), nem ebben a körben.
