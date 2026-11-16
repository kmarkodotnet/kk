# T-728 · Könyv: tudás- és helyzetkérdések szerkesztése

| | |
|---|---|
| Epik / user story | E11 / US-35 |
| Modul | knowledgebase, docs |
| Méret | L |
| Függ | T-721, T-726, T-727 |
| Követelmény | KB-07, KB-08, KB-11 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Kérdésvalidáció, emberileg áttekinthető megoldás-/forráslista és vegyes motor dryRun; a szakmai ellenőrzés hiánya explicit marad. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyv elméleti anyagából ellenőrizhető kérdések készüljenek az új motorhoz.

## Teendők

- [ ] Szerkessz első kérdéscsomagot minden alkalmas elméleti témakörhöz: történelem, személyek, etikett, filozófia/szimbólumok, edzéselvek, katák, küzdelem, versenyzés és vizsgaismeret.
- [ ] Kérdésenként legyen tanulási cél, háttér-entry, 4 értelmileg különböző opció, 1 alátámasztható megoldás, magyarázat és pontos source_ref.
- [ ] A helyzetkérdés a könyvben közölt eljárást kérdezze; a szervezetfüggő választ tegye egyértelművé a kérdésszöveg.
- [ ] Készíts témakörönkénti lefedettségi táblát; a nem egyértelmű, forráshiányos állításokból ne legyen aktív kérdés.
- [ ] A kérdés saját review állapota és esetleges kizárása ne öröklődjön automatikusan az entrytől.

## Kész, ha

- [ ] Minden felsorolt alkalmas témakörben legalább 5 valid kérdés vagy konkrétan dokumentált tartalmi akadály szerepel; a tényleges aktív menetszámot a dryRun határozza meg.
- [ ] A kezdeti csomagban van etikett-helyzetkérdés, történelmi kérdés és szimbólum-/kataismereti kérdés.
- [ ] A megoldás ellenőrizhető, a téves opciók nem védhető második helyes válaszok.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] Egyezés vagy kisebb eltérés alapján elfogadott háttérállítást ne lektoráltass újra a kérdéscsomag miatt. A helyes válasz alátámasztását és az opciók egyértelműségét szerkesztési ellenőrzés vizsgálja.
- [ ] A kérdés lefedettségi jelentése hivatkozzon a háttérállítás besorolására/elfogadási jogcímére. A conflict esetet külön szakmai ügyként, a new_content esetet külön új tartalomként mutassa, ne soroljon minden könyvkérdést automatikusan a konfliktusok közé.

## Ellenőrzés és Definition of Done

- Kérdésvalidáció, emberileg áttekinthető megoldás-/forráslista és vegyes motor dryRun; a szakmai ellenőrzés hiánya explicit marad.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
