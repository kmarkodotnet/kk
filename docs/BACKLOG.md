# Backlog – user story-k és fejlesztői taskok

| | |
|---|---|
| Verzió | 1.0 |
| Dátum | 2026-11-11 |
| Alapja | `REQUIREMENTS.md` v1.0, `FUNCTIONAL_SPEC.md` v1.0, `ARCHITECTURE.md` v1.0, ADR-001…ADR-011 |
| Tartalom | 6 epik · 21 user story · 71 dev task |

## Hogyan olvasd

| Jelölés | Mit jelent |
|---|---|
| `US-nn` | user story – **mit akar a felhasználó és miért**, elfogadási kritériumokkal |
| `T-nnn` | dev task – **egy konkrét, befejezhető fejlesztői munkadarab** |
| `FR/NFR/CR/DR/TR/AC-nn` | követelményhivatkozás a `REQUIREMENTS.md`-be |
| Méret | **S** ≈ 1–3 ó · **M** ≈ 3–6 ó · **L** ≈ 6–12 ó (ideális, zavartalan munkaidő) |

**A dokumentumok viszonya:**

| Dokumentum | Mire válaszol |
|---|---|
| `REQUIREMENTS.md` | *mit* kell tudnia a terméknek |
| `FUNCTIONAL_SPEC.md` | *hogyan néz ki és viselkedik* |
| `ARCHITECTURE.md` + `adr/` | *milyen szerkezetben* épül |
| **`BACKLOG.md` (ez)** | *milyen munkadarabokra bontva*, mi a kész definíciója |
| `IMPLEMENTATION_PLAN.md` | *milyen sorrendben*, és **hol tartunk most** |

A tényleges állapotot (kipipálás) az `IMPLEMENTATION_PLAN.md` vezeti – ez a fájl a
munkadarabok **katalógusa és áttekintése**, nem állapotnyilvántartás.

**Minden dev task külön fájlban is megvan**, teendőkkel, „Kész, ha" listával és
task-specifikus Definition of Done-nal:

| Mappa | Mit tartalmaz |
|---|---|
| [`backlog/`](backlog/) | a **nyitott** feladatok, egy fájl = egy task ([index](backlog/README.md)) |
| [`done/`](done/) | az **elkészült** feladatok, lezáró szakasszal (futtatott parancsok, commit) |

A **`/dev`** parancs kiveszi a `backlog/`-ból a soron következő feladatot (növekvő
azonosító szerint, csak teljesült függőséggel), lefejleszti, futtatja a teszteket, és
sikeres Definition of Done esetén átmozgatja a `done/` mappába. A `/kyo-next` a
fázisszintű lépésekhez marad.

---

## Globális Definition of Ready (DoR)

Egy task akkor kezdhető el, ha:

1. Van hozzá US és követelményhivatkozás (nem „jó lenne, ha" alapon indul).
2. A függőségei készen vannak (lásd a task „Függ" mezőjét).
3. Eldőlt, melyik modulba tartozik, és az nem sérti a rétegszabályt (ADR-001, L1–L6).
4. Ha tartalmat érint: eldőlt a hitelesítési státusz kezelése (CR-01, CR-02).

## Globális Definition of Done (DoD)

Egy task akkor kész, ha **mind** teljesül:

1. A kód lefordul: `./gradlew assembleDebug` **exit 0**.
2. Van hozzá automatizált teszt, és `./gradlew testDebugUnitTest` **exit 0** – a teszt
   neve hivatkozik a lefedett `TR-nn` vagy `FR-nn` azonosítóra.
3. Nem sért rétegszabályt: `core/model` és `core/quiz` nem lát Androidot; `feature/*` nem
   lát Room entitást, DAO-t, DTO-t.
4. Nincs benne közvetlen `System.currentTimeMillis()`, `Instant.now()` vagy
   `Random.Default` üzleti kódban – `Clock` és RNG injektálva (NFR-07).
5. Minden felhasználói szöveg `strings.xml`-ben, magyarul.
6. UI task esetén: `contentDescription` megvan, érintési célpont ≥ 48 dp, és van
   `@Preview` világos + sötét témára.
7. Az `IMPLEMENTATION_PLAN.md` állása frissítve, és a commit üzenete hivatkozik a taskra.
8. Ami **nem** futott le, az a `TEST_REPORT.md`-ben „NEM FUTOTT – ok" sorként szerepel.

---

## Epikek

| Epik | Cím | US | Task | Fázis |
|---|---|---|---|---|
| **E1** | Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia | US-01…US-04 | T-101…T-118 | 2–3 |
| **E2** | Tudástár böngészése és keresése | US-05…US-08 | T-201…T-211 | 4 |
| **E3** | Tanulás tanulókártyákkal | US-09…US-11 | T-301…T-308 | 5 |
| **E4** | Kvíz | US-12…US-16 | T-401…T-415 | 6 |
| **E5** | Haladás és beállítások | US-17…US-19 | T-501…T-509 | 7 |
| **E6** | Minőség, akadálymentesség, átadás | US-20…US-21 | T-601…T-610 | 8 |
| **E7** | Vizuális arculat és navigáció (2026-11-13 bővítés) | US-22…US-26 | T-701…T-706, T-712 | — |
| **E8** | Tartalom-munkafolyamat (2026-11-13 bővítés) | US-27…US-28 | T-707…T-708 | — |
| **E9** | Kiejtés, hangalapú kvíz, többnyelvűség – **jövőbeli, jelenleg NEM indítható** | — | T-709…T-711 | — |

> **E7/E8/E9 az eredeti M1…M7 szállítás lezárása (2026-11-13) után felvett igények.**
> Nem tartoznak egyik eredeti mérföldkőhöz sem; lásd a „Bővítési backlog" szakaszt lent.

---

# E1 · Fundamentum

> Ez az epik nem ad közvetlen felhasználói funkciót, de **minden más epik ezen áll**.
> A termék helyességének magja (kvízmotor, validátor, előrehaladás-megőrzés) itt készül el,
> tiszta Kotlinban, emulátor nélkül tesztelhetően.

## US-01 · A tartalom hibátlanul és ellenőrzötten kerül az appba

> **Tartalomfejlesztőként** azt akarom, hogy a tudástár betöltésekor minden rekord
> ellenőrzésen menjen át, **hogy** hibás adat ne jusson el a felhasználóig, és lássam, mi a baj.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | egy rekordból hiányzik a `hungarian_name` | betöltés fut | a rekord `INCOMPLETE`, kvízből kizárt, az app **nem** omlik össze |
| 2 | két rekordnak azonos az `id`-ja | betöltés fut | a későbbi elvetve, a jelentés `V003` kóddal jelzi |
| 3 | a `schema_version` ismeretlen major verzió | betöltés fut | a betöltés elutasítva, diagnosztika készül, nincs kivétel |
| 4 | egy `illustration_ref` nem létező fájlra mutat | betöltés fut | a technika megmarad, de `Placeholder` képpel, és kiesik a képes kvízből |
| 5 | két rekord normalizált neve azonos | betöltés fut | kölcsönös distraktor-kizárási csoportba kerülnek |

**Követelmények:** DR-05, FR-31, FR-32, CR-07 · **Teszt:** TR-01

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-101** | `core/model`: value class ID-k (`TechniqueId`, `CategoryId`, `SessionId`, `QuestionId`), enumok (`QuestionType`, `SelfRating`, `VerificationStatus`, `DatasetTrust`) | core/model | S | — |
| **T-102** | `core/model`: `Technique`, `Category`, `IllustrationRef` sealed (ADR-003) | core/model | S | T-101 |
| **T-103** | `core/model`: felhasználói adatmodellek (`QuizAttempt`, `StudyReview`, `QuizSession`) | core/model | S | T-101 |
| **T-104** | `core/quiz`: validátor szerződése – `KnowledgeValidator`, `ValidationReport`, `ValidationIssue`, `IssueCode` (V001…V010) | core/quiz | M | T-102 |
| **T-105** | `core/quiz`: validátor implementáció – kötelező mezők, egyedi ID, kategória, képhivatkozás, szinonimacsoportok | core/quiz | L | T-104 |
| **T-106** | **TR-01 tesztek**: táblázatos tesztkészlet minden `V` kódra + „érvénytelen tartalom nem dob kivételt" | core/quiz | M | T-105 |

---

## US-02 · A kvíz mindig egyértelmű, helyes kérdést ad

> **Gyakorlóként** azt akarom, hogy minden kvízkérdésnek pontosan egy helyes válasza legyen,
> **hogy** ne tanuljak meg rosszat, és ne veszítsek pontot hibás kérdésen.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | elegendő jelölt | kérdés generálódik | pontosan 4 opció, pontosan 1 helyes, 3 **különböző** téves |
| 2 | a helyesnek van szinonimája a jelöltek közt | kérdés generálódik | a szinonima **nem** lesz distraktor |
| 3 | egy kategóriában 3-nál kevesebb elfogadható distraktor | kérdés generálódna | a kérdés **nem jön létre**; a motor nem gyárt opciót |
| 4 | ugyanaz a `QuizRequest` és seed | kétszer generálunk | bitre azonos terv, az opciók sorrendjével együtt |
| 5 | 20 kérdést kérek, de csak 12 jelölt van | generálás fut | 12 kérdés jön, `shortfall` okkal; nincs ismétlés |

**Követelmények:** FR-14, FR-15, FR-22, NFR-07, CR-07 · **Teszt:** TR-02

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-107** | `core/quiz`: generátor szerződése – `QuizRequest`, `QuizPlan`, `QuizQuestion` (csak ID-k, ADR-006), `SkipReason`, `Shortfall` | core/quiz | M | T-102 |
| **T-108** | `core/quiz`: jelöltszűrés és típusjogosultsági kapuk – `quizEligible` formula (ADR-010), `quiz_usable` képkapu (ADR-011) | core/quiz | M | T-107 |
| **T-109** | `core/quiz`: distraktorválasztás – kategórián belül → kategórián kívül → kérdés kihagyása; szinonima- és képazonosság-kizárás | core/quiz | L | T-108 |
| **T-110** | `core/quiz`: seedelt, determinisztikus keverés (Fisher–Yates), rendezett bejárás mindenhol | core/quiz | M | T-109 |
| **T-111** | `core/quiz`: `dryRun()` – készenléti összegzés a kvízbeállító képernyőnek | core/quiz | S | T-110 |
| **T-112** | **TR-02 + TR-03 tesztek**: invariánsok, seed-determinizmus, fallback, kategóriaszűrés, típusjogosultság | core/quiz | L | T-111 |

---

## US-03 · Az eredményem őszintén tükrözi, amit tényleg megválaszoltam

> **Gyakorlóként** azt akarom, hogy a százalék csak a ténylegesen megválaszolt kérdésekből
> számolódjon, **hogy** egy félbehagyott kvíz ne rontsa el a statisztikámat.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | 0 megválaszolt kérdés | eredményt számolunk | nincs százalék (`null`), a UI „nincs adat"-ot mutat, **nem 0%-ot** |
| 2 | 10-ből 4-et válaszoltam meg, 3 helyes | eredményt számolunk | 3/4 = 75%, a meg nem válaszolt 6 **nem** számít hibásnak |
| 3 | minden válasz helyes / egyik sem | eredményt számolunk | 100% / 0% – mindkettő érvényes határeset |
| 4 | ugyanarra a kérdésre kétszer válaszolok | eredményt számolunk | az első válasz számít, az eredmény nem duplázódik |

**Követelmények:** FR-18, FR-20, FR-27, DR-09 · **Teszt:** TR-04

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-113** | `core/quiz`: `QuizScoring` – tiszta függvény, **nullable** arány (`ratio: Double?`) | core/quiz | S | T-103 |
| **T-114** | **TR-04 tesztek**: 0%, 100%, félbehagyott, dupla válasz, nulla válasz | core/quiz | M | T-113 |

---

## US-04 · A haladásom túléli a tudástár frissítését

> **Gyakorlóként** azt akarom, hogy egy tartalomfrissítés után is megmaradjon minden korábbi
> eredményem, **hogy** ne kelljen elölről kezdenem, ha új technikák kerülnek az appba.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | 50 rögzített próbálkozásom van | új `dataset_version` töltődik be | mind az 50 megmarad, a statisztika változatlan |
| 2 | egy technikát átneveznek (az `id` marad) | frissítés után megnézem | az előrehaladás követi az `id`-t, az új név látszik |
| 3 | egy technika kikerül a tananyagból | frissítés fut | `retired = true`, **nem** törlődik; a próbálkozásai értelmesek maradnak |
| 4 | ugyanazt a `dataset_version`-t kétszer töltöm | második betöltés | nincs duplikáció (idempotens upsert) |

**Követelmények:** FR-29, FR-30, FR-33, DR-06, DR-07, DR-08, AC-08 · **Teszt:** TR-05

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-115** | `core/data`: a kanonikus séma DTO-i (`ignoreUnknownKeys`), `manifest.json` + `entries.json` olvasása (ADR-011) | core/data | M | T-105 |
| **T-116** | `core/data`: Room séma – tartalom- és felhasználói táblák, `@TypeConverter`-ek, **`UNIQUE(sessionId, questionId)`** (ADR-005), nincs cascade (ADR-004) | core/data | L | T-103 |
| **T-117** | `core/data`: `ContentImporter` – DTO → validátor → upsert, idempotens, `retired` jelölés, `content_state` tárolás | core/data | L | T-115, T-116 |
| **T-118** | **TR-05 tesztek** (in-memory Room): előrehaladás megőrzése, átnevezés, visszavonás, idempotencia | core/data | L | T-117 |

---

# E2 · Tudástár

## US-05 · Böngészem a technikákat kategóriák szerint

> **Gyakorlóként** kategóriákba rendezve akarom látni a technikákat, **hogy** átlássam a
> tananyagot és tudjam, mi tartozik egybe.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | betöltött tudástár | megnyitom a Tudástárat | a 9 kategória a manifest `sort_order` szerint, azon belül romaji ábécérendben |
| 2 | kiválasztok 2 kategóriachipet | a lista frissül | csak azok a technikák látszanak; a chip mutatja a találatszámot |
| 3 | elforgatom a képernyőt | a lista újraépül | a szűrés és a görgetési pozíció megmarad |
| 4 | a kategórianevek | megjelennek | a **tartalomból** jönnek, nincsenek kódba égetve |

**Követelmények:** FR-01, FR-02 · **Teszt:** TR-06 (UI)

## US-06 · Rákeresek egy technikára, ahogy eszembe jut

> **Gyakorlóként** romaji vagy magyar név szerint akarok keresni, ékezetek nélkül is,
> **hogy** gyorsan megtaláljam, amit az edzésen hallottam.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | „zart labu" beírása (ékezet nélkül) | keresés fut | megtalálja a „Zárt lábú állás"-t |
| 2 | „DACHI" beírása (nagybetűvel) | keresés fut | megtalálja az összes `… dachi` technikát |
| 3 | alias alapján van találat | a tétel megjelenik | a tétel alatt látszik: „Egyezés: `{alias}`" |
| 4 | nincs találat | a lista üres lenne | konkrét üzenet a keresett kifejezéssel + „Szűrők törlése" gomb |

**Követelmények:** FR-03 · **Teszt:** TR-06

## US-07 · Megnézem egy technika részleteit

> **Gyakorlóként** nagyobb ábrával és magyarázattal akarom látni a technikát, **hogy**
> megértsem a mozdulatot, ne csak a nevét tanuljam meg.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | megnyitok egy tételt | a részletező megjelenik | nagy ábra, romaji, magyar név, kategória, magyarázat |
| 2 | a technikának nincs képe | a részletező megjelenik | semleges helyőrző + „Ehhez a technikához még nincs ábra" |
| 3 | egy opcionális mező üres | a részletező megjelenik | a szakasz **nem** jelenik meg (nincs „nincs adat" zaj) |
| 4 | már gyakoroltam a technikát | a részletező megjelenik | kis összegzés + link a haladás-részletezőre |

**Követelmények:** FR-04 · **Teszt:** TR-06

## US-08 · Látom, mi nincs még szakmailag ellenőrizve

> **Gyakorlóként** tudni akarom, ha egy adat lektorálásra vár, **hogy** ne tanuljak meg
> valamit tévesen hitelesként.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | `editorial_status: draft_review_required` | bármelyik képernyőt megnyitom | nem elutasítható szalag: „Lektorálatlan gyakorlóanyag…" |
| 2 | `review_status: editorial_review_required` rekord | a listában megjelenik | „Ellenőrzésre vár" jelölő |
| 3 | `INCOMPLETE` rekord | a részletezőben megjelenik | „Hiányos rekord" + a hiányzó mezők + kvízkizárás jelzése |
| 4 | AI-generált, nem ellenőrzött kép | megjelenik | a kép mellett látszik, hogy szakmai ellenőrzésre vár |

**Követelmények:** FR-05, CR-01, CR-03, CR-05, ADR-010 · **Teszt:** TR-06

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-201** | `core/data`: `TechniqueRepository` interfész + implementáció (`internal`), mapperek | core/data | M | T-117 |
| **T-202** | `core/data`: keresés – normalizált illeszkedés romajira, magyar névre, aliasokra; alias-találat jelzése | core/data | M | T-201 |
| **T-203** | `di`: Hilt modulok – `DatabaseModule`, `RepositoryModule`, `QuizModule`, `TimeModule` (`Clock`), `DispatcherModule` | di | M | T-201 |
| **T-204** | `core/ui`: `TechniqueIllustration` – `Asset`/`Placeholder`/hiányzó eset, helyes `contentDescription` | core/ui | M | T-102 |
| **T-205** | `core/ui`: `VerificationBadge`, `UnreviewedBanner`, `EmptyState`, `ErrorState` | core/ui | M | — |
| **T-206** | `navigation`: útvonalak, `NavHost`, bottom nav 4 céllal; a kvízfutás alatti elrejtés | navigation | M | T-205 |
| **T-207** | `feature/library`: lista ViewModel – `UiState` (Loading/Empty(ok)/Content/Error), keresés debounce, `SavedStateHandle` | feature/library | L | T-202, T-203 |
| **T-208** | `feature/library`: lista UI – keresősáv, kategóriachipek találatszámmal, listatételek | feature/library | L | T-207, T-204 |
| **T-209** | `feature/library`: részletező ViewModel + UI, hitelesítési blokk, haladás-összegzés | feature/library | L | T-207 |
| **T-210** | Üres és hibaállapotok a `FUNCTIONAL_SPEC` 2.4 és 10. pontja szerint | feature/library | M | T-208 |
| **T-211** | ViewModel tesztek: keresés normalizálás, szűrés, állapotmegőrzés | feature/library | M | T-207 |

---

# E3 · Tanulás

## US-09 · Tanulókártyákkal gyakorlok

> **Gyakorlóként** lapozható kártyákon akarom átnézni a technikákat, **hogy** saját tempóban
> ismételjek.

**Elfogadási kritériumok:** kategóriaválasztás után kártyák; előző/következő működik;
a kártyapozíció és a sorrend túléli a forgatást; a sorrend seedelt keverés.
**Követelmények:** FR-06, FR-09

## US-10 · Elrejtem a választ, és magam idézem fel

> **Gyakorlóként** el akarom rejteni a japán vagy a magyar nevet, **hogy** tényleg
> felidézzem, ne csak felismerjem.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | „japán nevet rejtse el" beállítás | kártya megjelenik | a romaji rejtve, a magyar név és az ábra látszik |
| 2 | koppintok a felfedésre | a kártya felfedi | **mindhárom** látszik: romaji + magyar név + ábra |
| 3 | a kép felirata felfedné a nevet (`hideable_on_study_card: false`) | kártya megjelenik | a kép **csak felfedés után** jelenik meg |
| 4 | visszalépek egy már felfedett kártyára | megjelenik | felfedve marad |

**Követelmények:** FR-07, FR-08, ADR-011

## US-11 · Jelölöm, mit tudtam és mit kell gyakorolnom

> **Gyakorlóként** önértékelést akarok adni, **hogy** lássam, mire kell visszatérnem – de
> **hogy** ez ne keveredjen a kvízeredményeimmel.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | felfedett kártya | „Tudtam"-ra koppintok | `StudyReview` mentve, automatikus továbblépés |
| 2 | fel nem fedett kártya | az önértékelő gombokra nézek | letiltva, és a letiltás oka bejelentve TalkBackkel |
| 3 | adtam önértékelést | megnyitom a Haladást | külön blokkban látszik, a kvízsikerarányt **nem** befolyásolja |

**Követelmények:** FR-10, FR-28

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-301** | `core/data`: `ProgressRepository` interfész – `recordStudyReview`, olvasó `Flow`-ok | core/data | M | T-116 |
| **T-302** | `feature/learn`: `learn/setup` ViewModel + UI – kategóriák, elrejtési mód, utolsó választás megjegyzése | feature/learn | M | T-203, T-206 |
| **T-303** | `feature/learn`: kártyaállapot – seedelt sorrend, pozíció és felfedettség `SavedStateHandle`-ben | feature/learn | M | T-302 |
| **T-304** | `feature/learn`: kártya UI – elrejtés/felfedés, előző/következő | feature/learn | L | T-303, T-204 |
| **T-305** | `feature/learn`: feliratos kép elrejtése felfedésig (`hideable_on_study_card`) | feature/learn | S | T-304 |
| **T-306** | `feature/learn`: önértékelés → `StudyReview`, automatikus továbblépés | feature/learn | M | T-301, T-304 |
| **T-307** | `feature/learn`: záróképernyő – újrakezdés / beállítások / kvíz indítása | feature/learn | S | T-304 |
| **T-308** | ViewModel tesztek: sorrend determinizmus, felfedési állapot, önértékelés rögzítése | feature/learn | M | T-306 |

---

# E4 · Kvíz

## US-12 · Beállítom a kvízt, és előre látom, mi várható

> **Gyakorlóként** ki akarom választani a kategóriákat, a kérdésszámot és a kérdéstípusokat,
> **hogy** azt gyakoroljam, amire szükségem van – és **hogy** indulás előtt lássam, összejön-e.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | kiválasztok kategóriákat | a képernyő frissül | kiírja: „A beállításokkal `{n}` kérdés állítható össze" |
| 2 | 20-at kérek, de csak 12 jön ki | a képernyő frissül | „A kért 20 helyett 12 kérdés lesz, mert nincs több megfelelő technika" |
| 3 | nincs 4 egyedi, felirat nélküli kép a kiválasztásban | a típusválasztót nézem | a (C) és (D) **letiltva**, az indoklás kiírva a jelenlegi darabszámmal |
| 4 | 0 kérdés állítható össze | az indító gombot nézem | letiltva + 3 konkrét javaslat (több kategória / több típus / kevesebb kérdés) |

**Követelmények:** FR-11, FR-12, FR-13, FR-22 · **Teszt:** TR-03

## US-13 · Egy kérdést egyszer válaszolok meg

> **Gyakorlóként** azt akarom, hogy egy véletlen dupla koppintás ne számítson kétszer,
> **hogy** az eredményem pontos legyen.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | megválaszolt kérdés | újra koppintok egy opcióra | semmi nem változik, nincs új próbálkozás |
| 2 | gyors dupla koppintás | a válasz feldolgozódik | **egy** `QuizAttempt` keletkezik (DB egyedi index is véd) |
| 3 | a repository `recordAttempt`-je | másodszor hívódik | `false`-t ad vissza, nem ír |

**Követelmények:** FR-18, ADR-005 · **Teszt:** TR-04

## US-14 · Megértem, miért volt rossz a válaszom

> **Gyakorlóként** válasz után azonnal látni akarom a helyes megoldást, **hogy** ott
> helyben tanuljak belőle.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | rosszul válaszolok | a visszajelzés megjelenik | ikon + „Helytelen" **szöveg** + szín (nem csak szín) |
| 2 | bármilyen válasz után | a magyarázó kártya megjelenik | a helyes technika romaji + magyar név + ábra hármasa |
| 3 | a magyarázó kártyán | a technikára koppintok | a Tudástár részletezője nyílik meg |

**Követelmények:** FR-16, FR-17, NFR-04

## US-15 · A megkezdett kvízem nem vész el

> **Gyakorlóként** azt akarom, hogy forgatás vagy megszakítás ne törölje a válaszaimat,
> **hogy** nyugodtan használhassam menet közben.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | a 3. kérdésnél tartok | elforgatom a képernyőt | ugyanaz a kérdés, **ugyanaz az opciósorrend**, megőrzött válaszok |
| 2 | megszakítom a kvízt | megerősítést kapok | „Az eddigi válaszaid megmaradnak"; a munkamenet `finishedAt = null` |
| 3 | megszakított kvíz | megnézem a Haladást | a megválaszolt kérdések beszámítanak |

**Követelmények:** FR-19, NFR-07, ADR-007

## US-16 · Újragyakorolom, amit elrontottam

> **Gyakorlóként** a hibás kérdéseimet akarom újra megkapni, **hogy** célzottan javítsak –
> de **hogy** ez ne duplázza a statisztikámat.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | 4 hibás válasz | „Hibák újragyakorlása" | **új** munkamenet indul, **új** seeddel, ugyanazokra a technikákra |
| 2 | az új munkamenet | lefut | a régi munkamenet eredménye változatlan, nincs kettős könyvelés |
| 3 | nincs hibás válasz | az eredményképernyőt nézem | a gomb nem jelenik meg |

**Követelmények:** FR-20, FR-21

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-401** | `core/data`: `ProgressRepository` kvízrésze – `startSession`, `recordAttempt(): Boolean`, `finishSession` | core/data | M | T-116 |
| **T-402** | `core/data`: kvízjelöltek lekérdezése (`getQuizCandidates`) a szinonimacsoportokkal és képkapukkal | core/data | M | T-201 |
| **T-403** | `feature/quiz`: `quiz/setup` ViewModel – élő készenléti összegzés `dryRun()`-nal | feature/quiz | L | T-111, T-402 |
| **T-404** | `feature/quiz`: `quiz/setup` UI – kategóriák, 5/10/20, típusválasztó letiltással és indoklással | feature/quiz | L | T-403 |
| **T-405** | `feature/quiz`: `QuizRunState` – seed, kérdések, index, válaszok `SavedStateHandle`-ben (ADR-007) | feature/quiz | M | T-403 |
| **T-406** | `feature/quiz`: egyszeri válasz kikényszerítése a ViewModelben + a repository `false` válaszának kezelése | feature/quiz | M | T-405, T-401 |
| **T-407** | `feature/quiz`: (A) és (B) típusú kérdés UI – szöveges opciók | feature/quiz | M | T-405 |
| **T-408** | `feature/quiz`: (C) és (D) típusú kérdés UI – kép a kérdésben, 2×2 képrács opcióként | feature/quiz | L | T-407, T-204 |
| **T-409** | `feature/quiz`: válasz-visszajelzés – ikon + szöveg + szín, magyarázó kártya linkkel | feature/quiz | M | T-407 |
| **T-410** | `feature/quiz`: előrehaladásjelző, megszakítás megerősítéssel, vissza-gomb kezelés | feature/quiz | M | T-405 |
| **T-411** | `feature/quiz`: `quiz/result` – összegzés, „nincs adat" eset, hibalista | feature/quiz | L | T-113, T-410 |
| **T-412** | `feature/quiz`: „Hibák újragyakorlása" – új munkamenet, új seed | feature/quiz | M | T-411 |
| **T-413** | `feature/quiz`: akadálymentesség – `selected`/`stateDescription`, a (D) opciók leírása **nem** fedi fel a nevet | feature/quiz | M | T-408, T-409 |
| **T-414** | ViewModel tesztek: dupla válasz, forgatás, megszakítás, shortfall megjelenítés | feature/quiz | L | T-412 |
| **T-415** | A lektorálatlan tartalom jelzése a kvízindításnál és az eredményen (ADR-010) | feature/quiz | S | T-404, T-411 |

---

# E5 · Haladás és beállítások

## US-17 · Látom, hol tartok

> **Gyakorlóként** összesített és kategóriánkénti képet akarok a teljesítményemről, **hogy**
> tudjam, mire koncentráljak.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | nincs egyetlen próbálkozásom sem | megnyitom a Haladást | „Még nincs adat. Indíts egy kvízt!" + gomb, **nem 0%** |
| 2 | van adatom | megnyitom a Haladást | összes megválaszolt, helyes, sikerarány |
| 3 | egy kategóriában nincs próbálkozás | a bontást nézem | „nincs adat", nem 0% |
| 4 | a statisztika | megjelenik | a `quiz_attempt` táblából **számolva**, nem külön számlálóból |

**Követelmények:** FR-23, FR-24, FR-27, DR-09

## US-18 · Megtalálom, mit kell gyakorolnom

> **Gyakorlóként** listát akarok a gyenge pontjaimról, **hogy** ne találgassak.

**Elfogadási kritériumok:** legfeljebb 10 technika, sikerarány növekvő, majd a legrégebben
gyakorolt szerint; csak legalább 1 próbálkozással rendelkező technika kerül bele;
technikánként látszik a sikerarány, a kérdéstípus-bontás és az utolsó gyakorlás ideje;
visszavont technikánál külön megjegyzés.
**Követelmények:** FR-25, FR-26, DR-07

## US-19 · Kezelem a saját adataimat

> **Felhasználóként** látni akarom, milyen tartalom van betöltve, és törölni tudjam a
> haladásomat, **hogy** kontrollom legyen a saját adataim felett.

**Elfogadási kritériumok:** a beállításokban látszik a `schema_version`, `dataset_version`,
`editorial_status` és a rekordszám; az „Előrehaladás törlése" kétlépcsős megerősítést kér,
és pontosan felsorolja, mi törlődik; debug buildben elérhető a teljes validációs jelentés.
**Követelmények:** FR-32, FR-35, NFR-09

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-501** | `core/data`: aggregált statisztika `@Query`-k – összesített, kategóriánkénti, technikánkénti, „gyakorlást igénylő" | core/data | L | T-401 |
| **T-502** | `core/data`: `clearAllProgress()` – csak a felhasználói táblák, a tartalom érintetlen | core/data | S | T-401 |
| **T-503** | `feature/progress`: áttekintő ViewModel + UI – összesítés, kategóriabontás, „nincs adat" esetek | feature/progress | L | T-501, T-206 |
| **T-504** | `feature/progress`: „Gyakorlást igénylő" lista a megadott rendezéssel | feature/progress | M | T-503 |
| **T-505** | `feature/progress`: önértékelési blokk elkülönítve + magyarázó szöveg (FR-28) | feature/progress | S | T-503 |
| **T-506** | `feature/progress`: technika-részletező – kérdéstípus-bontás, visszavont jelzés | feature/progress | M | T-503 |
| **T-507** | `feature/progress`: beállítások – tartalominfó, előrehaladás törlése megerősítéssel | feature/progress | M | T-502, T-503 |
| **T-508** | `feature/progress`: debug diagnosztika – a teljes `ValidationReport` megjelenítése | feature/progress | M | T-117, T-507 |
| **T-509** | ViewModel tesztek: „nincs adat" esetek, rendezés, törlés hatása | feature/progress | M | T-507 |

---

# E6 · Minőség, akadálymentesség, átadás

## US-20 · Mindenki tudja használni az appot

> **TalkBacket használó gyakorlóként** azt akarom, hogy az app felolvasható és
> megfelelő kontrasztú legyen, **hogy** én is tudjak vele tanulni.

**Elfogadási kritériumok:** minden illusztrációnak értelmes magyar leírása van, kivéve ahol
az felfedné a választ; a kvízopciók `selected` szemantikával és válasz utáni
`stateDescription`-nel; minden érintési célpont ≥ 48 dp; WCAG AA kontraszt világos és sötét
témában; nagy rendszerbetűméretnél sem törik a layout.
**Követelmények:** NFR-04, NFR-05

## US-21 · Az app átvehető és továbbfejleszthető

> **Fejlesztőként** futtatható projektet, hiteles tesztjelentést és pontos dokumentációt
> akarok, **hogy** át tudjam venni a munkát.

**Elfogadási kritériumok:** `assembleDebug` és `testDebugUnitTest` zöld; a `README.md`
leírja az előfeltételeket, a futtatást és az ismert korlátokat; a `TEST_REPORT.md` csak
tényleges futtatásokat tartalmaz; a `docs/` és a kód nem tér el (`/kyo-doc-sync`);
új technika hozzáadása nem igényel kódmódosítást (AC-08).
**Követelmények:** NFR-11, NFR-12, AC-01…AC-08

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-601** | A `knowledgebase_transfer/` bemásolása `app/src/main/assets/knowledge/` alá + build-ellenőrzés | app | S | T-117 |
| **T-602** | **TR-07**: rétegszabály-ellenőrzés automatizálva (tiltott importok tesztje vagy lint szabály) | core/quiz | M | T-110 |
| **T-603** | **TR-08**: manifest-teszt – nincs `android.permission.INTERNET` (az AndroidX saját receiver-permissionje megengedett) | app | S | — |
| **T-604** | Akadálymentességi átfésülés: leírások, szemantika, érintési célpontok, kontraszt | összes feature | L | T-413, T-503 |
| **T-605** | Nagy betűméret és forgatás ellenőrzése a 4 fő képernyőn | összes feature | M | T-604 |
| **T-606** | AVD létrehozása és **TR-06** UI tesztek: navigáció, tanulókártya, teljes kvíz | app | L | T-412, T-503 |
| **T-607** | Lint + ktlint bekötése és a találatok javítása | gyökér | M | — |
| **T-608** | `docs/KNOWLEDGE_BASE.md` kiadott változata (ADR-011 szerint) | docs | M | T-117 |
| **T-609** | `docs/QUIZ_RULES.md` kiadott változata a tényleges motorhoz mérve | docs | M | T-112 |
| **T-610** | `README.md` + `TEST_REPORT.md` lezárása, debug APK átadása, AC-01…AC-08 végigellenőrzése | docs | M | T-606 |

---

## Szállítási mérföldkövek

| # | Mérföldkő | Mi működik a végén | Taskok |
|---|---|---|---|
| **M1** | A mag igazoltan helyes | validátor + kvízmotor + scoring, **zöld TR-01…TR-04** | T-101…T-114 |
| **M2** | A tartalom bent van | 161 rekord Room-ban, import idempotens, **zöld TR-05** | T-115…T-118, T-601 |
| **M3** | Böngészhető app | Tudástár lista, keresés, részletező, navigáció, jelölések | T-201…T-211 |
| **M4** | Tanulható app | tanulókártyák, önértékelés | T-301…T-308 |
| **M5** | Kvízelhető app | teljes kvízfolyamat eredménnyel és újragyakorlással | T-401…T-415 |
| **M6** | Mérhető app | haladás, beállítások, adattörlés | T-501…T-509 |
| **M7** | Átadható app | akadálymentesség, UI tesztek, lint, dokumentáció, APK | T-601…T-610 |

**M1 a legfontosabb mérföldkő:** itt dől el, hogy a termék helyes-e. Emulátor nélkül,
sima JVM tesztekkel teljesen igazolható.

---

## Kockázatok és nyitott pontok

| # | Pont | Hatás | Kezelés |
|---|---|---|---|
| R-01 | A képek szakmailag nem ellenőrzöttek, 3-on rajta a név | a (C)/(D) kvíztípus csak 7 képpel, egyetlen kategóriában | elfogadott állapot; a felhasználó később javítja, a konverter újrafuttatása elég (`quiz_usable` automatikusan frissül) |
| R-02 | Minden rekord lektorálatlan | a kvíz végig „gyakorlóanyag" jelöléssel fut | ADR-010; a jelölés eltűnik, amint `reviewed` tartalom érkezik – kódmódosítás nélkül |
| R-03 | Nincs emulátor (B-03) | TR-06 UI tesztek nem futnak | T-606 első lépése az AVD létrehozása; addig a UI tesztek „NEM FUTOTT" státuszúak |
| R-04 | 8 kategóriában nincs kép | a képes kvíztípus ott nem indul | a beállító képernyő ezt megindokolja (T-404); nem hiba |
| R-05 | `aliases` és `tags` mindenhol üres | az alias-keresés élesben nem kap tesztadatot | a logika és a tesztje elkészül szintetikus adattal (T-202, T-211) |

---

# Bővítési backlog (2026-11-13) – vizuális arculat, navigáció, tartalom

> Az M1…M7 szállítás (lásd fent) lezárult. Az itt következő E7…E9 a felhasználó
> 2026-11-13-i design- és funkcióvisszajelzése alapján felvett **új** igénysor – külön
> munkamenet, amit a `/dev`/`/devall` az eredeti taskokhoz hasonlóan, de **a saját
> jogán** vehet fel (nincs eredeti mérföldkőhöz rendelve). `FR-36…FR-42` és
> `OQ-05…OQ-07`: lásd `REQUIREMENTS.md` 3.6. és 10. szakasz.

# E7 · Vizuális arculat és navigáció

## US-22 · Az indítás Kyokushin-karakterű, nem „webes űrlap" hatású

> **Felhasználóként** azt akarom, hogy az app induláskor és általános megjelenésében
> karate-iskolára emlékeztessen, ne egy generikus webes űrlapra, **hogy** a márka és a
> tanulási élmény összeillő legyen.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | hidegindítás | az app elindul | rövid indítóképernyő: kék kandzsi logó fehér háttéren, utána a Kezdőoldal |
| 2 | az indítóképernyő | megjelenik | nincs harmadik féltől származó kép; a logó a felhasználó saját `asserts/kyo.png` fotójából vektorizált forrásból épül |
| 3 | bármelyik képernyő | megnyitom | a tipográfia, a térközök és a színek nem hatnak „alapértelmezett Material form"-nak – lásd T-702 tervjavaslatot |

**Követelmények:** FR-36 (új, 2026-11-13)

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-701** | Indítóképernyő – vektorizált kandzsi logó (`asserts/kyo_kanji.svg` → Android vector drawable, már elkészítve: `asserts/ic_kyo_kanji.xml`) fehér háttéren, Android 12+ `SplashScreen` API-val és régebbi `minSdk`-ra kézi Compose fallbackkel | app | M | — |
| **T-702** | Vizuális arculat terv – dizájnjavaslat-dokumentum (tipográfia, térköz, szín, komponensstílus) a „webes űrlap" hatás megszüntetésére; a `docs/`-ba kerül, a tényleges Compose-átfestés külön taskokban követi | docs, core/ui | M | T-701 |

## US-23 · Van egy üres Kezdőoldal, alul a menüvel

> **Felhasználóként** azt akarom, hogy induláskor egy nyugodt, a logót középpontba
> állító Kezdőoldalt lássak, alul a navigációval, **hogy** legyen egy semleges
> kiindulópontom a négy funkció felé.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | az indítóképernyő eltűnik | a Kezdőoldal megjelenik | középen a kék kandzsi logó, alatta az alsó navigáció látszik |
| 2 | az alsó navigáció | megjelenik | a bal szélén egy „Főoldal" gomb van, ami minden más képernyőről ide vezet vissza |
| 3 | elforgatom a képernyőt a Kezdőoldalon | a képernyő újraépül | ugyanaz a tartalom, nincs véletlen navigáció |

**Követelmények:** FR-37 (új, 2026-11-13)

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-703** | `feature/home` (új modul) + navigáció – Kezdőoldal képernyő a logóval, az alsó navigáció bal szélén „Főoldal" bejárattal, induló útvonal átállítása a Tudástár listájáról a Kezdőoldalra | navigation, feature/home | L | T-701 |

## US-24 · A Tudástár a legjobboldali menüpont

> **Felhasználóként** azt akarom, hogy a napi szintű, gyakrabban használt funkciók
> (Tanulás, Kvíz, Haladás) kerüljenek előrébb, a Tudástár pedig a sor végére, **hogy**
> a megszokott sorrend a tényleges használati gyakoriságot tükrözze.

**Elfogadási kritériumok:** az alsó navigáció sorrendje Főoldal, Tanulás, Kvíz, Haladás,
Tudástár; a Tudástár részletezőjéből visszalépve a Tudástár fül marad kijelölve (a
meglévő `NavSection` logika szerint, FUNCTIONAL_SPEC szerint változatlanul).
**Követelmények:** FR-38 (új, 2026-11-13)

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-704** | `BottomNavDestination` sorrend átrendezése (Főoldal, Tanulás, Kvíz, Haladás, Tudástár) + a navigációs teszt (`NavigationGraphTest`) és a `kyo-spec-auditor` ellenőrzés frissítése | navigation | S | T-703 |

## US-25 · Kategóriaikonok a Tanulásban, nem checkbox

> **Gyakorlóként** azt akarom, hogy a Tanulás kategóriaválasztása vizuálisan is
> felismerhető ikonokkal történjen, és egy kategóriára koppintva egyből a hozzá tartozó
> technikák listáját lássam, **hogy** gyorsabban rátaláljak arra, amit gyakorolni akarok,
> checkbox-tömb böngészése nélkül.

**Elfogadási kritériumok**

| # | Adott | Amikor | Akkor |
|---|---|---|---|
| 1 | a Tanulás beállító képernyője | megnyílik | minden kategória egy ikonnal ellátott kártya, **nem** checkbox |
| 2 | egy kategóriakártyára koppintok | a nézet vált | a kategória technikái egyenként, böngészhető listában jelennek meg |
| 3 | egy kategóriának nincs dedikált ikonja | a kártya megjelenik | egy semleges, kategória-agnosztikus helyőrző ikon látszik (nincs kitalált szimbólum) |
| 4 | a listáról indítom a tanulást | a kártyák megjelennek | ugyanaz a meglévő felfedési/önértékelési folyamat (US-10, US-11) fut |

**Követelmények:** FR-39 (új, 2026-11-13)

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-705** | `feature/learn` beállító képernyő átalakítása – kategória-ikon-kártyák (`core/ui` ikonkészlet kategóriánként, helyőrzővel a hiányzóra) és a kategórián belüli technikalista drill-down, a meglévő `learn/setup` ViewModel kategória-kiválasztási állapotára építve | feature/learn, core/ui | L | — |

## US-26 · Japán/Magyar kiinduló oldal fülváltással a tanulókártyán

> **Gyakorlóként** azt akarom, hogy a tanulókártya tetején egy fülváltóval gyorsan
> eldönthessem, a japán vagy a magyar nevet lássam-e elsőként, **hogy** mindkét irányban
> gyakorolhassak anélkül, hogy vissza kellene mennem a beállításokhoz.

**Elfogadási kritériumok:** a kártya felett két fül – „Japán nevek" / „Magyar nevek";
fülváltás azonnal, az aktuális kártyapozíció megtartásával, újra elrejtett állapotba
állítja a kártyát; a választás túléli a forgatást; a meglévő FR-07 elrejtés/felfedés
logika változatlan, csak a beállítás helye kerül a fülekre.
**Követelmények:** FR-40 (új, 2026-11-13)

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-706** | `feature/learn` kártya képernyő – felső `TabRow` („Japán nevek" / „Magyar nevek") az eddigi elrejtési-irány beállítás felváltására, `SavedStateHandle`-ben tárolt fülállapottal | feature/learn | M | T-705 |
| **T-712** | `core/ui`: egyedi `Typography`/`Shapes` bevezetése a `docs/DESIGN_NOTES.md` (T-702) döntései szerint – `primary` szín a splash-logóból, megnevezett `Shapes` a ma nyolcszor duplikált `RoundedCornerShape(12.dp)` helyett, romaji/magyar tipográfiai hangsúly | core/ui, feature/learn, feature/quiz, feature/progress | M | T-702 |

---

# E8 · Tartalom-munkafolyamat

## US-27 · Utólagos illusztrációk befogadása

> **Tartalomfejlesztőként** (a felhasználó szerepében) azt akarom, hogy legyen egy
> dokumentált, ismételhető módja annak, hogy később általam átadott képeket az app
> ténylegesen felhasználjon, **hogy** ne kelljen minden egyes képnél kézzel kitalálni a
> folyamatot, és a validáció/`quizEligible` logika biztosan lefusson rájuk.

**Elfogadási kritériumok:** van egy lépésről lépésre leírt munkafolyamat (hova kerül a
képfájl, hogyan kapcsolódik `illustration_ref`-ként egy technikához, hogyan nő a
`contentVersion`/`dataset_version`, milyen validáció fut le utána); a folyamat
`docs/KNOWLEDGE_BASE.md`-ből linkelt, külön dokumentumban vagy szakaszban él; a
meglévő `kyo-knowledge-base` skill hivatkozza.
**Követelmények:** FR-41 (új, 2026-11-13)

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-707** | Illusztráció-befogadási munkafolyamat dokumentálása + (ha szükséges) egy kis ellenőrző szkript/Gradle-feladat, ami jelzi, ha egy becsatolt kép formátuma/mérete/elnevezése nem felel meg (`KNOWLEDGE_BASE.md` kiegészítése, `kyo-knowledge-base` skill frissítése) | docs, core/data | M | — |

## US-28 · Fordítások és terminológia szakmai ellenőrzése

> **Felhasználóként** azt akarom, hogy a romaji átírások és a magyar jelentések
> dokumentáltan össze legyenek vetve hiteles Kyokushin-forrásokkal, **hogy** tudjam, mely
> rekordok állíthatók `verified = true` státuszra, és mi alapján.

**Elfogadási kritériumok:** minden jelenlegi, lektorálatlan (`editorial_review_required`)
rekordhoz a felülvizsgálat dokumentált forrásmegjelöléssel történik (ADR-010 szerint a
kvízből emiatt egyik sem esik ki, csak a jelölésük változhat); az eltérő/kétes
fordítások listázva, a döntés (elfogadva / javítva / nyitva marad) és a forrás
rögzítve; a kitalált vagy nem ellenőrizhető forrásból származó javítás **tilos**
(CLAUDE.md tartalmi integritás). A konkrét hitelesítő személy/szervezet kérdése
(OQ-02) ettől függetlenül nyitott marad.
**Követelmények:** FR-42 (új, 2026-11-13), CR-01…CR-05

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-708** | A 161 rekord romaji/magyar/kategória adatainak összevetése hiteles Kyokushin-forrásokkal; eltérések és források `docs/CONTENT_VERIFICATION.md`-ben; a `kyokushin-content` skill szabályai szerint **csak** forrással alátámasztott javítás kerülhet be | docs, core/data | L | — |

---

# E9 · Kiejtés, hangalapú kvíz, többnyelvűsítés — jövőbeli, jelenleg NEM indítható

> A mesterprompt **10. pontja** explicit kizárja MOST-ra a „hangos japán kiejtés"-t és a
> „többnyelvűség"-et. Az alábbi tételek a felhasználó igényét **rögzítik**, de
> szándékosan **nem indíthatók** `/dev`/`/devall`-lal: a „Függ" mező egy nyitott
> kérdésre (`OQ-05`/`OQ-06`/`OQ-07`) mutat, nem egy elvégezhető taskra, így a függőség
> sosem teljesül automatikusan. Az indításukhoz a mesterprompt 10. pontjának explicit,
> felhasználói felülvizsgálata szükséges (lásd `REQUIREMENTS.md` 10. szakasz).

| Task | Cím | Modul | Méret | Függ |
|---|---|---|---|---|
| **T-709** | Japán kiejtés lejátszása a tanulókártyán/részletezőn egy hangikonnal (gépi TTS vagy a felhasználó által biztosított hangfájlok) | feature/learn, core/ui | L | **OQ-05 (nyitott döntés)** |
| **T-710** | Új kvízkérdés-típus: lejátszott kiejtés alapján a helyes magyar nevet kell kiválasztani | core/quiz, feature/quiz | L | **OQ-05, OQ-06 (nyitott döntés)**, T-709 |
| **T-711** | Többnyelvűsítés: angol és német felhasználói szövegek a magyar mellett, nyelvválasztás a beállításokban | összes feature, core/model | L | **OQ-07 (nyitott döntés)** |
