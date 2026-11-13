# T-704 · Alsó navigáció sorrendje – a Tudástár a legjobboldali menüpont

| | |
|---|---|
| Modul | navigation |
| Függ | T-703 |
| Követelmény | FR-38 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | `NavigationGraphTest` |

A T-703-mal a Főoldal bekerül a sor elejére, a Tudástár egyelőre a 2. helyen marad. Ez a
task véglegesíti a kért sorrendet: **Főoldal, Tanulás, Kvíz, Haladás, Tudástár** – azaz a
Tudástár a sor **végére** kerül, minden más eggyel balra csúszik.

## Teendők és készültségi feltételek

- [ ] `BottomNavDestination` enum sorrendjének átírása: `HOME`, `LEARN`, `QUIZ`,
  `PROGRESS`, `LIBRARY` (az `entries` bejárási sorrend adja a UI sorrendjét, a meglévő
  `KyokushinNavHost.kt` `BottomNavDestination.entries.forEach` mintája szerint – nincs
  szükség külön "sorrend" mezőre, ha az enum deklarációs sorrendje maga a UI sorrend;
  ha van külön `order`/`sortOrder` mező, azt is frissíteni kell).
- [ ] A `selectedFor(current: KyokushinScreen?)` logika és a `NavSection` hozzárendelések
  változatlanok maradnak (csak a megjelenítési sorrend változik, a szekció-hovatartozás
  nem).
- [ ] `NavigationGraphTest` frissítése: a sorrendre vonatkozó asserció (ha van ilyen) az
  új sorrendet várja.
- [ ] Manuális/eszközteszt-szintű ellenőrzés, hogy a Tudástár részletezőjéből
  visszalépve továbbra is a Tudástár fül marad kijelölve (FUNCTIONAL_SPEC szerint
  változatlan szabály, csak más pozícióban).
- [ ] `./gradlew testDebugUnitTest assembleDebug` zöld.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
