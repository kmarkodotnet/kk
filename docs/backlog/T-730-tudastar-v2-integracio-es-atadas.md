# T-730 · Tudástár v2: integrációs ellenőrzés és átadás

| | |
|---|---|
| Epik / user story | E11 / US-30…US-35 |
| Modul | app, docs, összes érintett modul |
| Méret | L |
| Függ | T-720, T-723, T-724, T-729 |
| Követelmény | KB-01…KB-12 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Teljes releváns Gradle-ellenőrzések, konverter/validátor, adatbázis-migráció és eszközös end-to-end folyamat; pontos parancsok és eredmények dokumentálva. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A teljes bővítés együtt is működjön régi adatbázissal, új könyvtartalommal és offline környezetben.

## Teendők

- [ ] Integráld a validált tartalomcsomagot az assetsbe, a szerkesztési forrás és generált kimenet közötti egyezés ellenőrzésével.
- [ ] Ellenőrizd a régi telepítés migrációját, a megőrzött eredményeket, az új cikkeket, forrásokat, vizsgacsomagokat és vegyes kvízt.
- [ ] Próbáld a tartalomfrissítést aktív kvíz közben, a kérdésrevíziót, megszakítást, folyamat-visszaállítást és hibás új csomag elutasítását.
- [ ] Futtasd a releváns teljes unit/instrumentációs, build-, lint-/formázás- és akadálymentességi ellenőrzéseket; ellenőrizd, hogy nincs INTERNET permission.
- [ ] Frissítsd az IMPLEMENTATION_PLAN, TEST_REPORT, ACCEPTANCE, KNOWLEDGE_BASE, QUIZ_RULES és érintett architekturális dokumentumokat a tényleges állapotra.

## Kész, ha

- [ ] A régi tanulási eredmények nem vesznek el, és az új adatok nem minősítik át őket más képességgé.
- [ ] A 15 témakör, 13 könyvbeli vizsga és az új kérdéscsomag valós adatokkal végigjárható; a lektorálatlanság/hiányosság látható marad.
- [ ] Az átadási jelentés elkülöníti a futott és nem futott ellenőrzéseket, valamint a nyitott szakmai ügyeket.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] Az átadási ellenőrzés külön bizonyítsa: match nem igényel új lektorálást; minor_difference a könyv változatát eredményezi; conflict célzott szakmai ügyként megmarad. Ellenőrizd a szabályt érvényesítő kvízjogosultságot és UI-jelölést is.
- [ ] Az elfogadás ne követelje az összes könyvanyag ismételt szakmai lektorálását. A döntési jelentésből legyen világos, mi egyeztetés alapján elfogadott, mi valódi konfliktus, és mi előzmény nélküli új tartalom.

## Ellenőrzés és Definition of Done

- Teljes releváns Gradle-ellenőrzések, konverter/validátor, adatbázis-migráció és eszközös end-to-end folyamat; pontos parancsok és eredmények dokumentálva.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
