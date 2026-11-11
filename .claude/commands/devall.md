---
description: Soros fejlesztési hurok – egyszerre pontosan egy subagent futtatja a /dev parancsot, amíg a docs/backlog mappa ki nem ürül.
allowed-tools: Agent, Bash, PowerShell, Read, Grep, Glob, Edit, Write
---

A `docs/backlog/` feladatainak sorozatos végigvitele. **Minden feladatot egy külön subagent
visz végig, és egyszerre pontosan egy subagent fut.** A főszál vezényel: elindít egyet,
megvárja, ellenőrzi az eredményt, majd elindítja a következőt.

**Argumentum (opcionális):** `$ARGUMENTS`
- szám (pl. `3`) → legfeljebb ennyi feladatot vigyen végig ebben a hívásban
- üres → fut, amíg a `docs/backlog/` ki nem ürül, vagy amíg egy megállási feltétel be nem lép

---

## 0. Indulás előtti ellenőrzés

Futtasd le, és csak akkor kezdd el a hurkot, ha mind rendben:

1. **Tiszta munkafa:** `git status --short`. Ha van nem commitolt változás, **állj meg**,
   és kérdezd meg a felhasználót, mi legyen vele. A hurok commitol, ezért nem keverheti
   össze a saját munkáját a meglévővel.
2. **Van feladat:** `ls docs/backlog/T-*.md | wc -l` > 0. Ha 0, jelentsd, hogy nincs
   hátralévő feladat, és állj meg.
3. **Build környezet él:** `java -version` és `./gradlew --version`. Ha nem, **állj meg**:
   a hurok teszt nélkül nem tud Definition of Done-t ellenőrizni. Javasold a
   `/kyo-setup-env` parancsot.

Írd ki induláskor: hány feladat van a backlogban, és mi a hurok határa (szám vagy „amíg ki nem ürül").

---

## 1. A hurok

Ismételd, amíg valamelyik megállási feltétel be nem lép.

### 1.1 A következő feladat (vagy csoport) meghatározása

Olvasd ki a `docs/backlog/` `T-*.md` fájljait növekvő azonosító szerint, és a fejlécük
`| Függ |` mezőjéből a függőségeket. A soron következő az **első olyan, amelynek minden
függősége már a `docs/done/` mappában van**.

- Ha nincs ilyen → **megállás: `DEADLOCK`**. Írd ki, melyik feladat melyik hiányzó
  függőségre vár.

**Ezután nézd meg a [`docs/backlog/GROUPS.md`](../../docs/backlog/GROUPS.md) fájlt.**
Ha a kiválasztott feladat egy csoport **első** tagja, és a csoport **minden** tagjának
minden külső függősége teljesül, akkor **az egész csoportot** add át egy subagentnek,
a GROUPS.md-ben megadott sorrendben. Ezek a feladatok ugyanazon a fájlkészleten dolgoznak,
ezért egy menetben alig kerülnek többe, mint egy.

Ha a csoport bármelyik tagjának függősége hiányzik, a csoport **nem** indul: akkor csak az
egyedi feladat megy.

Jegyezd fel az érintett feladatok azonosítóját és fájlnevét – ezeket fogod ellenőrizni.

### 1.1b Modellválasztás

Nem minden feladat igényel ugyanakkora gondolkodást. Az `Agent` eszköz `model` paraméterével
válassz:

| Modell | Mikor |
|---|---|
| `sonnet` | **`S` méretű, nem helyesség-kritikus** feladat: egyszerű UI-szakasz, egy törlő metódus, egy manifest-teszt, egy konfigurációs lépés. |
| (alapértelmezett) | **Minden más.** Különösen: kvízhelyesség, állapotgép, adatbázis-séma és migráció, akadálymentesség, tartalmi integritás, architektúra. |

**Csoport esetén a legigényesebb tag dönt:** ha bármelyik tag helyesség-kritikus, az egész
csoport az alapértelmezett modellen megy.

Ha kétséged van, válaszd az alapértelmezettet. Egy elrontott kvízszabály drágább, mint a
megspórolt tokenek.

### 1.2 Egyetlen subagent indítása

Indíts **pontosan egy** subagentet az `Agent` eszközzel:
- `subagent_type`: `general-purpose`
- `model`: az 1.1b szerint (`sonnet` vagy elhagyva)
- `description`: `dev <task-id>` (csoportnál `dev <első>…<utolsó>`)
- `prompt`: lásd lent

**Soha ne indíts másodikat, amíg az első fut.** Nem indítasz több feladatra egyszerre,
akkor sem, ha a függőségek megengednék – a felhasználó soros végrehajtást kért.

A subagentnek adott prompt:

```
Te egy fejlesztő subagent vagy a Kyokushin Karate Trainer projektben
(F:\Claude\kyo). Pontosan azt a feladatot (vagy feladatcsoportot) kell
végigvinned, amit itt megkaptál – se többet, se kevesebbet.

1. Olvasd be a CLAUDE.md fájlt – ez tartalmazza a projekt vasszabályait.
2. Olvasd be a docs/CONVENTIONS.md fájlt – ez a felhalmozott gyakorlati tudás
   (domain-, adatbázis-, teszt- és Compose-konvenciók, ismert fordítási csapdák).
   Ne fedezd fel újra, ami már le van írva benne.
3. Hajtsd végre a `dev` skillt (Skill eszköz, skill: "dev") a(z) <TASK-ID>
   feladatra: args: "<TASK-ID>". A skill leírja a teljes menetet.
   (Csoportnál az args az összes azonosító, szóközzel elválasztva, a megadott sorrendben.)
4. Ne indíts további subagentet. Magad végezd el a munkát.
5. Ne kezdj bele olyan feladatba, amit nem kaptál.
6. Ha új konvenciót vagy buktatót fedezel fel, írd hozzá a docs/CONVENTIONS.md-hez.

Kötelező zárójelentés – pontosan ezekkel a mezőkkel, a végén:

TASK: <task-id> (csoportnál mindegyik, vesszővel)
ALLAPOT: KESZ | BUKOTT | BLOKKOLT   (csoportnál csak akkor KESZ, ha MIND az)
ATMOZGATVA: igen | nem        (minden érintett fájl a docs/done/ mappában van-e)
COMMIT: <rövid hash vagy "nincs">
TESZT: <pontos parancs> -> exit <kód>, <teszt/hiba szám vagy "NO-SOURCE">
BUILD: <pontos parancs> -> exit <kód>
INDOK: <ha nem KESZ: mi hiányzik, egy-két mondatban>
MEGJEGYZES: <amit a következő feladat előtt tudni kell, vagy "nincs">

Soha ne írj zöld tesztet vagy buildet tényleges, nullás exit kódú futtatás
nélkül. Ha a Definition of Done bármelyik pontja nem teljesül, a feladat
MARAD a docs/backlog mappában, és az ALLAPOT nem KESZ.
```

A `<TASK-ID>` helyére az 1.1-ben meghatározott azonosító kerül – csoportnál az összes,
szóközzel elválasztva, a GROUPS.md sorrendjében. Add át azt is, hogy ez egy csoport,
és hogy **mindegyik** tag Definition of Done-jának teljesülnie kell, különben egyik sem
mozdul.

### 1.3 Várakozás

Várd meg a subagent befejezését. **Ne csinálj közben mást**, és semmiképp ne indíts újabb
agentet. A befejezésről értesítést kapsz – amíg az meg nem érkezik, a feladat fut.

Soha ne találd ki és ne vetítsd előre egy futó subagent eredményét. Ha a felhasználó
közben rákérdez, mondd meg, hogy még fut.

### 1.4 Az eredmény független ellenőrzése

A subagent jelentése **állítás**, nem bizonyíték. Ellenőrizd magad:

```
ls docs/done/<fájlnév>        # tényleg átkerült?
ls docs/backlog/<fájlnév>     # tényleg eltűnt a backlogból?
git log --oneline -1          # tényleg van commit?
git status --short            # maradt-e szemét a munkafában?
```

- **Ha minden érintett fájl átkerült és van commit** → a feladat (csoport) kész. Írj egy egysoros összefoglalót
  (task-azonosító, cím, teszt- és builderedmény), majd folytasd a hurkot.
- **Ha bármelyik fájl a backlogban maradt** → a feladat nem készült el. **Megállás: `TASK_FAILED`.**
- **Ha a jelentés KESZ-t mond, de a fájl nem mozdult** (vagy fordítva) → **megállás:
  `INCONSISTENT`**. Ez komolyabb jel, mint egy bukott task: a jelentés nem egyezik a
  valósággal. Írd le pontosan, mit állított a subagent és mit találtál.
- **Ha maradt nem commitolt változás** → **megállás: `DIRTY_TREE`**. Ne commitold te.

### 1.5 Számláló és folytatás

Növeld az elvégzett feladatok számlálóját, és menj vissza az 1.1 pontra.

---

## 2. Megállási feltételek

A hurok leáll, ha bármelyik teljesül:

| Kód | Mikor | Mit tegyél |
|---|---|---|
| `EMPTY` | a `docs/backlog/` nem tartalmaz több `T-*.md` fájlt | sikeres befejezés, zárójelentés |
| `LIMIT` | elérted a `$ARGUMENTS`-ben megadott darabszámot | zárójelentés, és írd ki, hány feladat maradt |
| `DEADLOCK` | nincs olyan feladat, amelynek a függőségei teljesülnek | írd ki, mi vár mire |
| `TASK_FAILED` | a feladat a backlogban maradt | írd ki a subagent `INDOK` mezőjét |
| `INCONSISTENT` | a jelentés és a fájlrendszer/git állapot nem egyezik | írd le mindkettőt |
| `DIRTY_TREE` | a subagent után nem commitolt változás maradt | listázd a `git status --short` kimenetét |
| `ENV` | a build vagy a teszt környezeti okból nem futtatható | idézd a hibát, javasold a `/kyo-setup-env`-et |

**Hiba esetén a hurok megáll, nem próbálkozik újra.** Egy bukott feladat általában
specifikációs vagy környezeti problémára utal, amit emberi döntés old meg – az újrapróbálás
csak elfedné. Ha a felhasználó mást akar, külön kérje.

---

## 3. Zárójelentés

A hurok végén, magyarul, tényszerűen:

1. **Megállás oka** a 2. pont kódjával, egy mondatban kifejtve.
2. **Elkészült feladatok** táblázata: task-azonosító, cím, teszteredmény, commit hash.
3. **Nem futott le, ami** – ha valami kimaradt (pl. UI teszt emulátor hiányában).
4. **Hátralévő munka**: hány feladat maradt a `docs/backlog/`-ban, és mi a következő.
5. **Figyelemre méltó megjegyzések** a subagentek `MEGJEGYZES` mezőiből összegyűjtve –
   különösen a tervtől való eltérések és a javasolt új taskok.

A subagentek jelentése a felhasználó számára **nem látszik**, ezért minden lényeges
információt neked kell átadnod. Ne hagyd ki azt sem, ami kedvezőtlen.
