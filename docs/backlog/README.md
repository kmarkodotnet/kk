# docs/backlog – a nyitott fejlesztési feladatok

Minden fejlesztési feladat **külön fájlban** él ebben a mappában. Egy elkészült feladat
fájlja átkerül a [`../done/`](../done/) mappába – a két mappa együtt adja ki a teljes
munkamennyiséget, és a `backlog/` tartalma mindig a hátralévő munka.

## Hogyan választódik ki a következő feladat

A `/dev` parancs determinista szabályt követ:

1. Vedd a `docs/backlog/` fájljait **növekvő task-azonosító szerint** (T-101, T-102, …).
2. Az első olyan feladat a soron következő, amelynek **minden függősége** már a
   `docs/done/` mappában van (a „Függ” mező üres vagy teljesül).
3. Ha egyetlen ilyen sincs, a parancs megáll és jelzi, melyik függőség hiányzik.

A **`/devall`** ugyanezt a szabályt futtatja sorozatban: feladatonként egy subagentet
indít, megvárja, ellenőrzi, hogy a fájl ténylegesen átkerült-e a `done/` mappába, majd
indítja a következőt. Egyszerre mindig pontosan egy subagent fut.

Így a sorrend kiszámítható, és a függőségi lánc nem sérülhet.

## Feladatok

Az eredeti M1…M7 szállítási kör (T-101…T-611) lezárult, mindegyik a
[`../done/`](../done/) mappában van. 2026-11-13-án a felhasználó design- és
funkcióvisszajelzése alapján **új, önálló taskok** kerültek fel – ezek a jelen mappában,
`T-701`-től indulva találhatók:

| Task | Indítható `/dev`-vel? | Megjegyzés |
|---|---|---|
| ~~T-701~~ | — | **kész, 2026-11-13** (`docs/done/`) – indítóképernyő, kék kandzsi logó |
| ~~T-702~~ | — | **kész, 2026-11-13** (`docs/done/`) – `docs/DESIGN_NOTES.md` tervjegyzet |
| ~~T-703~~ | — | **kész, 2026-11-14** (`docs/done/`) – Kezdőoldal, `feature/home`, induló útvonal |
| ~~T-704~~ | — | **kész, 2026-11-14** (`docs/done/`) – Tudástár a bottom nav jobb szélén |
| ~~T-705~~ | — | **kész, 2026-11-14** (`docs/done/`) – kategória-ikonok és technikalista drill-down a Tanulásban |
| ~~T-706~~ | — | **kész, 2026-11-14** (`docs/done/`) – felső fülváltó "Japán nevek"/"Magyar nevek" a tanulókártyán |
| ~~T-712~~ | — | **kész, 2026-11-14** (`docs/done/`) – `core/ui` `Typography`/`Shapes`, a T-702 tervét ülteti kódba – ezzel az E7 epik lezárult |
| ~~T-707~~ | — | **kész, 2026-11-14** (`docs/done/`) – illusztráció-befogadási munkafolyamat dokumentálva a `KNOWLEDGE_BASE.md` 6a. pontjában |
| T-708 | igen | tartalom-munkafolyamat (`BACKLOG.md` E8) |
| ~~T-711~~ | — | **kész, 2026-11-14** (`docs/done/`) – többnyelvűsítés (tetszőleges számú nyelv, valódi EN/DE tartalommal), OQ-07 feloldva – lásd `ADR-013`, `BACKLOG.md` E10 |
| T-709…T-710 | **nem** | hangos kiejtés, kiejtés-alapú kvíztípus – a „Függ" mezőjük szándékosan egy nyitott kérdésre (`OQ-05`/`OQ-06`) mutat, nem egy elvégezhető taskra, mert a mesterprompt 9/10. pontja MOST kizárja őket (`BACKLOG.md` E9) |

A `/dev`/`/devall` a megszokott, növekvő azonosító szerinti, függőségellenőrzött
szabályt követi – a T-709…T-710 ennek megfelelően **megáll és blokkoltként jelzi
magát**, nem próbálja implementálni a kizárt funkciót.

A tartalmi lektorálás és az ismert, külön fejlesztési ügyek az
[elfogadási jegyzőkönyvben](../ACCEPTANCE.md) és a
[konvenciók 8. szakaszában](../CONVENTIONS.md) szerepelnek; ezek nem megoldottként
jelentett feladatok.

## Kapcsolódó

- [`../BACKLOG.md`](../BACKLOG.md) – epikek, user story-k elfogadási kritériumokkal, a globális DoR/DoD és a mérföldkövek
- [`../IMPLEMENTATION_PLAN.md`](../IMPLEMENTATION_PLAN.md) – fázisok és a projekt tényleges állása
- [`../done/`](../done/) – az elkészült feladatok


## Könyv alapú tudástár – E11 (2026-11-16)

**18 új, nyitott jegy: T-713…T-730.** Ez tervezett fejlesztés; a jegyek felvétele
nem jelenti az adatmodell, az új kérdéstípus vagy a könyvtartalom beépítését.
Követelmények, user story-k és közös szabályok: [BACKLOG.md / E11](../BACKLOG.md#e11--a-konyv-alapu-tudastar-bovitese).

**Első indítható jegy: T-713**, a közös séma és specifikáció rögzítése.
A többi új jegy a T-713-tól induló függőségi láncon keresztül követi.
A korábbi T-709/T-710 hangos feladatok ettől változatlanul blokkoltak.

| Task | Cím | Függ |
|---|---|---|
| [T-713](T-713-tudastar-v2-specifikacio-es-migracios-szerzodes.md) | Tudástár v2: specifikáció, példarekordok és migrációs szerződés | — |
| [T-714](T-714-knowledge-entry-tipusos-domain-es-forrasmodell.md) | KnowledgeEntry: típusos domain, kapcsolatok és forrásmodell | T-713 |
| [T-715](T-715-tudastar-v2-konverter-es-importvalidacio.md) | Tudástár v2: szerkesztési formátum, konverter és validáció | T-714 |
| [T-716](T-716-knowledge-entry-room-migracio-es-repository.md) | KnowledgeEntry: Room-migráció és repositoryk | T-715 |
| [T-717](T-717-meglevo-tanulas-es-kviz-kompatibilitas.md) | Meglévő tanulás és név-/képkvízek átvezetése | T-716 |
| [T-718](T-718-cikkek-szemelyek-es-forrasok-tudastar-ui.md) | Cikkek, személyek és források megjelenítése a Tudástárban és Tanulásban | T-717 |
| [T-719](T-719-verziozott-tananyag-es-vizsgakovetelmeny.md) | Verziózott tananyagok és övvizsga-követelmények tárolása | T-716 |
| [T-720](T-720-ovvizsga-felkeszules-szures-es-tanulocsomag.md) | Övvizsga-felkészülés: tananyagválasztás és tanulócsomag | T-719, T-718 |
| [T-721](T-721-szerkesztett-kerdesek-adatmodell-es-import.md) | Szerkesztett tudás- és helyzetkérdések modellje és importja | T-715, T-716 |
| [T-722](T-722-vegyes-kvizmotor-szerkesztett-kerdesekkel.md) | Vegyes kvízmotor: generált és szerkesztett kérdések | T-721, T-717 |
| [T-723](T-723-tudas-es-helyzetkviz-felulet.md) | Tudás- és helyzetkvízek felülete és újragyakorlása | T-722, T-718, T-724 |
| [T-724](T-724-tanulasi-celonkenti-haladas-es-reviziok.md) | Haladás tanulási célonként, kérdésrevízióval | T-722, T-719, T-717 |
| [T-725](T-725-konyv-terminologia-es-15-kategoria.md) | Könyv: terminológia, technikaváltozatok és 15 témakör | T-717 |
| [T-726](T-726-konyv-magyarazo-cikkek-szemelyek-es-kata-hatter.md) | Könyv: magyarázó cikkek, személyek és kataháttér | T-718, T-725 |
| [T-727](T-727-konyv-ovvizsgatananyag-strukturalt-feldolgozasa.md) | Könyv: 10–1. kyu és 1–3. dan tananyag feldolgozása | T-719, T-725 |
| [T-728](T-728-konyv-tudas-es-helyzetkerdesek-szerkesztese.md) | Könyv: tudás- és helyzetkérdések szerkesztése | T-721, T-726, T-727 |
| [T-729](T-729-forras-es-lektoralasi-munkafolyamat.md) | Forrás-, OCR- és szakmai lektorálási munkafolyamat | T-725, T-726, T-727, T-728 |
| [T-730](T-730-tudastar-v2-integracio-es-atadas.md) | Tudástár v2: integrációs ellenőrzés és átadás | T-720, T-723, T-724, T-729 |

### E11 tartalmi egyeztetés – pontosítás (2026-11-16)

[Könyv–Kyo szabály](../BOOK_CONTENT_POLICY.md): egyezésnél nincs új lektorálás;
kisebb eltérésnél a könyv az irányadó, új lektorálás nélkül; lényegi ellentmondásnál
célzott lektorálás szükséges. Az előzmény nélküli új tartalom külön besorolás.
A T-713…T-730 érintett jegyei ezt a döntést tartalmazzák; új jegy nem keletkezett.
