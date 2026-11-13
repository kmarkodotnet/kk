# Követelményspecifikáció – Kyokushin Karate Trainer

| | |
|---|---|
| Dokumentum | Követelményspecifikáció (SRS) |
| Verzió | 1.0 |
| Dátum | 2026-11-11 |
| Állapot | Jóváhagyásra vár |
| Forrás | `CLAUDE_KYOKUSHIN_MASTER_PROMPT.md` |
| Kapcsolódó | `docs/FUNCTIONAL_SPEC.md`, `docs/ARCHITECTURE.md` |

---

## 1. Bevezetés

### 1.1 A termék célja
A **Kyokushin Karate Trainer** egy magyar nyelvű, offline működő Android alkalmazás, amely
Kyokushin karate technikaneveket (japán romaji átírás + magyar jelentés) és a hozzá tartozó
mozdulatábrákat tanítja, majd **ugyanazon tudástár alapján** kvízekkel gyakoroltat.

Az elsődleges tanulási feladat a **név–jelentés–mozdulat hármas** összekapcsolása: a
felhasználó lássa a technikát, tudja a japán nevét, és értse a magyar jelentését.

### 1.2 Célcsoport
Kezdő és közép szintű magyar anyanyelvű Kyokushin karate gyakorlók, akik övvizsgára vagy
edzésre készülve technikaneveket tanulnak. A felhasználó edzőteremben, utazás közben,
internet nélkül is használja az appot.

### 1.3 Hatókör (MVP)
**Benne van:** tudástár böngészés és keresés, tanulókártyák önértékeléssel, négy kvíztípus,
helyi előrehaladás-követés, magyar UI, akadálymentesség, világos/sötét téma.

**Nincs benne** (lásd 9. fejezet): backend, felhasználói fiók, szinkronizáció, fizetés,
AI-chat, spaced repetition, animáció, videó, hang, többnyelvűség, oktatói adminfelület,
övvizsgatanterv.

### 1.4 Nem cél
A Kumasoft (vagy bármely más) alkalmazás kódjának, dizájnjának vagy tartalmának másolása.
Harmadik fél appjából kinyert kép vagy szöveg felhasználása **tilos**.

### 1.5 Fogalmak

| Fogalom | Jelentés |
|---|---|
| Technika | A tudástár egy rekordja: egy karate fogalom, stabil azonosítóval |
| Romaji | A japán név latin betűs átírása |
| Kategória | Technikacsoport (pl. állások, ütések, védések, rúgások) |
| Alias | A technika alternatív, elfogadható névváltozata |
| Hitelesített (`verified`) | Emberi szakmai ellenőrzésen átment rekord |
| Kvízképes (`quizEligible`) | Kvízben használható rekord: hitelesített + elegendő mező |
| Distraktor | Helytelen válaszlehetőség egy kvízkérdésben |
| Demó adatkészlet | Nem hitelesített, kizárólag a működés bemutatására szolgáló tartalom |
| Munkamenet (session) | Egy elindított kvíz futása |

### 1.6 A követelmények jelölése
`FR-nn` funkcionális, `NFR-nn` nem funkcionális, `CR-nn` tartalmi integritás,
`DR-nn` adat, `TR-nn` tesztkövetelmény, `AC-nn` elfogadási kritérium.
Prioritás: **M** (Must, MVP), **S** (Should, MVP-ben ha belefér), **W** (Won't, most nem).

---

## 2. Érintettek és felhasználói történetek

| # | Történet | Követelmény |
|---|---|---|
| US-01 | Gyakorlóként böngészni akarom a technikákat kategória szerint, hogy átlássam a tananyagot. | FR-01, FR-02 |
| US-02 | Keresni akarok romaji vagy magyar név szerint, hogy gyorsan megtaláljak egy technikát. | FR-03 |
| US-03 | Egy technika részleteit akarom látni nagyobb ábrával, hogy megértsem a mozdulatot. | FR-04 |
| US-04 | Tanulókártyákkal akarok gyakorolni, elrejtett válasszal, hogy önállóan felidézzem. | FR-06, FR-07 |
| US-05 | Jelölni akarom, mit tudtam és mit kell gyakorolnom, hogy lássam hol állok. | FR-10 |
| US-06 | Kvízzel akarom ellenőrizni magamat, hogy objektív visszajelzést kapjak. | FR-11…FR-19 |
| US-07 | A kvíz végén látni akarom az eredményt és újra akarom gyakorolni a hibákat. | FR-20, FR-21 |
| US-08 | Látni akarom a haladásomat kategóriánként, hogy tudjam mire koncentráljak. | FR-23…FR-26 |
| US-09 | Internet nélkül, edzőteremben is használni akarom az appot. | NFR-01 |
| US-10 | Meg akarom tudni, ha egy adat még nincs szakmailag ellenőrizve, hogy ne tanuljak rosszat. | CR-01…CR-05 |
| US-11 | Tartalomfejlesztőként új hitelesített technikát akarok felvenni programozás nélkül. | FR-30, AC-08 |

---

## 3. Funkcionális követelmények

### 3.1 Tudástár (Library)

| # | Követelmény | Pri |
|---|---|---|
| FR-01 | Az app kategóriákba szervezve listázza a technikákat. A kategórialista a tudástár manifestjéből jön, nem a kódból. | M |
| FR-02 | A listán kategóriaszűrő chip-ek választhatók; több kategória egyszerre is; a szűrés állapota túléli a képernyőforgatást. | M |
| FR-03 | Keresés szabad szöveggel, amely **egyszerre** illeszkedik a romaji névre, a magyar névre és az aliasokra; kis-/nagybetűre és diakritikus jelre nem érzékeny. | M |
| FR-04 | Listatétel megnyitásakor részletező képernyő: nagyobb illusztráció, romaji, magyar név, opcionális japán íráskép, aliasok, magyarázat, kategória, hitelesítési státusz. | M |
| FR-05 | Minden lista- és részletnézet jelzi, ha a rekord nem hitelesített („Demó / ellenőrzésre vár") vagy hiányos. | M |

### 3.2 Tanulás (Learn)

| # | Követelmény | Pri |
|---|---|---|
| FR-06 | A felhasználó kategóriá(ka)t választ, majd lapozható tanulókártyákat kap. | M |
| FR-07 | A kártya egy oldala elrejtett: a felhasználó választhat, hogy a japán vagy a magyar nevet rejti el. A felfedés explicit interakció. | M |
| FR-08 | A kártya tartalmazza az illusztrációt (ha van), és felfedés után a romaji + magyar név + ábra hármast. | M |
| FR-09 | Előző/következő navigáció. A kártyapozíció túléli a képernyőforgatást. | M |
| FR-10 | A felhasználó „Tudtam" / „Gyakorolnom kell" önértékelést adhat. Ez `StudyReview` rekordként tárolódik, és **nem** számít kvíztalálatnak. | M |

### 3.3 Kvíz (Quiz)

| # | Követelmény | Pri |
|---|---|---|
| FR-11 | Kvízkonfiguráció: kategóriák, kérdésszám (5 / 10 / 20), engedélyezett kérdéstípusok. | M |
| FR-12 | Négy kérdéstípus: **(A)** romaji → magyar jelentés, **(B)** magyar név → romaji, **(C)** ábra → romaji, **(D)** romaji → négy ábra közül a helyes. | M |
| FR-13 | A (C) és (D) típus csak akkor aktív, ha elegendő, egyedi, validált illusztráció van. Egyébként a UI letiltja és **megindokolja**. | M |
| FR-14 | Minden kérdés **pontosan négy** válaszlehetőséget kínál, pontosan egy helyes válasszal. A motor nem indít kérdést, ha nincs legalább négy egyértelmű, megfelelő jelölt. | M |
| FR-15 | A kvíz kérdései a tudástárból generálódnak, nem kézzel kódolt kérdéslistából. | M |
| FR-16 | Egy kérdés egy képernyőn jelenik meg, előrehaladásjelzővel (pl. „3 / 10"). | M |
| FR-17 | Válasz után azonnali visszajelzés: helyes/helytelen, és a helyes technika romaji + magyar név + ábra hármasa. A helyesség nem csak színnel jelenik meg. | M |
| FR-18 | Egy kérdés **csak egyszer** válaszolható meg; ismételt (pl. dupla) koppintás nem rögzít újabb próbálkozást és nem módosítja az eredményt. | M |
| FR-19 | A kvíz állapota túléli a konfigurációváltást; a megszakított kvíz kezelt: az addig megválaszolt kérdések beszámítanak, a munkamenet befejezetlen marad. | M |
| FR-20 | A kvíz végén összegzés: megválaszolt kérdésszám, helyes válaszok, sikerarány, a hibás kérdések listája. | M |
| FR-21 | A hibás kérdések újragyakorolhatók; ez **új munkamenetet** indít, nem duplázza a statisztikát. | M |
| FR-22 | Ha a választott beállításokhoz nincs elég adat, a kvíz nem indul el hibás kérdésekkel; helyette konkrét magyar üzenet és javaslat jelenik meg (más kategória / más kérdéstípus / kisebb kérdésszám). | M |

### 3.4 Haladás (Progress)

| # | Követelmény | Pri |
|---|---|---|
| FR-23 | Összesített nézet: összes megválaszolt kérdés, helyes válaszok, összesített sikerarány. | M |
| FR-24 | Kategóriánkénti bontás sikeraránnyal. | M |
| FR-25 | Technikánkénti egyszerű sikerarány és utolsó gyakorlás időpontja. | M |
| FR-26 | „Gyakorlást igénylő" lista: a legalacsonyabb sikerarányú vagy legrégebben gyakorolt technikák. | M |
| FR-27 | A statisztika a tárolt próbálkozásokból **számolódik**, nem redundáns számlálókból. Nulla megválaszolt kérdés esetén „nincs adat" jelenik meg, nem 0%. | M |
| FR-28 | A kvízstatisztika és a tanulókártya-önértékelés külön jelenik meg, és soha nem keveredik. | M |

### 3.5 Tartalom és bővíthetőség

| # | Követelmény | Pri |
|---|---|---|
| FR-29 | A tudástár csomagolt JSON-ból (`assets/knowledge/manifest.json`, `techniques.json`) töltődik be, verziózott és dokumentált sémával. | M |
| FR-30 | Új, hitelesített technika és illusztráció hozzáadása **nem igényel UI- vagy kvízmotor-kódmódosítást**: elég a JSON + kép + `contentVersion` léptetése. | M |
| FR-31 | Az importáló validál (lásd DR-05), a hibás rekordot kizárja a kvízből, és az app nem omlik össze. | M |
| FR-32 | Debug buildben fejlesztői diagnosztika elérhető a teljes validációs jelentéssel. | S |
| FR-33 | A tudástár frissítése **nem törli** a felhasználói előrehaladást. | M |
| FR-34 | Az első éles adatkészlet szakmai ellenőrzésre előkészített CSV/JSON importtal feltölthető; a formátum és a folyamat dokumentált. | S |
| FR-35 | „Előrehaladás törlése" funkció a beállításokban, megerősítő párbeszéddel. | S |

### 3.6 Vizuális arculat, navigáció és tartalom-munkafolyamat (2026-11-13 kiegészítés)

> A felhasználó 2026-11-13-i design-visszajelzése alapján felvett követelmények. Nem
> részei az eredeti MVP elfogadási körének (M1…M7 már lezárva); a `docs/backlog/`
> új, önálló taskjaiban (T-701…T-708) kerülnek kidolgozásra.

| # | Követelmény | Pri |
|---|---|---|
| FR-36 | Induláskor indítóképernyő (splash) jelenik meg: kék kandzsi logó fehér háttéren, mielőtt a Kezdőoldal betöltődik. | M |
| FR-37 | Önálló Kezdőoldal (Főoldal) létezik a logóval; az alsó navigáció bal szélén Főoldal gomb található. | M |
| FR-38 | Az alsó navigáció sorrendje: Főoldal, Tanulás, Kvíz, Haladás, Tudástár – a Tudástár a legjobboldali elem. | S |
| FR-39 | A Tanulás kategóriaválasztása ikonos kategóriakártyákkal történik checkbox helyett; egy kategóriára koppintva a hozzá tartozó technikák egyenként, böngészhető listában jelennek meg. | M |
| FR-40 | A tanulókártya képernyő tetején lapfül váltja, hogy a kiinduló (felfedés előtti) oldal a japán vagy a magyar név legyen (a meglévő elrejtési beállítás FR-07 UI-ja lapfülekre cserélődik). | M |
| FR-41 | Dokumentált munkafolyamat áll rendelkezésre további, a felhasználó által utólag biztosított illusztrációk tudástárba való befogadására (fájlelhelyezés, manifest-bejegyzés, `contentVersion` léptetés, validáció). | S |
| FR-42 | A tudástár romaji/magyar fordításai és terminológiája dokumentáltan összevetésre kerül hiteles Kyokushin-forrásokkal; az eltérések, a forrás és a hitelesítési döntés a dokumentációban rögzül. | S |

---

## 4. Adatkövetelmények

| # | Követelmény |
|---|---|
| DR-01 | **Technikarekord mezői:** stabil `id`, `categoryId`, `romaji`, `hungarian` (kötelező); `japaneseScript`, `aliases[]`, `illustration`, `descriptionHu`, `difficulty`, `beltRank`, `source`, `verified`, `quizEligible` (opcionális/származtatott). |
| DR-02 | **Kategória:** `id`, `nameHu`, `sortOrder`. A magyar kategórianevek a tartalomban élnek, nem a kódban. |
| DR-03 | **Fogalom és névváltozat szétválasztása:** egy technika = egy fogalom, több elfogadható névváltozattal (`aliases`). A helyes válasz összehasonlítása **technikaazonosítóval** történik, soha nem szabad szöveges egyezéssel. |
| DR-04 | **Felhasználói adat:** `QuizAttempt(id, techniqueId, questionType, selectedTechniqueId, correct, answeredAt, sessionId)`, `StudyReview(id, techniqueId, selfRating, reviewedAt)`, `QuizSession(id, startedAt, finishedAt?, requestedCount, answeredCount)`. Tárolás Room-ban, **nem** a csomagolt JSON-ban. |
| DR-05 | **Validáció:** kötelező mezők jelenléte; egyedi stabil ID; létező képhivatkozás; létező kategóriaazonosító; duplikált vagy azonos jelentésű kifejezések felderítése; üres szöveg; nem támogatott `schemaVersion`. Részletes szabálytáblázat: `.claude/skills/kyo-knowledge-base/SKILL.md`, 4. pont. |
| DR-06 | **Azonosítóstabilitás:** a technika `id` örök, nem újrahasznosítható. Átnevezés nem változtat `id`-t. |
| DR-07 | **Életciklus-szétválasztás:** a tartalom és a felhasználói adat külön életciklust követ. A felhasználói adat nem törlődik a tartalom frissítésekor; nincs `CASCADE DELETE` a tartalomról a felhasználói adatra. |
| DR-08 | **Migráció:** minden Room sémaváltás explicit, dokumentált `Migration`-t kap. Destruktív migráció a felhasználói adatra tilos. A tartalomtörlések és átnevezések migrációja dokumentált. |
| DR-09 | **Nincs ellentmondó redundancia:** a statisztika levezetett, nem duplikált számláló. |

---

## 5. Tartalmi integritási követelmények

| # | Követelmény |
|---|---|
| CR-01 | Kitalált japán terminológia, átírás, magyar jelentés, övfok-hozzárendelés vagy magyarázat **nem jelenhet meg hiteles tananyagként**. Generált tartalom `verified = false`. |
| CR-02 | A `verified = true` átállítása **emberi döntés**. Kód, import vagy migráció automatikusan nem emelhet hitelesítési státuszt. |
| CR-03 | Ha nincs jogtisztán felhasználható, ellenőrzött tartalom, **elkülönített demó adatkészlet** készül (`datasetKind: "demo"`), minden rekord `verified = false`, `quizEligible = false`, és a UI globálisan jelzi a demó állapotot. |
| CR-04 | Harmadik fél alkalmazásából, weboldalról vagy könyvből kinyert, licenc nélküli kép felhasználása tilos. Minden illusztrációnál dokumentált: forrás, szerző, licenc. |
| CR-05 | Ha nincs valódi illusztráció, **explicit képhelyőrzőt** kell használni; ilyenkor a kép-alapú kvíztípusok (C, D) nem aktiválhatók. |
| CR-06 | A rajz egyértelműen mutatja a testhelyzetet és a mozgásirányt. Többfázisú mozdulathoz a séma később képsorozatot támogathat; az MVP egy képet használ technikánként. |
| CR-07 | Szinonim vagy azonos fogalmat jelölő rekordok **nem lehetnek egymás distraktorai**. |
| CR-08 | A dizájn kerüli a harcművészeti klisékkel túlterhelt megjelenést; tiszta, gyors, funkcionális UI. |

---

## 6. Nem funkcionális követelmények

| # | Kategória | Követelmény |
|---|---|---|
| NFR-01 | **Offline** | Az app internet nélkül indul és teljesen működik. A manifest **nem** kér `android.permission.INTERNET`-et. Egyetlen APK, minimális jogosultság. |
| NFR-02 | **Platform** | Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel, StateFlow/coroutines, Room, Hilt, kotlinx.serialization, Gradle Kotlin DSL, AndroidX Test/JUnit. Egymással kompatibilis, **stabil, ellenőrzött** verziók; verziószám kitalálása tilos. |
| NFR-03 | **Architektúra** | A UI nem ér hozzá közvetlenül Room DAO-hoz. Repository interfészek + DI. A kvízlogika tiszta Kotlin, Android-függőség nélkül tesztelhető. Modulszerkezet: `core/model`, `core/data`, `core/quiz`, `feature/library`, `feature/learn`, `feature/quiz`, `feature/progress`, `navigation`, `di`. |
| NFR-04 | **Akadálymentesség** | TalkBack-kompatibilis; minden illusztrációnak értelmes magyar `contentDescription`-ja van, kivéve ahol az felfedné a helyes választ; érintési célpont ≥ 48dp; WCAG AA kontraszt világos és sötét témában; skálázódó szövegméret. |
| NFR-05 | **Téma** | Világos és sötét téma támogatott, a rendszerbeállítást követve. |
| NFR-06 | **Robusztusság** | Kezelt üres állapotok, kis adatkészlet, hiányzó kép, képernyőforgatás, megszakított kvíz. Érvénytelen tartalom nem okoz összeomlást. |
| NFR-07 | **Determinizmus** | A kvízgenerálás seedelt RNG-vel reprodukálható: azonos bemenet + seed → azonos kérdés- és opciósorrend. Üzleti kódban nincs közvetlen `Random.Default` vagy rendszeróra-hívás; `Clock` és RNG injektált. |
| NFR-08 | **Perzisztencia** | Az eredmények app-újraindítás után megmaradnak, helyi tárolásban, bejelentkezés nélkül. |
| NFR-09 | **Adatvédelem** | Nincs adatgyűjtés, nincs analitika, nincs hálózati forgalom. Minden adat az eszközön marad. |
| NFR-10 | **Teljesítmény** | A tudástár betöltése (néhány száz rekord) nem blokkolja a UI szálat; a lista és a keresés érezhető késés nélkül reagál. |
| NFR-11 | **Minőség** | Lint és kódformázás ellenőrzött. `README.md` telepítési és futtatási utasításokkal. |
| NFR-12 | **Jelentéshűség** | Build vagy teszt eredmény csak tényleges, dokumentált futtatás alapján állítható. A `docs/TEST_REPORT.md` tartalmazza a pontos parancsokat és kimeneteket. |

---

## 7. Tesztkövetelmények

Kötelező automatizált unit tesztek. Minden teszt neve utaljon a TR azonosítóra.

| # | Tesztkövetelmény | Fedett követelmény |
|---|---|---|
| TR-01 | **JSON-validáció:** nem támogatott `schemaVersion`, hiányzó kötelező név, duplikált ID, nem létező képhivatkozás, nem létező kategóriaazonosító, üres szöveg – mindegyikre a megfelelő hibakód és a rekord helyes kizárása; nincs kezeletlen kivétel. | DR-05, FR-31 |
| TR-02 | **Kvízgenerálás:** pontosan egy helyes válasz; három **különböző** hibás; nincs szinonima a tévesek között; azonos seed → azonos sorrend; kevés adat → graceful fallback (kevesebb kérdés vagy kihagyott típus, nem hibás kérdés). | FR-14, FR-22, NFR-07, CR-07 |
| TR-03 | **Kategóriaszűrés és kérdéstípus-jogosultság:** a szűrt kategóriákon kívüli technika nem jelenik meg; a (C)/(D) típus elegendő validált kép nélkül kihagyásra kerül, dokumentált okkal. | FR-11, FR-13 |
| TR-04 | **Eredményszámítás:** 0%, 100%, félbehagyott teszt (csak a megválaszoltak számítanak), dupla válasz (nem duplázza az eredményt), nulla megválaszolt kérdés → „nincs adat", nem 0%. | FR-18, FR-20, FR-27 |
| TR-05 | **Tudástárfrissítés:** új `contentVersion` betöltése után a korábbi `QuizAttempt`/`StudyReview` rekordok megmaradnak és a statisztika változatlan; átnevezett technika előrehaladása követi az `id`-t; visszavont technika próbálkozásai nem veszítenek hivatkozást. | FR-33, DR-06, DR-07, DR-08 |
| TR-06 | **UI tesztek** (emulátor esetén): a négy fő navigációs cél elérése, tanulókártya felfedése és önértékelése, egy teljes kvíz végigvitele az összegzésig. | FR-06…FR-10, FR-16…FR-20 |
| TR-07 | **Rétegszabály:** nincs `android.*` import a `core/model` és `core/quiz` modulokban; a repository interfész nem ad vissza Room entitást. | NFR-03 |
| TR-08 | **Offline:** a manifest nem tartalmaz `INTERNET` permissiont. | NFR-01 |

Build célok: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, szükség esetén
`./gradlew connectedDebugAndroidTest`, valamint lint és formázásellenőrzés.

**Ha a build nem futtatható, a pontos környezeti okot a `docs/ENVIRONMENT.md` és a végső
jelentés tartalmazza.** Jelenlegi állapot: nincs JDK és nincs Android SDK a gépen
(B-01, B-02) – lásd `docs/ENVIRONMENT.md`.

---

## 8. Elfogadási kritériumok

| # | Kritérium | Hogyan ellenőrizzük |
|---|---|---|
| AC-01 | Az app internet nélkül elindul és a tudástár böngészhető. | repülő módban indítás + manifest ellenőrzés (TR-08) |
| AC-02 | Minden megjelenített **éles** technika tartalmaz romaji nevet, magyar nevet és mozdulatábrát; a hiányos tételek egyértelműen demó/ellenőrzésre vár státuszúak. | `/kyo-validate-kb` + UI ellenőrzés |
| AC-03 | A tanulókártyák felfedik a választ és rögzítik az önértékelést. | TR-06 + adatbázis-ellenőrzés |
| AC-04 | A kvíz ugyanabból a tudástárból generálódik, nem kézzel kódolt kérdéslistából. | kódaudit (`kyo-spec-auditor`) + TR-02 |
| AC-05 | Egyetlen kérdésnek pontosan egy egyértelmű helyes válasza van; elégtelen adat esetén nincs hibásan összeállított kvíz. | TR-02, TR-03 |
| AC-06 | Az eredmények app-újraindítás után megmaradnak. | TR-04 + manuális újraindítás |
| AC-07 | A tesztek és a debug build sikeresek az elérhető buildkörnyezetben, **vagy** a blokkoló tényezők dokumentáltak. | `docs/TEST_REPORT.md`, `docs/ENVIRONMENT.md` |
| AC-08 | Új, ellenőrzött technika hozzáadása nem igényel UI- vagy kvízmotor-kódmódosítást. | gyakorlati próba: JSON + kép hozzáadása, kódmódosítás nélküli megjelenés |

---

## 9. Később, MOST nem implementálandó (W)

Többképkockás mozdulatábra, animáció, videó, hangos japán kiejtés; kyu/dan tanterv és
övvizsgatesztek; több dojo/szövetség szerinti tananyag; spaced repetition (SM-2/FSRS),
tanulási tervek, értesítések; központi tudástár-szerkesztő, felhős tartalomszinkron,
többnyelvűség; felhasználói profilok, felhős backup, oktatói adminfelület.

Az architektúra ezekre **előkészít** (séma bővíthetőség, modulhatárok), de kódot nem ír.

---

## 10. Nyitott kérdések

| # | Kérdés | Hatás | Javasolt kezelés |
|---|---|---|---|
| OQ-01 | Van-e jogtisztán felhasználható, szakmailag ellenőrzött technikalista és illusztrációkészlet? | Ettől függ, hogy éles vagy demó adatkészlettel indulunk, és hogy a (C)/(D) kvíztípus aktiválható-e. | **ELDÖNTVE (2026-11-11): nincs még.** Az MVP demó adatkészlettel indul – 4 kategória × ~5 rekord, képhelyőrzőkkel (CR-03, CR-05). A (C)/(D) kvíztípus nem aktiválható; az éles tartalom importtal jön (FR-34). |
| OQ-02 | Ki a szakmai hitelesítő (szenszei / dojo / szövetség)? | A `verified = true` átállítás folyamata. | A folyamat dokumentálása `docs/KNOWLEDGE_BASE.md`-ben; a hitelesítőt a felhasználó nevezi meg. |
| OQ-03 | Melyik `minSdk`? | Eszköztámogatás vs. Compose/Room követelmények. | **ELDÖNTVE (2026-11-11): `minSdk = 26` (Android 8.0)** – natív `java.time`, nincs core library desugaring. Lásd [ADR-009](adr/ADR-009-min-sdk.md). |
| OQ-04 | Legyen-e felhasználói adat exportálása az MVP-ben? | Adatvédelem, felhasználói kontroll. | „Előrehaladás törlése" igen (FR-35, S); export a Fázis 8 után, igény szerint. |
| OQ-05 | Vezessünk-e be gépi vagy hangfájl alapú japán kiejtés-lejátszást a tanuláshoz? | A mesterprompt **9. pontja** („hangos japán kiejtés") explicit kizárja a MOST fázisból. | **NYITOTT.** A 9. pont felülvizsgálata (felhasználói jóváhagyással) nélkül nem indítható. A `docs/backlog/T-709` rögzíti az igényt, de függősége (ez az OQ) szándékosan nincs feloldva. |
| OQ-06 | Legyen-e kvízkérdés-típus, amely lejátszott kiejtés alapján kér magyar nevet? | Az OQ-05 megoldása nélkül nem építhető (a kiejtéslejátszás az előfeltétele). | **NYITOTT**, OQ-05-től függ. A `docs/backlog/T-710` rögzíti az igényt. |
| OQ-07 | Vezessünk-e be többnyelvűséget (angol, német) a magyar mellett? | A mesterprompt **9. pontja** („többnyelvűség") ezt is explicit kizárja a MOST fázisból. | **NYITOTT.** A `docs/backlog/T-711` rögzíti az igényt, de a 9. pont felülvizsgálata nélkül nem indítható. |
