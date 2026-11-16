# T-720 · Övvizsga-felkészülés: tananyagválasztás és tanulócsomag

| | |
|---|---|
| Epik / user story | E11 / US-32 |
| Modul | feature/learn, feature/quiz, feature/library, navigation |
| Méret | L |
| Függ | T-719, T-718 |
| Követelmény | KB-05, KB-06 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | ViewModel/Compose folyamat, szűrők metszete, forgatás és kevés jelölt. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A felhasználó kiválaszthassa, melyik tananyag melyik vizsgájára készül.

## Teendők

- [ ] Készíts tananyag-/verzió-/vizsgaválasztást a T-713 funkcionális specifikációja szerint.
- [ ] A választásból képzett csomag ugyanazokra az entry-id-kra hivatkozzon; működjön együtt a témakör- és típusválasztással.
- [ ] Mutasd külön az elméletileg gyakorolható részt és a gyakorlati/erőnléti követelménylistát. Kvízpontszám ne jelentsen teljesített övvizsgát.
- [ ] A beállítások túléljék a forgatást; hiányos, visszavont és lektorálatlan tananyagnál legyen pontos jelzés.
- [ ] A szűrt kvíz indíthatóságát a közös jogosultság/dryRun mondja meg; kevés kérdésre ne ígérj teljes menetet.

## Kész, ha

- [ ] A kiválasztott tananyagverzió és vizsgafokozat minden érintett képernyőn azonos.
- [ ] A csomag nem kever más szervezetből követelményeket, és nem címkéz gyakorlati jártasságot kvízeredmény alapján.
- [ ] Üres vagy kevés adat esetén érthető visszajelzésből vissza lehet módosítani a szűrést.

## Ellenőrzés és Definition of Done

- ViewModel/Compose folyamat, szűrők metszete, forgatás és kevés jelölt.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
