# Funkcionális specifikáció – Kyokushin Karate Trainer

| | |
|---|---|
| Dokumentum | Funkcionális specifikáció |
| Verzió | 1.0 |
| Dátum | 2026-11-11 |
| Állapot | Jóváhagyásra vár |
| Alapja | `docs/REQUIREMENTS.md` v1.0 |
| Kapcsolódó | `docs/ARCHITECTURE.md` |

Ez a dokumentum azt írja le, **mit lát és mit tesz** a felhasználó: képernyőket, állapotokat,
folyamatokat, szövegeket és hibakezelést. A megvalósítás *hogyanját* az architektúraterv írja le.

---

## 1. Információs architektúra

```
MainActivity (egyetlen Activity)
└── NavHost
    ├── bottom nav: Tudástár   → library/list ──→ library/detail/{techniqueId}
    ├── bottom nav: Tanulás    → learn/setup   ──→ learn/cards?categories=..&hide=..
    ├── bottom nav: Kvíz       → quiz/setup    ──→ quiz/run/{sessionId} ──→ quiz/result/{sessionId}
    └── bottom nav: Haladás    → progress/overview ──→ progress/technique/{techniqueId}
                                                  └─→ settings (Előrehaladás törlése)
```

Bottom navigation négy célpont: **Tudástár**, **Tanulás**, **Kvíz**, **Haladás**.
A bottom nav a `quiz/run` és `quiz/result` képernyőn **elrejtve** marad, hogy a futó kvíz
ne legyen véletlenül elhagyható (kilépés explicit „Kvíz megszakítása" megerősítéssel).

---

## 2. Globális elemek

### 2.1 Demó szalag
Ha a betöltött manifest `datasetKind = "demo"`, minden képernyő tetején egy nem elutasítható,
alacsony kontrasztú információs szalag jelenik meg:

> **Demó adatkészlet.** A tartalom szakmai ellenőrzésre vár, nem tanulásra hitelesített.

Indok: CR-03. Ez nem hibaállapot, hanem tájékoztatás.

### 2.2 Hitelesítési jelölők
| Állapot | Jelölő a listán | Jelölő a részletnézetben |
|---|---|---|
| `verified = true` | nincs jelölő | nincs jelölő |
| `verified = false` | kis „Ellenőrzésre vár" chip | „Demó / ellenőrzésre vár" blokk, magyarázattal |
| hiányos rekord (validátor jelölte) | „Hiányos" chip | „Hiányos rekord" + a hiányzó mezők felsorolása |

### 2.3 Illusztráció megjelenítése
| Eset | Megjelenítés | `contentDescription` |
|---|---|---|
| validált kép van | a kép | „`{romaji}` – `{magyar név}` illusztrációja" |
| csak placeholder | semleges helyőrző ikon + „Ábra készül" | „Ehhez a technikához még nincs ábra" |
| kvíz (D) típus opciói | a kép | „`{n}`. válaszlehetőség illusztrációja" – **a technika nevét nem tartalmazza** |

A kvízopcióknál a leírás nem fedheti fel a helyes választ (NFR-04).

### 2.4 Üres és hibaállapotok (globális minta)
Minden lista- és tartalomképernyő négy állapotot ismer: `Loading`, `Empty`, `Content`, `Error`.
Az `Empty` mindig **konkrét, cselekvésre hívó** magyar üzenetet ad, nem csak egy üres listát.

---

## 3. Tudástár

### 3.1 Lista képernyő (`library/list`)

**Felépítés (felülről lefelé)**
1. Cím: „Tudástár".
2. Keresősáv (`SearchBar`), helykitöltő szöveg: „Keresés romaji vagy magyar név szerint".
   Törlés (×) ikon, ha van beírt szöveg.
3. Kategóriaszűrő chip-sor, vízszintesen görgethető. Minden chip: kategória magyar neve +
   találatszám. Többszörös választás. Egy „Összes" chip visszaállítja a szűrést.
4. Lista. Egy tétel:
   - bal oldalon kis illusztráció (vagy helyőrző), 56×56 dp,
   - fő sor: **romaji név** (félkövér),
   - alsó sor: magyar név,
   - jobb oldalon a hitelesítési chip, ha van.
   - A teljes sor kattintható, magassága ≥ 64 dp.
5. A lista rendezése: kategória `sortOrder`, azon belül romaji ábécé szerint.

**Keresés viselkedése (FR-03)**
- Illeszkedés: romaji, magyar név **és** aliasok, részszó-egyezéssel.
- Normalizálás: kisbetűsítés + diakritikus jelek eltávolítása mindkét oldalon,
  így „üraken" megtalálja az „Uraken"-t és „Ürakén"-t is.
- Ha alias alapján van találat, a tétel alatt megjelenik: „Egyezés: `{alias}`".
- Debounce ~200 ms, a keresés nem blokkolja a UI szálat.

**Állapotok**
| Állapot | Mikor | Mit lát a felhasználó |
|---|---|---|
| `Loading` | első betöltés / tartalomfrissítés | shimmer vagy körkörös jelző |
| `Empty` – nincs tartalom | a tudástár üres vagy minden rekord elvetve | „A tudástár üres. A tartalom betöltése nem sikerült." + debug buildben „Diagnosztika" gomb (FR-32) |
| `Empty` – nincs találat | keresés/szűrés nem ad eredményt | „Nincs találat a(z) „`{kifejezés}`" keresésre." + „Szűrők törlése" gomb |
| `Content` | van találat | lista |
| `Error` | betöltési hiba | „A tudástár betöltése nem sikerült." + „Újra" gomb |

**Állapotmegőrzés:** a keresőszöveg és a kiválasztott kategóriák `SavedStateHandle`-ben,
így túlélik a képernyőforgatást (FR-02).

### 3.2 Részletező képernyő (`library/detail/{techniqueId}`)

**Felépítés**
1. Vissza nyíl + a technika romaji neve a felső sávban.
2. Nagy illusztráció (vagy helyőrző), teljes szélesség, képarány megtartva.
3. **Romaji név** (nagy betű).
4. **Magyar név.**
5. Japán íráskép, ha van (`japaneseScript`) – külön, kisebb sorban.
6. Aliasok, ha vannak: „Más néven: …".
7. Kategória chip (kattintható: visszavisz a listára erre a kategóriára szűrve).
8. Magyarázat (`descriptionHu`), ha van.
9. Nehézség / övfok, ha van.
10. Hitelesítési blokk: státusz, és `verified = true` esetén a `source` megjelenítése.
11. Ha a felhasználó már gyakorolta: kis összegzés „Eddig: `{helyes}`/`{összes}` helyes válasz,
    utolsó gyakorlás: `{dátum}`" + link a technika haladás-részletezőjére.

**Hiányzó opcionális mező:** a szakasz nem jelenik meg (nincs „nincs adat" zaj).
**Hiányzó kötelező mező:** a rekord a validátor szerint hiányos, a képernyő a „Hiányos rekord"
blokkot mutatja, és a kvízből kizárt státuszt is jelzi.

---

## 4. Tanulás

### 4.1 Beállítás (`learn/setup`)

1. Cím: „Tanulás".
2. Kategóriaválasztás: többszörös választású lista jelölőkkel, mindegyiknél a kvízképes és
   összes rekord száma („12 technika").
3. Mit rejtsünk el? – két lehetőség közül egy (`RadioGroup`):
   - „Japán nevet rejtse el" (a magyar név és az ábra látszik),
   - „Magyar nevet rejtse el" (a romaji és az ábra látszik).
4. „Kártyák indítása" gomb. Letiltva, ha nincs kiválasztott kategória vagy a kiválasztásban
   nincs egyetlen megjeleníthető technika sem. A letiltás mellett magyarázó szöveg.
5. A beállítás megjegyzése a munkamenetek között (utolsó választás előhívása).

A tanuláshoz **nem** szükséges `quizEligible` vagy `verified` státusz: a felhasználó
böngészheti a demó tartalmat is, de a kártya mutatja a „Ellenőrzésre vár" jelölőt.

### 4.2 Kártyák (`learn/cards`)

**Felépítés**
1. Felső sáv: „`{aktuális}` / `{összes}`", bezárás (×), és a kategória neve.
2. Kártya (középen, nagy felület):
   - illusztráció (vagy helyőrző),
   - a **nem elrejtett** név nagy betűvel,
   - az elrejtett név helyén: „Koppints a felfedéshez" gomb/felület.
3. Felfedés után a kártyán **mind a három** elem látszik: romaji + magyar név + ábra (FR-08).
4. Önértékelés két gomb: **„Tudtam"** és **„Gyakorolnom kell"**. Csak felfedés után aktívak.
   Megnyomás után:
   - `StudyReview` rekord mentése (`selfRating = KNEW` / `NEEDS_PRACTICE`),
   - automatikus továbblépés a következő kártyára.
5. Alsó sáv: „Előző" / „Következő" gomb. A továbblépés önértékelés nélkül is megengedett
   (ekkor nem születik `StudyReview`).

**Viselkedési szabályok**
- A kártyasorrend a kiválasztott kategóriákon belül **seedelt keverés**, a munkamenet elején
  egyszer sorsolva; a sorrend és a pozíció `SavedStateHandle`-ben (FR-09).
- Felfedési állapot kártyánként tárolt: visszalépéskor a már felfedett kártya felfedve marad.
- Az utolsó kártya után záróképernyő: „Végigértél `{n}` kártyán." + „Újrakezdés" /
  „Vissza a beállításokhoz" / „Kvíz indítása ezekkel a kategóriákkal".
- Az önértékelés **soha nem** jelenik meg kvíz-sikerarányként (FR-28).

---

## 5. Kvíz

### 5.1 Konfiguráció (`quiz/setup`)

1. Cím: „Kvíz".
2. Kategóriaválasztás: többszörös választás; minden kategória mellett a **kvízképes**
   technikák száma („8 kvízképes / 12 technika").
3. Kérdésszám: szegmentált választó – **5 / 10 / 20**.
4. Kérdéstípusok (többszörös választás, jelölőkkel):
   - „Japán név → magyar jelentés" (A)
   - „Magyar név → japán név" (B)
   - „Ábra → japán név" (C)
   - „Japán név → ábra" (D)
   A (C) és (D) **letiltva**, ha nincs elegendő egyedi, validált illusztráció. A letiltott
   sor alatt indoklás: „Ehhez legalább 4 egyedi, ellenőrzött ábra kell a kiválasztott
   kategóriákban (jelenleg: `{n}`)." (FR-13)
5. Élő készenléti összegzés a konfiguráció alatt:
   - „A beállításokkal `{n}` kérdés állítható össze."
   - Ha `n < kért kérdésszám`: „A kért `{k}` helyett `{n}` kérdés lesz, mert nincs több
     megfelelő technika." (FR-22)
   - Ha `n = 0`: a „Kvíz indítása" gomb letiltva + konkrét javaslat
     („Válassz több kategóriát", „Engedélyezz több kérdéstípust", „Csökkentsd a kérdésszámot").
6. „Kvíz indítása" gomb.

A készenléti összegzés ugyanazt a motort használja „próbafutásra" (száraz generálás), amit
a valódi kvíz – így a beállítási képernyő soha nem ígér többet, mint ami előállítható.

### 5.2 Kérdés képernyő (`quiz/run/{sessionId}`)

**Felépítés**
1. Felső sáv: „`{index}` / `{összes}`" + lineáris előrehaladásjelző + bezárás (×).
2. Kérdésblokk, típus szerint:
   | Típus | Kérdésblokk | Opciók |
   |---|---|---|
   | A | „Mit jelent: **`{romaji}`**?" | 4 szöveges gomb, magyar nevek |
   | B | „Melyik a japán neve: **`{magyar}`**?" | 4 szöveges gomb, romaji nevek |
   | C | „Melyik technika ez?" + illusztráció | 4 szöveges gomb, romaji nevek |
   | D | „Melyik ábra: **`{romaji}`**?" | 2×2 képrács, 4 illusztráció |
3. Opciók: nagy érintési célpont (≥ 48 dp), egy oszlop (A/B/C) vagy 2×2 rács (D).
4. Válasz után (azonnali visszajelzés, FR-17):
   - a kiválasztott opció megjelölve: helyes → pipa ikon + „Helyes", helytelen → × ikon +
     „Helytelen" (**ikon + szöveg, nem csak szín**);
   - a helyes opció mindig kiemelve;
   - alatta magyarázó kártya: a helyes technika **romaji + magyar név + ábra** hármasa,
     és link a részletező képernyőre;
   - „Következő" gomb (az utolsó kérdésnél: „Eredmény").
5. Bezárás (×) → megerősítő párbeszéd: „Megszakítod a kvízt? Az eddigi válaszaid megmaradnak."
   → „Megszakítás" / „Folytatás".

**Viselkedési szabályok**
- **Egyszeri válasz (FR-18):** a válaszkezelő a kvízállapot `answeredQuestionIds` halmazát
  ellenőrzi; a második esemény no-op. A gombletiltás csak másodlagos védelem.
- A válasz pillanatában keletkezik egy `QuizAttempt` rekord (`answeredAt` injektált `Clock`-ból).
- Az állapot (kérdéslista, seed, index, válaszok) `SavedStateHandle`-ben → forgatás túlélése (FR-19).
- Megszakításkor a `QuizSession.finishedAt` `null` marad, `answeredCount` a tényleges érték.
- Vissza-gomb = bezárás (ugyanaz a megerősítés). A kérdések között **nincs** visszalépés.

### 5.3 Eredmény (`quiz/result/{sessionId}`)

1. Nagy összegzés: „`{helyes}` / `{megválaszolt}` helyes" + százalék.
   - Ha `megválaszolt = 0`: nincs százalék, helyette „Nem válaszoltál egyetlen kérdésre sem."
   - Megszakított kvíz esetén: „Megszakított kvíz – `{megválaszolt}` / `{kért}` kérdés."
2. Hibás kérdések listája: kérdéstípus, a helyes technika (romaji + magyar + kis ábra), és
   amit a felhasználó választott. Tétel kattintható → részletező képernyő.
3. Gombok:
   - **„Hibák újragyakorlása"** – csak ha van hibás válasz. Új munkamenetet indít a hibás
     kérdések technikáiból, új seeddel (FR-21). A statisztika nem duplázódik, mert a régi
     munkamenet zárt, és az új önálló próbálkozásokat rögzít.
   - „Új kvíz" – vissza a konfigurációra, megtartott beállításokkal.
   - „Vissza a Haladáshoz".

---

## 6. Haladás

### 6.1 Áttekintés (`progress/overview`)

1. Cím: „Haladás".
2. Összesítő kártya (FR-23): összes megválaszolt kérdés, helyes válaszok, sikerarány.
   Nulla megválaszolt kérdés → „Még nincs adat. Indíts egy kvízt!" + „Kvíz indítása" gomb.
3. Kategóriánkénti bontás (FR-24): kategória neve, megválaszolt kérdésszám, sikerarány sávval.
   Kategória, amelyben még nincs próbálkozás: „nincs adat", nem 0%.
4. „Gyakorlást igénylő" szakasz (FR-26): legfeljebb 10 technika, rendezve
   (a) sikerarány növekvő, majd (b) utolsó gyakorlás régisége szerint.
   Minimum 1 próbálkozás kell, hogy egy technika ide bekerüljön.
5. Külön, **elhatárolt** szakasz: „Önértékelés a tanulókártyákon" (FR-28) –
   „Tudtam: `{n}` · Gyakorolnom kell: `{m}`" + magyarázat:
   „Az önértékelés nem számít bele a kvíz sikerarányába."
6. Alul: „Beállítások" link (Előrehaladás törlése, FR-35).

### 6.2 Technika-részletező (`progress/technique/{techniqueId}`)

- A technika azonosító adatai (romaji, magyar, ábra) + link a tudástári részletezőre.
- Kvízstatisztika: összes próbálkozás, helyes, sikerarány, utolsó gyakorlás időpontja (FR-25).
- Kérdéstípus szerinti bontás (melyik típusban hibázik).
- Önértékelési előzmény (utolsó néhány `StudyReview`), külön blokkban.
- Ha a technikát a legfrissebb tartalom már nem tartalmazza: „Ez a technika már nem része a
  tananyagnak, de a korábbi eredményeid megmaradtak." (DR-07)

### 6.3 Beállítások (`settings`)

- „Előrehaladás törlése" – kétlépcsős megerősítés, explicit szöveggel arról, hogy mi törlődik
  (kvízpróbálkozások, munkamenetek, önértékelések) és hogy a művelet nem visszavonható.
- Tartalominformáció: `schemaVersion`, `contentVersion`, `datasetKind`, rekordszám.
- Debug buildben: „Tartalomdiagnosztika" – a teljes validációs jelentés (FR-32).
- Verzióinformáció és licenc/forrás hivatkozás.

---

## 7. Folyamatok

### 7.1 Első indítás
```
app indul
 └─ tárolt contentVersion == csomagolt contentVersion ?
     ├─ nem (vagy üres) → assets JSON beolvasása
     │                     → validálás (ValidationReport)
     │                     → elfogadott rekordok upsert a tartalomtáblákba
     │                     → contentVersion mentése
     │                     → ha 0 elfogadott rekord: Tudástár "Empty – nincs tartalom" + diagnosztika
     └─ igen → betöltés kihagyva
 └─ Tudástár képernyő
```
A betöltés háttérszálon fut; a UI `Loading` állapotot mutat. A validációs jelentés a
diagnosztikához eltárolódik.

### 7.2 Tartalomfrissítés (app-frissítés új JSON-nal)
```
új csomagolt contentVersion
 └─ validálás
 └─ upsert (NEM törlés+beszúrás)
 └─ a csomagban már nem szereplő technika: visszavont jelölés, NEM fizikai törlés
 └─ felhasználói táblák: érintetlenek
 └─ a Haladás képernyő a visszavont technikákat megjelöli
```
Garancia: a felhasználói előrehaladás megmarad (FR-33, TR-05).

### 7.3 Kvíz életciklus
```
quiz/setup → konfiguráció
 └─ "próbafutás": a motor száraz generálása a készenléti összegzéshez
 └─ "Kvíz indítása":
     ├─ seed sorsolása (injektált RNG-ből), QuizSession létrehozása (startedAt)
     ├─ QuizPlan generálása (seed + konfiguráció)
     ├─ ha questions üres → nem indul el, vissza a setupra üzenettel
     └─ quiz/run
         └─ kérdésenként: válasz → QuizAttempt mentése → visszajelzés → Következő
         ├─ megszakítás → finishedAt = null, vissza a Kvíz fülre
         └─ utolsó kérdés → finishedAt beállítása → quiz/result
```

---

## 8. Szövegek (magyar UI – kivonat)

| Kulcs | Szöveg |
|---|---|
| nav.library | Tudástár |
| nav.learn | Tanulás |
| nav.quiz | Kvíz |
| nav.progress | Haladás |
| library.search.hint | Keresés romaji vagy magyar név szerint |
| library.empty.noResult | Nincs találat a(z) „%1$s" keresésre. |
| library.empty.noContent | A tudástár üres. A tartalom betöltése nem sikerült. |
| content.demoBanner | Demó adatkészlet. A tartalom szakmai ellenőrzésre vár, nem tanulásra hitelesített. |
| content.unverified | Ellenőrzésre vár |
| content.incomplete | Hiányos |
| learn.reveal | Koppints a felfedéshez |
| learn.knew | Tudtam |
| learn.needsPractice | Gyakorolnom kell |
| quiz.type.jpToHu | Japán név → magyar jelentés |
| quiz.type.huToJp | Magyar név → japán név |
| quiz.type.imageToJp | Ábra → japán név |
| quiz.type.jpToImage | Japán név → ábra |
| quiz.type.disabled.images | Ehhez legalább 4 egyedi, ellenőrzött ábra kell a kiválasztott kategóriákban (jelenleg: %1$d). |
| quiz.notEnoughData | A kiválasztott beállításokkal nem állítható össze kvíz. Válassz több kategóriát, engedélyezz több kérdéstípust, vagy csökkentsd a kérdésszámot. |
| quiz.shortfall | A kért %1$d helyett %2$d kérdés lesz, mert nincs több megfelelő technika. |
| quiz.answer.correct | Helyes |
| quiz.answer.wrong | Helytelen |
| quiz.abort.title | Megszakítod a kvízt? |
| quiz.abort.message | Az eddigi válaszaid megmaradnak. |
| quiz.result.noAnswers | Nem válaszoltál egyetlen kérdésre sem. |
| quiz.result.retryWrong | Hibák újragyakorlása |
| progress.noData | Még nincs adat. Indíts egy kvízt! |
| progress.selfRating.note | Az önértékelés nem számít bele a kvíz sikerarányába. |
| progress.retired | Ez a technika már nem része a tananyagnak, de a korábbi eredményeid megmaradtak. |

Minden felhasználói szöveg `strings.xml`-ben; a kódban hardcode-olt magyar szöveg nincs.

---

## 9. Akadálymentességi követelmények képernyőnként

| Képernyő | Konkrét követelmény |
|---|---|
| mind | TalkBack bejárási sorrend felülről lefelé; érintési célpont ≥ 48 dp; `sp` szövegméret |
| Tudástár lista | a listatétel egy bejárási egység: „`{romaji}`, `{magyar}`, `{kategória}`, `{státusz}`" |
| Tudástár részlet | az illusztráció leírása tartalmazza a technika nevét |
| Tanulókártya | felfedés előtt a `stateDescription` „elrejtve", utána „felfedve"; az önértékelő gombok letiltott állapota is bejelentve |
| Kvíz kérdés | opciók `selected` szemantikával; válasz után `stateDescription` = „helyes"/„helytelen"; a (D) típus képleírása **nem** tartalmazza a technika nevét |
| Kvíz eredmény | a sikerarány szövegesen is kiolvasható, nem csak a diagramon |
| Haladás | a sikerarány-sávok szöveges értékkel is |

---

## 10. Hibakezelési mátrix

| Esemény | Rendszerválasz | Felhasználói élmény |
|---|---|---|
| Nem támogatott `schemaVersion` | betöltés elutasítva, jelentés eltárolva | Tudástár: „Empty – nincs tartalom" + debug diagnosztika; **nincs összeomlás** |
| Parse hiba a JSON-ban | mint fentebb | mint fentebb |
| Néhány rekord érvénytelen | rekordok kizárva, a többi betöltve | a hiányzó technikák nem jelennek meg; diagnosztika listázza |
| Hiányzó képfájl | a rekord kép nélkül jelenik meg | helyőrző + „Ábra készül"; a (C)/(D) típus kizárja ezt a technikát |
| Nincs elég kvízképes technika | `QuizPlan.questions` üres vagy rövidebb | setup képernyő előre jelzi; a kvíz nem indul hibás kérdésekkel |
| Dupla koppintás a válaszon | második esemény no-op | semmi nem változik, az eredmény nem duplázódik |
| Képernyőforgatás kvíz közben | állapot visszaállítása `SavedStateHandle`-ből | ugyanaz a kérdés, ugyanaz az opciósorrend, megőrzött válaszok |
| Kvíz megszakítása | `finishedAt = null`, válaszok megőrizve | „Az eddigi válaszaid megmaradnak" |
| Room írási hiba | hiba naplózva, UI jelzi | „A válasz mentése nem sikerült." snackbar; a kvíz folytatható |
| Tartalomfrissítés közben indítás | a `Loading` állapot fennáll a betöltésig | körkörös jelző, nincs félig betöltött lista |

---

## 11. A funkcionális specifikáció lefedettsége

| Követelmény | Hol teljesül ebben a dokumentumban |
|---|---|
| FR-01…FR-05 | 3. fejezet |
| FR-06…FR-10 | 4. fejezet |
| FR-11…FR-22 | 5. fejezet |
| FR-23…FR-28 | 6. fejezet |
| FR-29…FR-35 | 6.3, 7.1, 7.2, 10. fejezet |
| CR-01…CR-08 | 2.1, 2.2, 2.3 |
| NFR-04, NFR-05 | 9. fejezet, 2.3 |
| NFR-06 | 2.4, 10. fejezet |
| NFR-07 | 5.1 (próbafutás), 5.2 (seed az állapotban) |
