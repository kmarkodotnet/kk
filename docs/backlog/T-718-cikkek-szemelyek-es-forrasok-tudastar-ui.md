# T-718 · Cikkek, személyek és források megjelenítése a Tudástárban és Tanulásban

| | |
|---|---|
| Epik / user story | E11 / US-31 |
| Modul | feature/library, feature/learn, core/ui |
| Méret | L |
| Függ | T-717 |
| Követelmény | KB-03, KB-04 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | ViewModel- és Compose-tesztek vegyes tartalomra, lokalizációra, hosszú szövegre és akadálymentességre. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A hosszabb könyvtartalom is olvasható és tanulható legyen, ne technikanévként jelenjen meg.

## Teendők

- [ ] Bővítsd a listát és keresőt típus-/címkeszűréssel; romaji nélküli elemen is legyen értelmes cím és részletező.
- [ ] Jelenítsd meg a summary/body_md mezőt, a kapcsolatokat és a forráshelyeket. A Markdown olvasás offline, a támogatott formázás dokumentált legyen.
- [ ] Különítsd el az olvasást és a visszakérdezhető tanulókártyát; cikkolvasás ne jelentsen automatikus tudást vagy gyakorlati teljesítést.
- [ ] A nyelvi visszaesés, hiányzó bibliográfia, hiányzó kép, retired hivatkozás és ellenőrzési jelzés kezelése legyen konzisztens.
- [ ] Az új felületekhez magyar UI-alap, a meglévő támogatott UI-nyelvek, TalkBack, nagy betűméret és forgatáskezelés is tartozzon.

## Kész, ha

- [ ] Technika, kata, személy és hosszú cikk is böngészhető működő visszanavigálással.
- [ ] A szöveg nem veszít felsorolást/kiemelést, és nem fed fel rejtett szókártyaválaszt.
- [ ] A forráshivatkozás nem igényel internetet vagy új INTERNET permissiont.

## Ellenőrzés és Definition of Done

- ViewModel- és Compose-tesztek vegyes tartalomra, lokalizációra, hosszú szövegre és akadálymentességre.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
