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

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | lásd a következő commit üzenetét (`feat(navigation): …[T-703]`) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest assembleDebug --console=plain` | 0 | `BUILD SUCCESSFUL`, 648 teszt összesen, 0 hiba; `:feature:home:assembleDebug` is lefutott |
| `./gradlew.bat ktlintCheck lint --console=plain` | 0 | `BUILD SUCCESSFUL`; `feature/home` és `navigation` lint-jelentése is üres (0 találat) |

**Megjegyzés:** lásd a fenti "Megosztott logó-komponens" és "A munka közben…" bekezdést
– a `KyoKanjiMark` kiemelése a `core/ui`-ba, és a tőlem független, párhuzamosan
elkészült filmes intro (nincs ütközés, nem ennek a tasknak a tárgya).

## Teendők és készültségi feltételek

- [x] Új `feature/home` modul (a többi `feature/*` modul mintájára, a rétegszabály
  betartásával – nem lát Room entitást/DAO-t/DTO-t). Minimális: nincs ViewModel/Hilt,
  mert nincs megfigyelendő állapot és nincs repository-függőség – csak
  `core/ui`-ra és Compose-ra épül.
- [x] `KyokushinScreen`-be új `HOME` route (`"home"`), ez lett az **induló útvonal**
  (`startRoute`) a korábbi Tudástár lista helyett.
- [x] `BottomNavDestination`-be új `HOME` elem, **bal szélen** – a sorrend egyelőre
  Főoldal, Tudástár, Tanulás, Kvíz, Haladás (a tényleges Tudástár-jobbra-tolás T-704
  tárgya marad).
- [x] A Kezdőoldal tartalma: kizárólag a logó középen, körülötte nincs más tartalom
  (`HomeScreen` – `Box` + `KyoKanjiMark`).
- [x] Bármelyik fülről a Főoldal gombra koppintva ide jut vissza a felhasználó – a
  meglévő `navigateToBottomNavDestination` függvény változatlanul minden
  `BottomNavDestination`-re (így `HOME`-ra is) fut, nem kellett külön eset.
- [x] Forgatás: a Kezdőoldal állapotmentes (nincs `ViewModel`, nincs `remember`-elt
  mutálható állapot), forgatás után is ugyanazt mutatja.
- [x] `contentDescription` a logóra, érintési célpont ≥ 48 dp a Főoldal gombra – az
  előbbi a megosztott `KyoKanjiMark` (`core/ui`) felelőssége, az utóbbi a
  `NavigationBarItem` Material 3 alapértelmezése (ugyanaz, mint a többi fülnél).
- [x] `@Preview` világos és sötét témára – `HomeScreenPreview`.
- [x] `NavigationGraphTest` bővítve: `navGraph_startsOnTheHomeScreen_FR37`,
  `bottomNav_hasTheFiveSpecifiedDestinationsInOrder_NFR03_FR37`, és a teljes
  route-lista asszerció is tartalmazza a `"home"`-ot.
- [x] `./gradlew testDebugUnitTest assembleDebug` zöld (648 teszt, 0 hiba).
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.

**Megosztott logó-komponens (eredetileg nem volt a Teendők között, de a feladat
elvégzése közben szükségessé vált):** a T-701 splash-logója az `app` modul saját
drawable-je volt – egy `feature/*` modul viszont **nem** függhet az `app`-ra (ADR-001,
L5 fordított iránya tiltott), így a Kezdőoldal nem érhette volna el ugyanazt a képet.
A drawable és a leírás átkerült a `core/ui`-ba (`KyoKanjiMark` komponens), a splash és
a Kezdőoldal is ezt hívja – egy hely, nem duplikált kép/szöveg. A `tools/trace_kanji.py`
kimeneti útvonala is frissítve erre, hogy a jövőbeli újravektorizálás ne hozzon létre
egy használaton kívüli másolatot az `app` modulban.

**A munka közben, tőlem függetlenül, egy külön felhasználói kérésre egy 14 mp-es
filmes indítóanimáció is elkészült** (`AnimatedIntroScreen`, `docs/CINEMATIC_INTRO.md`)
– ez a `KyokushinApp`-ot a splash elé állítja, de a Kezdőoldal útvonalát, a
`BottomNavDestination` sorrendjét és a `KyoKanjiMark` komponenst változatlanul hagyta
(a saját dokumentációja is rögzíti: "A meglévő T-703 … állapota ettől nem változik").
Nincs ütközés, a két munka összhangban fut; ez a task nem vállal felelősséget az
intróért, és nem is nyúlt hozzá.
