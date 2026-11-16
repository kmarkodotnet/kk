# T-717 · Meglévő tanulás és név-/képkvízek átvezetése

| | |
|---|---|
| Epik / user story | E11 / US-30 |
| Modul | core/quiz, core/data, feature/learn, feature/library, feature/progress |
| Méret | L |
| Függ | T-716 |
| Követelmény | KB-01, KB-04, KB-10 ([E11 követelmények](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese)); a normatív FR/DR/TR megfeleltetés T-713 feladata |
| Teszt | Meglévő kvízinvariánsok, valós tartalom, vegyes fixture, process restore és eredményképernyő regressziós tesztjei. |
| Állapot | Nyitott; a 2026-11-16-i felhasználói kérés alapján backlogba véve |

## Cél

A KnowledgeEntry bevezetése ne törje el a jelenlegi négy kérdéstípust és a tanulókártyákat.

## Teendők

- [ ] Vezesd át a jelenlegi fogyasztókat az új domainre, szükség esetén dokumentált, átmeneti Technique-adapterrel.
- [ ] A JP_TO_HU, HU_TO_JP, IMAGE_TO_JP, JP_TO_IMAGE csak a tényleges mezőkövetelményeiknek megfelelő tananyagelemekből kapjon jelölteket; cikkből ne gyárts japán nevet.
- [ ] Őrizd meg a determinisztikus generálást, dryRun/generate egyezést, képkapukat, feliratos kép kezelését és szinonimakizárást.
- [ ] A parent/variant kapcsolat ne váljon automatikusan szinonimává; a kétértelmű gyűjtőfogalom–változat válaszokat kérdésfüggően zárd ki.
- [ ] A régi mentett munkamenet, eredményképernyő és újragyakorlás a T-713 kompatibilitási szerződése szerint működjön.

## Kész, ha

- [ ] A meglévő 161 rekorddal mind a négy korábbi mód a korábbi szabályok szerint működik.
- [ ] Vegyes tartalommal sincs hibásan létrejött japán név vagy félreérthető válaszpár.
- [ ] A régi eredmény megjelenítése nem igényli az eredeti név változatlanságát.

## Könyv–Kyo egyeztetési szabály (2026-11-16)

A felhasználói döntés kötelező: [BOOK_CONTENT_POLICY.md](../BOOK_CONTENT_POLICY.md).
Ez az itt szereplő általános lektorálási megfogalmazások pontosítása; nem kell minden
könyvbeli adatot újra szakmailag lektorálni.

- [ ] A közös kvízjogosultság az egyeztetési szabály szerinti elfogadást is kezelje; a korábbi review státusz ne küldje vissza kötelező szakmai körre az egyező/kisebb eltérésű állítást.
- [ ] A mező-/állításfüggő kapu különítse el a vitatott és elfogadott adatot: egy leírásbeli konfliktus ne tiltsa automatikusan az egyébként elfogadott név–jelentés párt.

## Ellenőrzés és Definition of Done

- Meglévő kvízinvariánsok, valós tartalom, vegyes fixture, process restore és eredményképernyő regressziós tesztjei.
- Az E11 közös szabályai és a releváns globális DoD teljesülnek; a dokumentációs/tartalmi feladat ellenőrzése a változás jellegéhez igazodik.
- Az implementációs állás és a ténylegesen futtatott ellenőrzések dokumentáltak. Ami nem futott, annak oka szerepel.
- Csak elkészült és ellenőrzött munka után kerülhet a jegy a `docs/done/` mappába.

## Hatókör

Ez végrehajtandó backlogjegy, nem a funkció elkészültének igazolása. A jegy létrehozása nem módosítja az alkalmazás kódját vagy az éles tudástárat. A nem ismert szakmai/bibliográfiai adatot jelölni kell; kitalálni és hitelesként tárolni tilos.
