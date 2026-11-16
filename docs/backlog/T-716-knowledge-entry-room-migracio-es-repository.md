# T-716 · KnowledgeEntry: Room-migráció és repositoryk

| | |
|---|---|
| Epik / user story | E11 / US-30 |
| Modul | core/data |
| Méret | L |
| Függ | T-715 |
| Követelmény | KB-01, KB-02, KB-03, KB-10 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | MigrationTestHelper, repository-integráció, duplikált import, rollback és retired-feloldás; build és rétegszabály-ellenőrzés. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

Az új tartalomséma helyi tárolása az eddigi tanulási adatok elvesztése nélkül működjön.

## Teendők

- [ ] Implementáld a T-713 szerinti tartalomtáblákat, indexeket, mappereket, atomikus importot és publikus repository-szerződéseket.
- [ ] A ténylegesen legfrissebb Room-sémából vezess nem destruktív migrációt; ne feltételezz a taskíráskori verziószámot.
- [ ] Tartsd meg a KY-id-kat, régi kvízmeneteket, próbálkozásokat és önértékeléseket. A felhasználói adat ne kapjon tartalomra mutató törlési CASCADE-et.
- [ ] Kereshető legyen a cím, romaji és alias; szűrhető a kategória, kind és tags. A kapcsolatok és források visszaolvasása ne legyen N+1.
- [ ] A retired rekord továbbra is feloldható maradjon a történeti eredményekhez; az ismételt import legyen idempotens.

## Kész, ha

- [ ] Éles szerkezetű, korábbi eredményeket tartalmazó adatbázis migrálható, a sorok és összesítések megmaradnak.
- [ ] Az import sikertelensége után is a teljes korábbi tartalom látható, nincs félállapot.
- [ ] A feature modulok továbbra sem látnak Room entitást, DTO-t vagy DAO-t.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] Tárold és olvasd vissza veszteség nélkül az összevetési eredményt, a feldolgozás előtti Kyo-verziót, az elfogadási jogcímet és a lényegi ellentmondáshoz tartozó ügyet. A régi és könyvbeli érték nyoma kisebb eltérésnél is maradjon meg.
- [ ] Import- és migrációteszt ellenőrizze a kiválasztott könyvértéket, az egyező rekord id-megőrzését és az aktív tartalom automatikus ellentmondásos felülírásának hiányát.

## Ellenőrzés és Definition of Done

- MigrationTestHelper, repository-integráció, duplikált import, rollback és retired-feloldás; build és rétegszabály-ellenőrzés.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
