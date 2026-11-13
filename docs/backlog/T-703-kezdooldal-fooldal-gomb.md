# T-703 · Kezdőoldal (Főoldal) a logóval + navigáció bekötése

| | |
|---|---|
| Modul | navigation, feature/home (új modul) |
| Függ | T-701 |
| Követelmény | FR-37 (`docs/REQUIREMENTS.md` 3.6) |
| Teszt | ViewModel/navigáció teszt (ha van állapot), `@Preview` világos/sötét |

Jelenleg nincs önálló kezdőoldal: az app indítási útvonala egyenesen a Tudástár
listájára mutat (`KyokushinScreen` dokumentációja szerint). A felhasználó egy üres,
nyugodt Kezdőoldalt kér: a kandzsi logó középen, alatta az alsó navigáció, aminek a bal
szélén egy "Főoldal" gomb van.

## Teendők és készültségi feltételek

- [ ] Új `feature/home` modul (a többi `feature/*` modul mintájára, a rétegszabály
  betartásával – nem lát Room entitást/DAO-t/DTO-t).
- [ ] `KyokushinScreen`-be új route (pl. `home`), ez lesz az **induló útvonal** a jelenlegi
  Tudástár lista helyett.
- [ ] `BottomNavDestination`-be új `HOME` elem, **bal szélen** (a tényleges sorrend-csere a
  Tudástárral T-704-ben történik; ez a task csak hozzáadja a Főoldal gombot a sor
  elejére, a meglévő sorrendet egyelőre megtartva: Főoldal, Tudástár, Tanulás, Kvíz,
  Haladás).
- [ ] A Kezdőoldal tartalma: a T-701 logó középen, alatta/körülötte **nincs** más
  tartalom (szándékosan üres, a FUNCTIONAL_SPEC jelenlegi 4 funkciója a navigációból
  érhető el, nem a Kezdőoldal törzséből).
- [ ] Bármelyik más fülről a Főoldal gombra koppintva ide jut vissza a felhasználó,
  `launchSingleTop` + a navigációs verem megfelelő kezelésével (a meglévő
  `navigateToBottomNavDestination` mintáját követve).
- [ ] Forgatás: a Kezdőoldal állapotmentes, forgatás után is ugyanazt mutatja.
- [ ] `contentDescription` a logóra, érintési célpont ≥ 48 dp a Főoldal gombra.
- [ ] `@Preview` világos és sötét témára.
- [ ] `NavigationGraphTest` bővítése az új induló útvonalra és a Főoldal
  navigációra.
- [ ] `./gradlew testDebugUnitTest assembleDebug` zöld.
- [ ] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
