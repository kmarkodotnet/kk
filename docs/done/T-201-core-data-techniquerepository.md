# T-201 · core/data: TechniqueRepository

| | |
|---|---|
| Epik | **E2** – Tudástár böngészése és keresése |
| User story | US-05, US-07 |
| Modul | `core/data` |
| Méret | M (3–6 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | NFR-03, DR-07 |
| Teszt | — |

## Cél

A tartalom olvasási felülete a feature modulok felé – domain modellel, nem entitással.

## Teendők

- [ ] `interface TechniqueRepository`: `observeCategories()`, `observeTechniques(query, categories)`, `getTechnique(id)`, `getQuizCandidates(...)`, `observeContentState()`.
- [ ] Implementáció `internal`, Hilt köti be (T-203).
- [ ] Entity → domain mapperek; a `retired = true` rekordok alapból kiszűrve.
- [ ] `Flow` olvasásra, `suspend` pontszerű lekérdezésre.

## Kész, ha

- [ ] A publikus API-ban nem szerepel Room entitás, DAO vagy DTO típus (L4).
- [ ] A `retired` szűrés a repositoryban van, nem a hívónál.

## Figyelj rá

- A `retired` szűrés elfelejtése csendes hiba lenne (visszavont technikák megjelennének) – ezért a repository felelőssége, nem a UI-é.

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
| Commit | `f5b4f46` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat testDebugUnitTest` | 0 | 352 teszt, 0 hiba (12 új) |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` |

**Megjegyzés:**

- **Új publikus API** a `core/data`-ban: `hu.kyokushin.trainer.core.data.repository`
  csomag – `TechniqueRepository` interfész és a `ContentState` domain modell
  (`NotImported` / `Imported`). A megvalósítás `internal RoomTechniqueRepository`,
  `@Inject` konstruktorral: `(KyokushinDatabase, @IoDispatcher CoroutineDispatcher)`.
- **A kötés a T-203 dolga, és a `core/data`-ban kell élnie**, nem a `di` modulban: a
  `KyokushinDatabase` `internal`, a `di` modul nem látja a típust, amit injektálnia
  kellene. Ugyanaz a helyzet, mint a `RoomContentImporter`-nél (T-117).
- **A `retired` szűrés helye** (ADR-004, DR-07): a listán és a kvízjelölteknél SQL-ben
  (`WHERE retired = 0`). A `getTechnique(id)` **szándékos kivétel** – a Haladás képernyő
  technika-részletezőjének a visszavont technikát is fel kell oldania, hogy kiírhassa:
  „már nem része a tananyagnak, de a korábbi eredményeid megmaradtak"
  (FUNCTIONAL_SPEC 6.2). A `null` ezért csak ismeretlen azonosítót jelent. A hívó a
  `Technique.retired` zászlóból a *megjelenítésről* dönt, nem a szűrésről.
- **Üres halmaz = minden kategória** konvenció az `observeTechniques`-ban és a
  `getQuizCandidates`-ban; üres keresőszöveg = nincs szöveges szűrés.
- **A T-202-nek hagyva:** a keresés jelenleg a beolvasott soron fut (a `NameNormalizer`
  ugyanazzal a szabállyal, amit a validátor használ), nem `LIKE`-kal. A viselkedés a
  szerződés szerinti (ékezet- és kisbetű-érzéketlen részszó-egyezés romajira, magyar
  névre és aliasra, teszttel lefedve), de az előre normalizált `search_romaji` /
  `search_hungarian` oszlopok, a `LIKE '%…%'` és az **egyező alias megjelölése** még
  nincs meg. Figyelem: ez sémaváltozás, tehát `version` emelés + explicit `Migration` +
  ADR (DR-08) – `fallbackToDestructiveMigration()` nélkül.
- **A T-402-nek hagyva:** a `QuizCandidateSet.illustrationDigests` **üres térkép**. A
  képek tartalmi ujjlenyomatához nincs oszlop a sémában, becsült ujjlenyomatot pedig nem
  gyártunk. Amíg üres, a `JP_TO_IMAGE` típus nem tud vizuálisan azonos képpárt kizárni –
  ez ma nem gyakorlati hiba (egyetlen illusztráció sincs a tudástárban), de a T-402-nek
  pótolnia kell. A `quizExcluded` / `INCOMPLETE` szűrés **szándékosan nincs** a
  repositoryban: azt kérdéstípusonként a `QuizEligibility` kapu végzi (ADR-010), ahogy a
  `QuizCandidateSet` szerződése is kimondja.
- **Séma nem változott**, migráció nem kellett: csak új `@Query` metódusok kerültek a
  DAO-kba (kategóriaszűrt aktív lista, pontszerű aktív lekérdezések,
  `observeActiveCount()`, az összes alias egy kérdésben – az N+1 elkerülésére, NFR-10).
- **Teszt-tanulság a következő `Flow`-s feladatokhoz:** a `UnconfinedTestDispatcher`-nek
  át kell adni a `runTest` `testScheduler`-ét. Paraméter nélkül saját ütemezőt hoz létre,
  és a `combine`/`flowOn` láncon a Room megfigyelt lekérdezése „Detected use of different
  schedulers" hibával elhasal. Pontszerű, `withContext`-es metódusnál ez nem derül ki.
- **Javasolt új task:** a `TechniqueRepository` publikus API-ját reflexióval vizsgáló
  teszt (`techniqueRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03`) ma csak ezt
  az egy interfészt nézi. A TR-07 rétegszabály-ellenőrzés automatizálása (T-602) ezt
  általánosítsa minden repository interfészre és modulra.
