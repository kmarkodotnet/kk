# T-723 · Tudás- és helyzetkvízek felülete és újragyakorlása

| | |
|---|---|
| Epik / user story | E11 / US-33 |
| Modul | feature/quiz, core/ui, navigation |
| Méret | L |
| Függ | T-722, T-718, T-724 |
| Követelmény | KB-07, KB-08 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | ViewModel/Compose végigjátszás, hibás kérdések újragyakorlása, állapot-visszaállítás és akadálymentesség. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyv tudás- és helyzetkérdései teljes kvízfolyamatban legyenek használhatók.

## Teendők

- [ ] Bővítsd a beállítót az új kérdésfajtákkal és a valós készenléti összegzéssel.
- [ ] Jeleníts meg promptot, önálló szöveges opciókat, helyes/hibás visszajelzést, magyarázatot és kapcsolódó tananyagot.
- [ ] A terv nyelvi szerződését kövesd: ne keverd egy kérdésen belül a különböző nyelvű promptot/opciókat/magyarázatot.
- [ ] Vezesd át a végső összegzést és hibás kérdések újragyakorlását, a stabil kérdés-/opcióazonosítókra és revízióra támaszkodva.
- [ ] Tartsd meg az egyszeri válaszrögzítést, megszakítást, forgatást, process restore-t és TalkBack használhatóságot.

## Kész, ha

- [ ] Etikett-helyzetkérdés és névkvíz ugyanabban a menetben végigjátszható.
- [ ] A helyes válasz és a magyarázat nem szivárog ki felfedés előtt.
- [ ] Az eredményképernyő nem várja, hogy minden válasz egy technikanév legyen.

## Ellenőrzés és Definition of Done

- ViewModel/Compose végigjátszás, hibás kérdések újragyakorlása, állapot-visszaállítás és akadálymentesség.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
