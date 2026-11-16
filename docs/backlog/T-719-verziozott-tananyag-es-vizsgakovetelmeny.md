# T-719 · Verziózott tananyagok és övvizsga-követelmények tárolása

| | |
|---|---|
| Epik / user story | E11 / US-32 |
| Modul | core/model, core/data, knowledgebase_transfer/tools |
| Méret | L |
| Függ | T-716 |
| Követelmény | KB-05, KB-06 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Import-, repository- és sorrend/öröklés tesztek, erőnléti mértékegységek, hibás/ciklikus hivatkozások. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

Az övfokozathoz tartozás szervezethez és tananyagverzióhoz kötött kapcsolat legyen.

## Teendők

- [ ] Implementáld a Curriculum, CurriculumVersion, Exam, Requirement modellt és import/repository rétegét.
- [ ] A szervezet és bibliográfia ismeretlen értéke maradhat jelölt null; a könyvből származó tananyag ne váljon általános Kyokushin-szabállyá.
- [ ] A követelmény hivatkozzon konkrét entryre és elvárásra: megnevezés, elméleti ismeret, gyakorlati végrehajtás vagy erőnléti feladat. Az erőnléti mennyiségek/idők strukturáltan, mértékegységgel legyenek tárolhatók.
- [ ] Legyen explicit vizsgasorrend, előfeltétel/öröklés, valamint ciklus- és duplikációellenőrzés. Egy minimum_kyu szám és annak numerikus összehasonlítása nem helyettesíti a tananyagot.
- [ ] Az entry ne duplikálódjon attól, hogy több vizsgához vagy tananyaghoz tartozik; a követelmény külön source_ref-et és review státuszt kaphasson.

## Kész, ha

- [ ] Két eltérő tananyagverzió követelményei egymás mellett kezelhetők, nem írják felül egymást.
- [ ] A 9. kyu konkrét Mae geri-változatra hivatkozhat a gyűjtőfogalom téves megjelölése nélkül.
- [ ] Az örökölt követelmények determinisztikusan, duplikálás nélkül állnak elő; ismeretlen öröklés nem lesz automatikusan igaz.

## Ellenőrzés és Definition of Done

- Import-, repository- és sorrend/öröklés tesztek, erőnléti mértékegységek, hibás/ciklikus hivatkozások.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
