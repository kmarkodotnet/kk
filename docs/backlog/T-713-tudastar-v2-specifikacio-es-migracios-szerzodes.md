# T-713 · Tudástár v2: specifikáció, példarekordok és migrációs szerződés

| | |
|---|---|
| Epik / user story | E11 / US-30 |
| Modul | docs |
| Méret | M |
| Függ | — |
| Követelmény | KB-01…KB-12 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Dokumentumhivatkozás- és JSON-példaellenőrzés; nincs alkalmazásimplementáció ebben a taskban. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyvből származó új tartalomtípusokhoz közös, implementálható szerződés kell a jelenlegi név–jelentés modell helyett.

## Teendők

- [ ] Dokumentáld a jelenlegi szerkesztési JSON/CSV → konverter → csomagolt manifest/entries → DTO → Technique → Room láncot, a 161 rekordot és a 9 kategóriát.
- [ ] Rögzítsd a KnowledgeEntry közös mezőit: stabil id, kind, category_id, title_hu, summary_hu, body_md_hu, fordítás-mapek, tags, aliases, típusos details, source_refs, revision, review_status, quiz_excluded, retired. Típusok: term, technique, kata, person, article; a kategória külön dimenzió.
- [ ] Definiáld a források, kapcsolatok, tananyagverziók, vizsgák, követelmények, tanulási célok és szerkesztett kérdések sémáját. A technique parent/variant kapcsolat nem szinonima. A kvízképesség levezetett marad.
- [ ] Rögzítsd a séma- és adatbázis-verzióváltás, régi csomagok olvasása, régi kliens új csomaggal, ismeretlen kind, hibás hivatkozás és félbeszakadt import viselkedését. Az ismeretlen mezők ignorálása nem tesz egy új kötelező szerkezetet visszafelé kompatibilissé.
- [ ] Készíts validálható példákat: meglévő KY-0096, annak külön változata, etikettcikk, személy/kata, forrás, 9. kyu követelmény és helyzetkérdés. Az első szerkesztett single_choice típus 4 stabil opcióazonosítóval és 1 helyes válasszal illeszkedjen a jelenlegi UI-hoz; a korábbi háromopciós beszélgetési példa csak illusztráció.
- [ ] Frissítsd a REQUIREMENTS, FUNCTIONAL_SPEC, ARCHITECTURE, KNOWLEDGE_BASE és QUIZ_RULES vonatkozó részeit és készíts ADR-t a következő szabad azonosítóval. Rendelj normatív FR/DR/TR hivatkozásokat az E11 KB-xx követelményeihez; ezek nem meglévő FR-azonosítók.

## Kész, ha

- [ ] A jelenlegi és a tervezett adatszerkezet, mezőkötelezőség és rétegleképezés egyértelműen összehasonlítható.
- [ ] Minden későbbi E11 jegyhez van specifikációs hivatkozás és nincs egymásnak ellentmondó példa.
- [ ] A KY-azonosítók, felhasználói eredmények, magyar visszaesés és ellenőrzési státusz megőrzésének szabályai rögzítettek.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A sémában különüljön el az összevetési besorolás és az elfogadás jogcíme a szakértői lektorálástól. Egyezés vagy kisebb eltérés miatt ne legyen kötelező új szakmai kör; kisebb eltérésnél a könyvérték kerül be. Lényegi ellentmondásnál legyen célzott lektorálási ügy.
- [ ] A példarekordok és normatív követelmények fedjék le a match, minor_difference, conflict és new_content esetet; az utóbbi nem feltételezett egyezés. A T-713-ban összehangolandó dokumentumok és ADR-ek is a felhasználói döntést kövessék.

## Ellenőrzés és Definition of Done

- Dokumentumhivatkozás- és JSON-példaellenőrzés; nincs alkalmazásimplementáció ebben a taskban.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
