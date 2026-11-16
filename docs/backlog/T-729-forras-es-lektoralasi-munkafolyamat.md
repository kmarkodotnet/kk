# T-729 · Forrás-, OCR- és szakmai lektorálási munkafolyamat

| | |
|---|---|
| Epik / user story | E11 / US-35 |
| Modul | docs, knowledgebase_transfer/tools |
| Méret | M |
| Függ | T-725, T-726, T-727, T-728 |
| Követelmény | KB-03, KB-11 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Jelentésgenerátor hiányzó és vegyes státuszú fixture-rel; hivatkozási és teljes lefedettségi ellenőrzés. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyvből származó tartalom eredete és bizonytalansága a későbbi javításoknál is követhető legyen.

## Teendők

- [ ] Különítsd el a transzkripció (OCR/javított), bibliográfiai azonosítás, tartalmi szakmai ellenőrzés, fordításellenőrzés és felhasználhatóság mezőit.
- [ ] Dokumentáld a forrásregisztrációt, forráshely/állítás javítását, reviewer/dátum/evidence rögzítését és a revízióemelés szabályát.
- [ ] Készíts gépi jelentést a hiányzó forrásról, ismeretlen bibliográfiáról, lektorálatlan állításról és kérdésről; nyitott ügyek maradhatnak, de ne legyenek elrejtve.
- [ ] A teljes könyv minden fejezetének/tételének legyen coverage hivatkozása feldolgozott rekordra vagy indokolt nyitott/kizárt ügyre.
- [ ] Az import/konverter a dokumentált összevetési döntés szerint kezelje az elfogadást; nincs feltétel nélküli státuszemelés, és az egyező/kisebb eltérésű állítás nem igényel új szakmai lektorálást. Az alkalmazásbeli használat a felhasználói szabállyal kiegészített bizalmi szerződés szerint dőljön el.

## Kész, ha

- [ ] A folyamat ismételhető és a jelentésből világos, mi készült el, mi vár még azonosításra vagy szakértőre.
- [ ] Az ismeretlen könyvcím/szerző/kiadás nem akadályozza az őszinte nyilvántartást, és nem töltődik kitalált értékkel.
- [ ] Egy javított OCR-szó nem állítja reviewed-ra az egész fejezetet vagy kérdéscsomagot.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A munkafolyamat központi szabálya a felhasználói háromutas döntés: egyezés elfogadva, kisebb eltérés könyv szerint elfogadva, lényegi ellentmondás lektorálandó. Az összevetés előzmény nélküli új tartalma külön new_content osztály.
- [ ] A jelentés külön számolja a négy osztályt és a kérdésszerkesztési/bibliográfiai/OCR nyitott ügyeket. Ne kérjen új lektorálást match/minor_difference esetben, és ne nevezze ezeket szakértő által lektoráltnak. Mutassa a kiinduló Kyo-verziót és az állításszintű döntési nyomot.

## Ellenőrzés és Definition of Done

- Jelentésgenerátor hiányzó és vegyes státuszú fixture-rel; hivatkozási és teljes lefedettségi ellenőrzés.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
