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
| T-709…T-711 | **nem** | hangos kiejtés, kiejtés-alapú kvíztípus, többnyelvűsítés – a „Függ" mezőjük szándékosan egy nyitott kérdésre (`OQ-05`/`OQ-06`/`OQ-07`) mutat, nem egy elvégezhető taskra, mert a mesterprompt 9/10. pontja MOST kizárja őket (`BACKLOG.md` E9) |

A `/dev`/`/devall` a megszokott, növekvő azonosító szerinti, függőségellenőrzött
szabályt követi – a T-709…T-711 ennek megfelelően **megáll és blokkoltként jelzi
magát**, nem próbálja implementálni a kizárt funkciót.

A tartalmi lektorálás és az ismert, külön fejlesztési ügyek az
[elfogadási jegyzőkönyvben](../ACCEPTANCE.md) és a
[konvenciók 8. szakaszában](../CONVENTIONS.md) szerepelnek; ezek nem megoldottként
jelentett feladatok.

## Kapcsolódó

- [`../BACKLOG.md`](../BACKLOG.md) – epikek, user story-k elfogadási kritériumokkal, a globális DoR/DoD és a mérföldkövek
- [`../IMPLEMENTATION_PLAN.md`](../IMPLEMENTATION_PLAN.md) – fázisok és a projekt tényleges állása
- [`../done/`](../done/) – az elkészült feladatok
