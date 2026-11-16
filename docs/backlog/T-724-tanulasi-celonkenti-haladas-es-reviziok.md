# T-724 · Haladás tanulási célonként, kérdésrevízióval

| | |
|---|---|
| Epik / user story | E11 / US-34 |
| Modul | core/model, core/data, feature/progress |
| Méret | L |
| Függ | T-722, T-719, T-717 |
| Követelmény | KB-09, KB-10 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Adatbázis-migráció, aggregációs invariánsok, több-entrys kérdés, revision és Haladás UI tesztjei. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A név felismerése, elméleti megértés és gyakorlati követelmény ne egyetlen félrevezető sikerarányba kerüljön.

## Teendők

- [ ] Bővítsd az eredmények szerződését: entry/objective/question/revision és selected_option_id; a generált kérdésekhez is legyen determinisztikus cél-hozzárendelés.
- [ ] Migráld a régi próbálkozásokat az eredeti kérdéstípusuknak megfelelő név-/képfelismerési célra; ne állíts elő visszamenőleges elméleti vagy gyakorlati teljesítést.
- [ ] A régi correct ítélet maradjon történeti adat; módosult kérdés megoldása alapján ne számold újra.
- [ ] Külön kezeld a kártya-önértékelést, kvízeredményt és olvasást; a gyakorlati bemutatás/erőnlét teljesítése nem vezethető le ezekből.
- [ ] Bővítsd a haladásképernyőt céltípus és tananyag/vizsga szerinti bontással; egy több entryhez kötött kérdés eredményét ne számold többször ugyanabba az összesítésbe.

## Kész, ha

- [ ] A migráció előtt és után a korábbi menetek helyes/hibás darabszáma változatlan.
- [ ] Két külön cél eredménye elkülönül; a nincs adat továbbra sem 0% tudás.
- [ ] Kérdés visszavonása vagy revízióváltása nem tünteti el a történeti eredményt.

## Ellenőrzés és Definition of Done

- Adatbázis-migráció, aggregációs invariánsok, több-entrys kérdés, revision és Haladás UI tesztjei.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
