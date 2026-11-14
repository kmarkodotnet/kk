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

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-14 |
| Commit | lásd a következő commit üzenetét (`feat(navigation): …[T-704]`) |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `./gradlew.bat testDebugUnitTest assembleDebug ktlintCheck lint --console=plain` | 0 | `BUILD SUCCESSFUL`; 648 teszt összesen, 0 hiba; `navigation` lint-jelentése üres |

**Megjegyzés:** az első futtatás `java.io.EOFException`-nel bukott a Kotlin inkrementális
fordító és a `:app:testDebugUnitTest` gyorsítótárában – `gradlew --stop`, majd egy
`--rerun-tasks` futás helyreállította, utána a megszokott (gyorsítótárazott) parancs is
zölden ment. Nem kódhiba volt; lásd `docs/CONVENTIONS.md` 7. szakaszát a hasonló,
korábban is dokumentált Gradle cache-problémákról.

## Teendők és készültségi feltételek

- [x] `BottomNavDestination` enum sorrendjének átírása: `HOME`, `LEARN`, `QUIZ`,
  `PROGRESS`, `LIBRARY` (nincs külön "sorrend" mező – az `entries` deklarációs
  sorrendje maga a UI sorrend, ahogy `KyokushinNavHost.kt`
  `BottomNavDestination.entries.forEach`-ja is bejárja).
- [x] A `selectedFor(current: KyokushinScreen?)` logika és a `NavSection`
  hozzárendelések változatlanok (`NavSection`/`KyokushinScreen` enumot nem érintette
  a task – csak a `BottomNavDestination` deklarációs sorrendje változott).
- [x] `NavigationGraphTest` frissítve: `bottomNav_hasTheFiveSpecifiedDestinationsInOrder_NFR03_FR37_FR38`
  már az új sorrendet (`home, learn/setup, quiz/setup, progress/overview,
  library/list`) várja.
- [x] A "Tudástár részletezőjéből visszalépve a Tudástár fül marad kijelölve" szabályt
  a `bottomNav_keepsTheOwningTabSelectedOnDeeperScreens_NFR03` JVM teszt már bizonyítja
  (`selectedFor` a `NavSection`-ön egyezik, nem a pozíción, ezért a sorrendváltás nem
  érinti) – **nem futott külön emulátoros/manuális próba**, mert a task saját `Teszt`
  mezője csak a `NavigationGraphTest`-et írja elő, és ez a meglévő teszt pontosan ezt a
  szabályt fedi.
- [x] `./gradlew testDebugUnitTest assembleDebug` zöld (648 teszt, 0 hiba).
- [x] `docs/IMPLEMENTATION_PLAN.md` bővítési szakaszának frissítése, commit a taskra
  hivatkozva.
