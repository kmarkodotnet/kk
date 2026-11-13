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

**Nincs nyitott számozott feladat.** A T-604, T-605, T-606, T-610 és T-611
2026-11-13-án, a sikeres ellenőrzések után átkerült a [`../done/`](../done/) mappába.

A tartalmi lektorálás és az ismert, külön fejlesztési ügyek az
[elfogadási jegyzőkönyvben](../ACCEPTANCE.md) és a
[konvenciók 8. szakaszában](../CONVENTIONS.md) szerepelnek; ezek nem megoldottként
jelentett feladatok.

## Kapcsolódó

- [`../BACKLOG.md`](../BACKLOG.md) – epikek, user story-k elfogadási kritériumokkal, a globális DoR/DoD és a mérföldkövek
- [`../IMPLEMENTATION_PLAN.md`](../IMPLEMENTATION_PLAN.md) – fázisok és a projekt tényleges állása
- [`../done/`](../done/) – az elkészült feladatok
