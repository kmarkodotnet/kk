# T-725 · Könyv: terminológia, technikaváltozatok és 15 témakör

| | |
|---|---|
| Epik / user story | E11 / US-35 |
| Modul | knowledgebase, knowledgebase_transfer/tools, docs |
| Méret | L |
| Függ | T-717 |
| Követelmény | KB-02, KB-03, KB-11 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Konverter és teljes tudástárvalidáció, id/alias/variant duplikációs jelentés, mintavételes JPG-összevetés. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyv fogalmai és technikái a meglévő tudástárat bővítsék, ne hozzanak létre párhuzamos névjegyzéket.

## Teendők

- [ ] Vedd fel a 6 új kategóriát a manifest szerkesztési forrásába: history, philosophy, training, combat, competition, exams. A jelenlegi 9 id maradjon meg.
- [ ] Készíts fejezet-/forráshely-alapú leltárt az images/book/book-repaired.md és a JPG-k alapján; a Markdown az átirat, a fotó az OCR-vita ellenőrzési alapja.
- [ ] Vesd össze a terminológiát a meglévő 161 rekorddal: egyező fogalomhoz meglévő id, valódi alternatív névhez alias, technikaváltozathoz külön entry és parent kapcsolat tartozzon.
- [ ] Fogalmak, irányok, magasságok, testrészek, számok, állások, ütések, rúgások, védések, katák és versenyvezényszavak kerüljenek a megfelelő típusba/kategóriába.
- [ ] Minden új és módosított rekord kapjon pontos source_ref-et és megfelelő review státuszt; a könyv bizonytalan terminológiáját dokumentáld, ne normalizáld észrevétlenül hiteles állítássá.

## Kész, ha

- [ ] A kategórialistában pontosan a javasolt 15 témakör szerepel, megőrzött régi id-kkel.
- [ ] Nincs gyűjtőfogalomba beolvasztott konkrét technikaváltozat és nincs tartalmi duplikáció.
- [ ] A szótár és technikai fejezetek minden leltározott tétele rekordra vagy indokolt nyitott/kizárt sorra vezet.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A meglévő Kyo-rekordokat állításonként sorold match, minor_difference vagy conflict osztályba. Match esetén nincs új lektorálás; minor_difference esetén a könyv értékét kell átvezetni, új lektorálás nélkül; conflict esetén célzott szakmai ügy keletkezik.
- [ ] A leltár őrizze a kiinduló Kyo-verziót/értéket, a könyv forráshelyét, az indoklást és az eredményt. Az új fogalom/technikaváltozat new_content, nem mesterséges konfliktus; a bizonytalan OCR-t előbb a fotóból rendezni kell.

## Ellenőrzés és Definition of Done

- Konverter és teljes tudástárvalidáció, id/alias/variant duplikációs jelentés, mintavételes JPG-összevetés.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
