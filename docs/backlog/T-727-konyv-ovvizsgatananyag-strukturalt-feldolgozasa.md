# T-727 · Könyv: 10–1. kyu és 1–3. dan tananyag feldolgozása

| | |
|---|---|
| Epik / user story | E11 / US-35 |
| Modul | knowledgebase, docs |
| Méret | L |
| Függ | T-719, T-725 |
| Követelmény | KB-05, KB-06, KB-11 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Fokozatonkénti tartalmi ellenőrzés, kombinációs sorrend és mértékegység-tesztek, importvalidáció. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyv vizsgatáblái ténylegesen kiválasztható, verziózott tananyagként legyenek elérhetők.

## Teendők

- [ ] Hozz létre könyvhöz kötött, nem általánosított CurriculumVersion rekordot; az ismeretlen szervezet/kiadás legyen jelölt, ne kitalált.
- [ ] Dolgozd fel mind a 10 kyu és 3 dan fokozatot, az elméleti, technikai, kata-, kombináció-, kumite- és erőnléti követelményekkel.
- [ ] A konkrét technikákat meglévő vagy T-725-ben létrehozott variant entryhez kösd; a kombináció sorrendje, az ismétlésszám/idő/mértékegység és feltételek megmaradjanak.
- [ ] Vizsgáld a könyvben ténylegesen közölt előfeltétel/öröklés szabályt; hiányában ne találd ki, és a felület jelenítse meg a korlátot.
- [ ] Adj forráshelyet minden követelményhez és készíts fokozatonkénti visszaellenőrzési jelentést.

## Kész, ha

- [ ] Mind a 13 vizsga visszaállítható strukturált rekordokból a könyv tábláival összevethetően.
- [ ] Nincs elveszett erőnléti egység vagy hibás gyűjtőfogalom-hivatkozás.
- [ ] A T-720 felület az új csomagból a megfelelő vizsga tartalmát állítja össze.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A követelményeket csak azonos fogalom, technikaváltozat és összevethető tananyagkontextus esetén hasonlítsd. Egyezésnél nincs új lektorálás; kisebb eltérésnél a könyv az erősebb; valódi lényegi konfliktusnál célzott ügy keletkezik.
- [ ] Más szervezet vagy kiadás tananyaga önmagában nem ellentmondás. Megváltozott övfok, mennyiség vagy technika nem minősülhet pusztán kevés eltérő karakter miatt kisebb eltérésnek.

## Ellenőrzés és Definition of Done

- Fokozatonkénti tartalmi ellenőrzés, kombinációs sorrend és mértékegység-tesztek, importvalidáció.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
