# Vizuális arculat – tervjegyzet

| | |
|---|---|
| Verzió | 1.0 |
| Dátum | 2026-11-13 |
| Alapja | felhasználói visszajelzés ("nagyon úgy néz ki, mint egy webes űrlap"), `docs/backlog/T-702` |
| Hatókör | **csak döntések és konkrét érték-javaslatok** – ez a dokumentum nem tartalmaz Compose-kódot. A tényleges `core/ui` téma-kód egy **későbbi** taskban követi (lásd a 8. szakaszt). |

Ez a jegyzet a `docs/CONVENTIONS.md`-ből linkelt, felhalmozott tudás egy tagja: a
*miért* ilyen a mai megjelenés, és a *mit* változtatunk rajta tartozik ide. A *hogyan*
(a tényleges Kotlin-kód) a T-705/T-706 és egy önálló `core/ui` taskban (lásd 8. szakasz)
készül el.

---

## 1. Diagnózis: miért hat "webes űrlapnak" a mai megjelenés

Konkrét, a kódban visszakereshető okok – nem benyomás, hanem tényleges hiány:

| Jelenség | Hol látszik | Miért ad "generikus form" hatást |
|---|---|---|
| **Nincs egyedi `Typography`** | `core/ui/.../theme/Theme.kt` a `MaterialTheme(colorScheme = …, content = …)`-ot hívja – **nincs `typography` paraméter**. | A teljes app a Material 3 **alapértelmezett** Roboto típusskáláját kapja, változtatás nélkül. A romaji címek (`headlineSmall`/`titleLarge`) és a magyar jelentés (`bodyMedium`/`bodyLarge`) vizuálisan **ugyanúgy** néz ki, mint egy admin-felület bármelyik címe/törzsszövege – nincs olyan tipográfiai jel, ami a kettőt (japán fogalom vs. magyar jelentés) megkülönböztetné. |
| **Nincs egyedi `Shapes`** | Ugyanott – nincs `shapes` paraméter a `MaterialTheme`-nek. | Nyolc különböző fájl (`QuizResultScreen`, `QuizRunScreen` ×3, `ProgressTechniqueScreen`, `ProgressDiagnosticsScreen`, `ProgressOverviewScreen` ×2, `ProgressSettingsScreen` ×2, `LearnCardsScreen` ×2) **egymástól függetlenül** írja ki ugyanazt a `RoundedCornerShape(12.dp)` (vagy `CARD_CORNER_RADIUS`) literált – nincs egyetlen, megnevezett "ez a mi kártyaformánk" döntés, csak nyolcszor megismételt véletlen egyezés. |
| **`OutlinedTextField` a keresőn** | `feature/library/.../LibraryListScreen.kt:195` | Az `OutlinedTextField` a Material Design webes/admin-form mintázatának a legfelismerhetőbb eleme – szegélyezett doboz, lebegő címke. Pontosan ez az elem adja a felhasználó "webes űrlap" benyomásának nagy részét. |
| **`FilterChip` kategóriaszűrőn** | `feature/library/.../LibraryListScreen.kt:239, 249` | Önmagában nem hiba (a Material chip a helyes komponens), de az alapértelmezett, lekerekített, szegélyezett megjelenésében **semmi** nem köti az alkalmazás tárgyához – ugyanaz, mint bármely webes adminfelület szűrője. |
| **Nincs egyedi ikonográfia** | Az egész appban `androidx.compose.material.icons.*` generikus ikonok (`MenuBook`, `School`, `Quiz`, `BarChart` – lásd `navigation/.../KyokushinNavHost.kt`). | Az ikonok funkcionálisan helyesek, de semmi nem köti őket Kyokushinhoz – egy könyvtár- vagy oktatóalkalmazás ugyanezeket használná. |

**Amit ez a diagnózis *nem* mond:** hogy bármelyik komponens *hibás* – mindegyik a
`docs/CONVENTIONS.md` szabályai szerint épült (téma-token szín, 48 dp célpont,
akadálymentesség). A "webes űrlap" benyomás nem egy hibából jön, hanem abból, hogy a
**márka-réteg** (típusskála, forma, ikonográfia) sosem készült el – a Fázis 1 óta
mindenhol a Material 3 gyári alapértelmezés fut.

---

## 2. Irányelv és korlát: CR-08

> `docs/REQUIREMENTS.md` CR-08: *"A dizájn kerüli a harcművészeti klisékkel túlterhelt
> megjelenést; tiszta, gyors, funkcionális UI."*

A felhasználói kérés ("legyen kyokushin-karatésabb") és a CR-08 ("ne legyen
harcművészeti-klisével túlterhelt") **nem mond ellent egymásnak** – csak kijelöli,
*hogyan* legyen karakteres a megjelenés: **visszafogott márkajelzéssel** (szín, forma,
tipográfia, a már vektorizált logó), **nem** díszítő motívumokkal (lásd 7. szakasz, mit
NEM szabad). A cél egy nyugodt, "dojo"-jellegű felület, nem egy harcművészeti
posztertervezés.

---

## 3. Szín paletta javaslat

A jelenlegi `LightScheme.primary` (`#1C3D5A`) és a T-701 splash logójának színe
(`#17317A`, a felhasználó saját gi-fotójából vektorizálva) **majdnem**, de nem pontosan
egyezik – két, egymástól függetlenül választott sötétkék. Javaslat: **a logó legyen az
igazságforrás**, a téma ehhez igazodjon, ne fordítva.

| Token | Jelenlegi (világos) | Javasolt (világos) | Indoklás |
|---|---|---|---|
| `primary` | `#1C3D5A` | **`#17317A`** | Pontosan a splash-logó kék színe (T-701) – a márka egyetlen, végig következetes kék tónusa. |
| `onPrimary` | `#FFFFFF` | `#FFFFFF` (változatlan) | A T-701 `SplashContrastTest` már bizonyította: fehér a `#17317A`-n ≈ **11,95:1** – jóval a WCAG AA 4,5:1 fölött, nem kell újraszámolni. |
| `primaryContainer` | `#D3E4F5` | `#D9E2F7` | Finoman a `#17317A` felé tolva (semleges kékesszürke helyett a márka tónusába hajló), de a kontraszttesztet (`ThemeContrastTest`) a tényleges kódolás taskjában újra kell futtatni. |
| `secondary` | `#4F616E` | változatlan | Nincs ok a cserére – semleges, funkcionális, nem "forma"-hatású. |
| `surface` | `#FCFCFF` | változatlan | A splash-logóval megegyező, majdnem-fehér alapfelszín már ma is a kívánt "tiszta, nyugodt" hatást adja. |

Sötét témára (`DarkScheme`): a `primary` (`#A2C9EA`) már most is a `#17317A` hue-jának
világos tónusa – **ezen nem szükséges módosítani**, csak a világos téma `primary`-jét
kell a logóhoz igazítani, hogy a kettő egy konzisztens kékskála két vége legyen.

**A tényleges hex-értékek és a WCAG-újramérés a 8. szakaszban hivatkozott taskban dől
el** – ez a szakasz az *irányt* és a *forrást* (a logó) rögzíti, nem egy véglegesített
palettát.

---

## 4. Tipográfia javaslat

**Nincs új betűtípus-függőség javasolva** – a Roboto (Material 3 alapértelmezett)
marad, csak a **szerepkiosztás** válik explicitté és következetessé:

| Szerep | Javasolt `MaterialTheme.typography` szerep | Súly | Hol |
|---|---|---|---|
| Romaji/japán fogalom (elsődleges cím) | `titleLarge` / `headlineSmall` | **Bold** (`FontWeight.Bold`, nem az alapértelmezett `Medium`) | Tudástár lista/részletező, tanulókártya, kvízkérdés – mindenhol, ahol a romaji a főszereplő. |
| Magyar jelentés (alárendelt) | `bodyLarge` / `bodyMedium` | `Normal` | Közvetlenül a romaji alatt, `onSurfaceVariant` színnel (nem `onSurface`) – ez már ma is sokhol így van színben, a súlykülönbség hiányzik. |
| Kategórianév (chip, cím) | `labelLarge` | `Medium` | Változatlan – ez már ma is jól elkülönül. |

**Lényeg:** a romaji és a magyar jelentés **ugyanakkora betűmérettel**, csak eltérő
súllyal/színnel is elég különbséget adna – nem kell drámaian megnövelni egyiket sem
(az akadálymentességi 48 dp és a nagy rendszerbetűméret-teszt, T-605, nem sérülhet).

**Elvetett ötlet (jóváhagyás nélkül NEM indítható):** egy dedikált japán betűtípus (pl.
Noto Sans JP) a `japaneseScript` mező megjelenítéséhez. Ez új betűtípus-erőforrást
(fájlt) igényelne – a `CLAUDE.md` "nincs korai bővítés" és "ne találj ki
verziószámot/függőséget" szabálya miatt ez egy **külön, jóváhagyást igénylő** jövőbeli
ötlet, nem ennek a tervnek a része (ugyanaz a mintázat, mint a T-701 natív
`SplashScreen` API döntésénél).

---

## 5. Forma (`Shapes`) javaslat

Mivel **nyolc fájl** véletlenül, egymástól függetlenül ugyanazt a `12.dp` lekerekítést
írja ki (lásd 1. szakasz táblázata), a javaslat egyszerű: **egy megnevezett `Shapes`
objektum a témában**, amit ezek a helyek importálnak, ahelyett hogy újra leírnák:

| Token | Javasolt érték | Honnan jön |
|---|---|---|
| `shapes.small` | `4.dp` | Material 3 alapértelmezett – chipekhez, kis gombokhoz, változatlan. |
| `shapes.medium` | `12.dp` | A már **ma is** mindenhol használt, kézzel beírt érték – csak egyetlen helyre kerül. |
| `shapes.large` | `16.dp` | A tanulókártya (`LearnCardsScreen.CARD_CORNER_RADIUS`) kicsit nagyobb, "nyugodtabb" sarka – a kártya a legnagyobb, leghangsúlyosabb felület, ezért kap saját, valamivel lágyabb értéket. |

Ez **nem** vizuális változás (a `12.dp` már ma is mindenhol ez), hanem egy
architekturális rendrakás: a forma egyetlen helyen él, nem nyolc helyen véletlenül
egyező szám formájában – pontosan ugyanaz a hiba-kategória, amit a
`docs/CONVENTIONS.md` már a `quizEligible`/`NameNormalizer` egyetlen-igazságforrás
szabályánál dokumentál, csak itt vizuális tokenre alkalmazva.

---

## 6. Kategória-ikonográfia iránya (T-705 előkészítése)

A T-705 (Tanulás: kategória-ikonok checkbox helyett) ehhez a döntéshez igazodjon:

- **Egységes vonalvastagság és stílus** (pl. Material "outlined" ikonkészlet, nem
  kevert "filled"/"outlined"), hogy a kategóriák ikonsorozata egységesnek hasson.
- **Absztrakt, nem szó szerinti** ikonok (pl. egy álló alak sziluettje "állások"
  kategóriához) **tilos**, ha nincs hozzá hiteles, ellenőrzött forrás (CLAUDE.md
  tartalmi integritás – egy kitalált "ez a technika ikonja" ugyanúgy hamis hiteles
  tartalom, mint egy kitalált japán kifejezés). Helyette **semleges, geometrikus**
  jelölők (pl. szám, kezdőbetű-monogram, vagy egy, a kategória *típusát* jelző, nem a
  *konkrét mozdulatot* ábrázoló ikon – pl. "irány" nyilak az állásoknál, nem egy
  rajzolt emberalak).
- Az ikon **ugyanaz** a Tudástár listáján és a Tanulás kategóriaválasztóján is
  megjelenhet (konzisztencia a két képernyő között) – ezt a T-705 Teendői között már
  rögzítettük.

---

## 7. Mit NEM szabad (CR-08 + CLAUDE.md tartalmi integritás)

Explicit, hogy a T-705/T-706 és a követő téma-task ne vezessen be semmi ilyet:

- Gi-szövet textúra vagy fénykép háttérként (a splash logó **az egyetlen** fotó-eredetű
  elem, és az is vektorizált, nem nyers kép).
- Öv-szín alapú rangsor-dekoráció (a projekt szándékosan **nem** kezel övfokot – lásd
  `docs/CONVENTIONS.md` dokumentált drift-sora, `Technique`-nek nincs `beltRank` mezője
  a kanonikus sémában).
- Harcművész-sziluett, ököl/rúgás-piktogram, "dojo"-klisé (tatami-minta, torii-kapu,
  stb.) díszítőelemként – ez pont a CR-08 által tiltott "túlterhelt" hatás.
- Bármilyen, nem a felhasználó saját anyagából (T-701 logó) vagy a tényleges tartalomból
  (verified mezők) származó, de hitelesnek ható japán írásjel vagy szimbólum.

---

## 8. Végrehajtási terv – mely taskok építenek erre

Ez a dokumentum **nem** zár le egyetlen kódváltozást sem. A tényleges megvalósítás:

| Task | Mit visz át ebből a tervből |
|---|---|
| **T-705** (kategória-ikonok) | A 6. szakasz iránya. |
| **T-706** (felső tab Japán/Magyar) | A 4. szakasz tipográfiai hangsúlya (a két fül tartalma kapja a romaji/magyar súlykülönbséget). |
| **T-712** (új, ezzel a taskkal egyszerre felvéve) | A 3. (szín) és 5. (forma) szakasz tényleges `core/ui` `Theme.kt` kódja: `Typography`/`Shapes` objektum, a nyolc duplikált `RoundedCornerShape(12.dp)` lecserélése `MaterialTheme.shapes.medium`-ra, `LightScheme.primary` igazítása, `ThemeContrastTest` újrafuttatása az új színekkel. |

A `T-712`-t a `docs/backlog/`-ba vettem fel, `Függ: T-702` (ez a dokumentum).
