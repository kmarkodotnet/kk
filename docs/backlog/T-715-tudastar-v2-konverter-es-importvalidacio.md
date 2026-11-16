# T-715 · Tudástár v2: szerkesztési formátum, konverter és validáció

| | |
|---|---|
| Epik / user story | E11 / US-30 |
| Modul | knowledgebase_transfer/tools, core/data, core/quiz |
| Méret | L |
| Függ | T-714 |
| Követelmény | KB-01, KB-02, KB-03 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Python konvertertesztek, JVM DTO/validátorteszt, round-trip és hibás fixture-ek; meglévő V-szabályok regressziója. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

Az új mezők a szerkesztési forrástól az importig ellenőrzötten és reprodukálhatóan jussanak el.

## Teendők

- [ ] Bővítsd a szerkesztési forrást és a konvertert: a hosszú cikk és típusos details ne kényszerüljön lapos CSV-cellákba; legyen egy kijelölt kanonikus szerkesztési hely.
- [ ] Vezesd át az általános rekordokat, fordításokat, címkéket, kapcsolatokat, forrásokat és revíziókat a csomagba és DTO-kba. A vizsgák és szerkesztett kérdések specifikus importja T-719/T-721.
- [ ] A T-713 alapján fogadd a régi formátumot, és adj diagnosztikát a nem támogatott új verzióra. A generált keresőmezőket egyetlen normalizáló állítsa elő.
- [ ] Validáld a típusfüggő mezőket, duplikált id-kat, kategóriát, source_ref-et, parent/variant ciklust és célrekordot. Az ismeretlen bibliográfiai adat null maradjon, ne váljon kitalált adattá.
- [ ] Rögzítsd a rekord- és csomagszintű hibák hatását, valamint az atomikus import előtti érvényesített köztes szerkezetet.

## Kész, ha

- [ ] Kétszeri konvertálás azonos forrásból azonos tartalmat eredményez a dokumentált időbélyegek kivételével.
- [ ] A régi 161 rekord és az új, vegyes fixture veszteség nélkül feldolgozható.
- [ ] A hibás rekord diagnosztikát ad; a csomag elutasítása nem váltja le a működő tudástárat.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A konverter/validátor vigye tovább és ellenőrizze az állításszintű összevetési bizonyítékot és elfogadási jogcímet. A kiinduló Kyo-adat a könyvfeldolgozás előtti verzió legyen; újrafuttatás ne hozzon létre önmegerősítő egyezést.
- [ ] Legyen fixture mind a négy besorolásra, félrevezető szöveghasonlóságra (például negáció vagy eltérő magasság) és bizonytalan OCR-re. Az import nem utasíthat el szabályosan elfogadott állítást pusztán a korábbi lektorálatlansági státusz miatt.

## Ellenőrzés és Definition of Done

- Python konvertertesztek, JVM DTO/validátorteszt, round-trip és hibás fixture-ek; meglévő V-szabályok regressziója.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
