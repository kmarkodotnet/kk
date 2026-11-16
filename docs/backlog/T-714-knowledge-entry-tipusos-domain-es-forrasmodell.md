# T-714 · KnowledgeEntry: típusos domain, kapcsolatok és forrásmodell

| | |
|---|---|
| Epik / user story | E11 / US-30 |
| Modul | core/model |
| Méret | L |
| Függ | T-713 |
| Követelmény | KB-01, KB-02, KB-03 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | JVM domain- és lokalizációtesztek a T-713 szerződésére. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A technikák mellett fogalmak, katák, személyek és cikkek is elsőrendű tananyagelemek legyenek.

## Teendők

- [ ] Vezesd be az új azonosítókat és a KnowledgeEntry modellt, a meglévő KY-id-k megtartásával.
- [ ] A details típusos Kotlin hierarchia legyen, ne korlátlan Map<String, Any>; a típusonkénti invariánsok a domainben éljenek.
- [ ] Valósítsd meg az aliasok, címkék, parent/variant és related kapcsolat, source/source_ref, LearningObjective és annak entry-kapcsolatai, rekordrevízió és ellenőrzési állapot szerződéseit.
- [ ] A magyar cím kötelező; romaji csak az azt használó típusoknál kötelező. Fordítás hiányában mezőnként magyar visszaesés legyen; üres fordítás ne fedje el az eredetit.
- [ ] Tartsd Android-függetlenül a modellt, és dokumentáld a későbbi kompatibilitási adapter belépési pontját.

## Kész, ha

- [ ] Cikk és személy romaji nélkül is érvényes, a régi technika adatai veszteség nélkül képviselhetők.
- [ ] Hibás vagy üres azonosító, kind/details eltérés és önmagára mutató parent nem jut érvényes domain objektumba.
- [ ] A változat és az alias fogalma elkülönül; a domain nem emel automatikusan hitelesítési státuszt.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] Legyen típusos összevetési eredmény és elfogadási jogcím: érintett állítás/mező, kiinduló Kyo-verzió/érték, könyvforrás, besorolás, indoklás és kiválasztott érték. Az egyeztetési szabály szerinti elfogadás nem igényel kitalált reviewer-adatot.
- [ ] Domain-tesztek igazolják az egyezés és kisebb eltérés lektorálás nélküli elfogadhatóságát, illetve a lényegi ellentmondás külön kezelését; egy mező elfogadása nem terjed ki a rekord összes állítására.

## Ellenőrzés és Definition of Done

- JVM domain- és lokalizációtesztek a T-713 szerződésére.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
