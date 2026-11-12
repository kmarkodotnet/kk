# docs/backlog – a nyitott fejlesztési feladatok

Minden fejlesztési feladat **külön fájlban** él ebben a mappában. Egy elkészült feladat
fájlja átkerül a [`../done/`](../done/) mappába – a két mappa együtt adja ki a teljes
munkamennyiséget, és a `backlog/` tartalma mindig a hátralévő munka.

## Hogyan választódik ki a következő feladat

A `/dev` parancs determinista szabályt követ:

1. Vedd a `docs/backlog/` fájljait **növekvő task-azonosító szerint** (T-101, T-102, …).
2. Az első olyan feladat a soron következő, amelynek **minden függősége** már a
   `docs/done/` mappában van (a „Függ” mező üres vagy teljesül).
3. Ha egyetlen ilyen sincs, a parancs megáll és jelzi, melyik függőség hiányzik.

A **`/devall`** ugyanezt a szabályt futtatja sorozatban: feladatonként egy subagentet
indít, megvárja, ellenőrzi, hogy a fájl ténylegesen átkerült-e a `done/` mappába, majd
indítja a következőt. Egyszerre mindig pontosan egy subagent fut.

Így a sorrend kiszámítható, és a függőségi lánc nem sérülhet.

## Feladatok

| Task | Cím | Epik | Modul | Méret | Függ |
|---|---|---|---|---|---|
| [T-304](T-304-feature-learn-kartya-ui.md) | feature/learn: kártya UI | E3 | `feature/learn` | L | T-303, T-204 |
| [T-305](T-305-feature-learn-feliratos-kep-elrejtese-felfedesig.md) | feature/learn: feliratos kép elrejtése felfedésig | E3 | `feature/learn` | S | T-304 |
| [T-306](T-306-feature-learn-onertekeles.md) | feature/learn: önértékelés | E3 | `feature/learn` | M | T-301, T-304 |
| [T-307](T-307-feature-learn-zarokepernyo.md) | feature/learn: záróképernyő | E3 | `feature/learn` | S | T-304 |
| [T-308](T-308-feature-learn-viewmodel-tesztek.md) | feature/learn: ViewModel tesztek | E3 | `feature/learn` | M | T-306 |
| [T-403](T-403-feature-quiz-beallito-viewmodel.md) | feature/quiz: beállító ViewModel | E4 | `feature/quiz` | L | T-111, T-402 |
| [T-404](T-404-feature-quiz-beallito-ui.md) | feature/quiz: beállító UI | E4 | `feature/quiz` | L | T-403 |
| [T-405](T-405-feature-quiz-quizrunstate.md) | feature/quiz: QuizRunState | E4 | `feature/quiz` | M | T-403 |
| [T-406](T-406-feature-quiz-egyszeri-valasz-kikenyszeritese.md) | feature/quiz: egyszeri válasz kikényszerítése | E4 | `feature/quiz` | M | T-405, T-401 |
| [T-407](T-407-feature-quiz-a-es-b-kerdes-ui.md) | feature/quiz: (A) és (B) kérdés UI | E4 | `feature/quiz` | M | T-405 |
| [T-408](T-408-feature-quiz-c-es-d-kerdes-ui.md) | feature/quiz: (C) és (D) kérdés UI | E4 | `feature/quiz` | L | T-407, T-204 |
| [T-409](T-409-feature-quiz-valasz-visszajelzes.md) | feature/quiz: válasz-visszajelzés | E4 | `feature/quiz` | M | T-407 |
| [T-410](T-410-feature-quiz-elorehaladas-es-megszakitas.md) | feature/quiz: előrehaladás és megszakítás | E4 | `feature/quiz` | M | T-405 |
| [T-411](T-411-feature-quiz-eredmenykepernyo.md) | feature/quiz: eredményképernyő | E4 | `feature/quiz` | L | T-113, T-410 |
| [T-412](T-412-feature-quiz-hibak-ujragyakorlasa.md) | feature/quiz: hibák újragyakorlása | E4 | `feature/quiz` | M | T-411 |
| [T-413](T-413-feature-quiz-akadalymentesseg.md) | feature/quiz: akadálymentesség | E4 | `feature/quiz` | M | T-408, T-409 |
| [T-414](T-414-feature-quiz-viewmodel-tesztek.md) | feature/quiz: ViewModel tesztek | E4 | `feature/quiz` | L | T-412 |
| [T-415](T-415-feature-quiz-lektoralatlan-tartalom-jelzese.md) | feature/quiz: lektorálatlan tartalom jelzése | E4 | `feature/quiz` | S | T-404, T-411 |
| [T-501](T-501-core-data-aggregalt-statisztika-lekerdezesek.md) | core/data: aggregált statisztika lekérdezések | E5 | `core/data` | L | T-401 |
| [T-503](T-503-feature-progress-attekinto-kepernyo.md) | feature/progress: áttekintő képernyő | E5 | `feature/progress` | L | T-501, T-206 |
| [T-504](T-504-feature-progress-gyakorlast-igenylo-lista.md) | feature/progress: gyakorlást igénylő lista | E5 | `feature/progress` | M | T-503 |
| [T-505](T-505-feature-progress-onertekelesi-blokk.md) | feature/progress: önértékelési blokk | E5 | `feature/progress` | S | T-503 |
| [T-506](T-506-feature-progress-technika-reszletezo.md) | feature/progress: technika-részletező | E5 | `feature/progress` | M | T-503 |
| [T-507](T-507-feature-progress-beallitasok.md) | feature/progress: beállítások | E5 | `feature/progress` | M | T-502, T-503 |
| [T-508](T-508-feature-progress-fejlesztoi-diagnosztika.md) | feature/progress: fejlesztői diagnosztika | E5 | `feature/progress` | M | T-117, T-507 |
| [T-509](T-509-feature-progress-viewmodel-tesztek.md) | feature/progress: ViewModel tesztek | E5 | `feature/progress` | M | T-507 |
| [T-601](T-601-a-tudastar-bemasolasa-az-assets-ala.md) | A tudástár bemásolása az assets alá | E6 | `app` | S | T-117 |
| [T-602](T-602-tr-07-retegszabaly-ellenorzes-automatizalva.md) | TR-07: rétegszabály-ellenőrzés automatizálva | E6 | `core/quiz` | M | T-110 |
| [T-603](T-603-tr-08-manifest-permission-teszt.md) | TR-08: manifest permission teszt | E6 | `app` | S | — |
| [T-604](T-604-akadalymentessegi-atfesules.md) | Akadálymentességi átfésülés | E6 | minden feature | L | T-413, T-503 |
| [T-605](T-605-nagy-betumeret-es-forgatas-ellenorzese.md) | Nagy betűméret és forgatás ellenőrzése | E6 | minden feature | M | T-604 |
| [T-606](T-606-tr-06-avd-es-ui-tesztek.md) | TR-06: AVD és UI tesztek | E6 | `app` | L | T-412, T-503 |
| [T-607](T-607-lint-es-kodformazas.md) | Lint és kódformázás | E6 | gyökér | M | — |
| [T-608](T-608-docs-knowledge-base-md-kiadott-valtozata.md) | docs/KNOWLEDGE_BASE.md kiadott változata | E6 | `docs` | M | T-117 |
| [T-609](T-609-docs-quiz-rules-md-kiadott-valtozata.md) | docs/QUIZ_RULES.md kiadott változata | E6 | `docs` | M | T-112 |
| [T-610](T-610-atadas-readme-tesztjelentes-apk-elfogadasi-krite.md) | Átadás: README, tesztjelentés, APK, elfogadási kritériumok | E6 | `docs` | M | T-606 |

## Kapcsolódó

- [`../BACKLOG.md`](../BACKLOG.md) – epikek, user story-k elfogadási kritériumokkal, a globális DoR/DoD és a mérföldkövek
- [`../IMPLEMENTATION_PLAN.md`](../IMPLEMENTATION_PLAN.md) – fázisok és a projekt tényleges állása
- [`../done/`](../done/) – az elkészült feladatok
