# Feladatcsoportok – amit érdemes egy menetben elvégezni

A `docs/backlog/` feladatai **érthetőségre** vannak bontva, nem agent-gazdaságosságra.
Néhányuk ugyanazon a fájlkészleten dolgozik: ezeket egy menetben elvégezni alig kerül
többe, mint egyet, mert a kontextus betöltése közös.

A **`/devall`** ezt a fájlt olvassa, és ha a soron következő feladat egy csoport **első**
tagja, **az egész csoportot** adja át egy subagentnek. A csoport akkor indítható, ha
minden tagjának minden külső függősége teljesült.

## Szabályok

- A csoport tagjai **ugyanabban a sorrendben** készülnek el, ahogy itt szerepelnek.
- A csoport **minden** task-fájlja átkerül a `docs/done/` mappába, mindegyik a saját
  „Elkészült" szakaszával.
- Ha a csoport **bármelyik** tagja nem teljesíti a Definition of Done-t, **egyik sem**
  mozdul, és a `/devall` megáll (`TASK_FAILED`).
- Egy csoport sem lépheti át az epik határát, és nem keverhet modult – ha mégis
  indokolt lenne, inkább maradjon külön.

## Csoportok

| Csoport | Tagok | Modul | Miért egy menetben |
|---|---|---|---|
| **G-01** | T-301, T-401, T-402, T-502 | `core/data` | Mind a `ProgressRepository` és a kvízjelöltek körül forog; ugyanaz a DAO- és mapper-készlet. Külön-külön négyszer fizetnénk ki ugyanazt a betöltést. |
| **G-02** | T-207, T-211 | `feature/library` | A ViewModel és a tesztje ugyanaz a fájlkészlet; a teszt írása közben derül ki, jó-e az állapotmodell. |
| **G-03** | T-208, T-210 | `feature/library` | A listaképernyő és az üres/hiba állapotai ugyanannak a képernyőnek az ágai. |
| **G-04** | T-302, T-303 | `feature/learn` | A beállítóképernyő adja a kártyaállapot bemenetét; együtt tervezhető. |
| **G-05** | T-304, T-305, T-307 | `feature/learn` | Kártya UI + a feliratos kép elrejtése + a záróképernyő – egyazon képernyő három ága. |
| **G-06** | T-306, T-308 | `feature/learn` | Önértékelés és a tanulási ViewModel tesztjei. |
| **G-07** | T-405, T-406, T-410 | `feature/quiz` | A futó kvíz **állapotgépe**: mentett állapot, egyszeri válasz, megszakítás. Szétvágva a három rész között kellene átadni a teljes állapotmodellt. |
| **G-08** | T-407, T-409 | `feature/quiz` | A szöveges kérdés UI és a rá adott visszajelzés. |
| **G-09** | T-411, T-412, T-415 | `feature/quiz` | Eredményképernyő + hibák újragyakorlása + a lektorálatlanság jelzése. |
| **G-10** | T-504, T-505, T-506 | `feature/progress` | Az áttekintő képernyő három alszakasza. |
| **G-11** | T-507, T-509 | `feature/progress` | Beállítások és a haladás ViewModel tesztjei. |
| **G-12** | T-603, T-607 | `app`, gyökér | Két független infrastruktúra-feladat, egyiknek sincs függősége; a manifest-teszt és a lint ugyanaz a build-kontextus. |
| **G-13** | T-608, T-609 | `docs` | Két dokumentumkiadás; mindkettő a kész kódhoz méri a leírást. |

## Szándékosan NEM csoportosított

| Feladat | Miért marad önálló |
|---|---|
| T-403, T-404 | A kvízbeállító ViewModel és UI külön-külön is `L`; a `dryRun`-egyezés miatt a ViewModel önmagában is figyelmet kíván. |
| T-408 | A képes kérdéstípusok akadálymentessége önálló gondolkodást kíván. |
| T-413, T-414 | Akadálymentesség és a kvíz ViewModel tesztjei – mindkettő a kvízhelyesség közelében. |
| T-501 | `L` méretű, aggregált SQL; önálló figyelmet kíván. |
| T-503 | A haladás áttekintő képernyője az alszakaszok kerete. |
| T-601 | Az éles assets bemásolása – külön ellenőrzendő. |
| T-602 | A rétegszabály automatizált ellenőrzése. |
| T-604, T-605, T-606 | Akadálymentességi átfésülés, extrém beállítások, UI tesztek – mind az egész appot érintik. |
| T-610 | Az átadás lezárása. |
