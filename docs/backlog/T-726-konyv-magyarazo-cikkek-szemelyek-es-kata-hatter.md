# T-726 · Könyv: magyarázó cikkek, személyek és kataháttér

| | |
|---|---|
| Epik / user story | E11 / US-35 |
| Modul | knowledgebase, docs |
| Méret | L |
| Függ | T-718, T-725 |
| Követelmény | KB-03, KB-04, KB-11 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Cikk-fixture validáció, Markdown/link és forráshely ellenőrzése, hosszú tartalom UI-próba. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A könyv narratív és szemléleti részei tanulható, hivatkozható tartalommá váljanak.

## Teendők

- [ ] Dolgozd fel a történelmet, mestereket, etikettet, filozófiát, szimbólumokat, edzéselméletet, küzdelmi magyarázatokat és katahátteret önálló person/article/kata elemekre.
- [ ] A rövid summary és a hosszú body különüljön el; a kapcsolódó fogalmakra és személyekre entry-hivatkozások mutassanak.
- [ ] A gyakorlati és elméleti tananyagot ne mosd össze; tameshiwari/kyusho csak a specifikált fogalom- és háttérismereti körben jelenjen meg.
- [ ] Rögzítsd a konkrét forrásképet és fejezetet; a történeti/életrajzi vitatható állítások és szervezeti eltérések maradjanak jelölt felülvizsgálati tételek.
- [ ] Az OCR-korrigálás önmagában ne jelentsen szakmai lektorálást; az egyező/kisebb eltérésű állítás elfogadását a felhasználói könyv–Kyo szabály szerinti jogcímmel rögzítsd.

## Kész, ha

- [ ] A narratív fejezetek teljes lefedettségi leltára elkészült rekord- vagy indokolt nyitott/kizárt hivatkozással.
- [ ] Minden feldolgozott cikk és személy megjeleníthető az új Tudástár UI-ban, offline.
- [ ] Nem kerül be a könyvön túli kitalált életrajzi adat vagy szakmailag hitelesnek feltüntetett következtetés.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A személyek/cikkek/katahátterek meglévő Kyo-állításokkal egyező részeit ne küldd új lektorálásra. Kisebb eltérésnél a könyv fogalmazása vagy pontosítása az irányadó; lényegi ellentmondásból külön ügy készüljön.
- [ ] A könyvben újonnan megjelenő életrajzi/történeti adat new_content marad, és nem fogadható el pusztán az ugyanabban a bekezdésben talált másik egyezés alapján.

## Ellenőrzés és Definition of Done

- Cikk-fixture validáció, Markdown/link és forráshely ellenőrzése, hosszú tartalom UI-próba.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
