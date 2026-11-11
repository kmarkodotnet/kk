# Claude Code mesterprompt – Kyokushin Karate Trainer

## Szerep és cél
Senior Android architect, Kotlin/Jetpack Compose fejlesztő, UX-tervező, tesztmérnök és oktatástechnológiai szakértő szerepében dolgozz. Készíts működő, offline-first Android MVP-t **Kyokushin Karate Trainer** néven. A termék magyar nyelvű, Kyokushin technikaneveket és mozdulatokat tanít, majd ugyanazon tudástár alapján kvízekkel gyakoroltat. Nem cél a Kumasoft alkalmazás kódjának, dizájnjának vagy tartalmának másolása.

**Munkamód:** Először vizsgáld meg a repositoryt, az SDK-t és az elérhető buildkörnyezetet. Ha üres, inicializálj projektet. Hozz létre rövid `docs/IMPLEMENTATION_PLAN.md` tervet, utána önállóan implementálj. Ne állj meg pusztán specifikációnál vagy vázkódnál. Kisebb, jól tesztelhető lépésekben dolgozz; minden lépés után ellenőrizd a buildet, ha a környezet engedi. Ha valami blokkol, dokumentáld pontosan és folytasd az elvégezhető feladatokkal. Ne állítsd, hogy futott teszt vagy build, ha nem futott.

## 1. MVP-hatókör
- Magyar nyelvű, akadálymentesen használható Android app.
- **Tudástár:** kategóriák (pl. állások/tachi, ütések/tsuki, védések/uke, rúgások/geri), keresés romaji és magyar név szerint, lista és részletes technikakártya.
- Minden rekord: stabil azonosító, japán név latin betűs átírása (romaji), magyar elnevezés, kategória, helyi illusztráció hivatkozása; opcionális japán íráskép, alternatív nevek, rövid magyarázat, nehézség/övfok, forrás és ellenőrzési státusz.
- **Tanulás:** lapozható tanulókártyák, elrejthető/felfedhető japán vagy magyar név, képpel, „Tudtam / Gyakorolnom kell” jelöléssel.
- **Kvíz:** (A) japán név → magyar jelentés; (B) magyar név → japán név; (C) rajz → japán név; (D) japán név → négy rajz közül a helyes. Az utóbbi két mód csak elegendő, egyedi és validált kép esetén aktív. Négy válaszlehetőség az alapértelmezés, de a motor csak akkor indít kérdést, ha legalább négy egyértelmű, megfelelő jelölt van; ne gyártson félrevezető válaszokat.
- Kvízbeállítások: kategóriák, kérdésszám (5/10/20), kérdéstípusok; helyes/hibás visszajelzés, végső összegzés és hibás kérdések újragyakorlása.
- **Előrehaladás:** helyes/hibás próbálkozások, utolsó gyakorlás ideje, technikánkénti egyszerű sikerarány; helyi tárolás, internet és bejelentkezés nélkül.
- Üres állapotok, kis adatkészlet, hiányzó kép és képernyőforgatás kezelése.

## 2. Technológia és szerkezet
- Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel, StateFlow/coroutines, Room, Hilt, kotlinx.serialization, Gradle Kotlin DSL, AndroidX Test/JUnit. Válassz egymással kompatibilis, stabil verziókat az elérhető Android SDK és a hivatalos dokumentáció alapján; ne találj ki verziószámokat.
- Egyszerű, moduláris csomagszerkezet: `core/model`, `core/data`, `core/quiz`, `feature/library`, `feature/learn`, `feature/quiz`, `feature/progress`, `navigation`, `di`.
- UI nem férhet hozzá közvetlenül Room DAO-hoz. Repository interfészek, DI, külön domain-szabályok. A kvízlogika tiszta Kotlin legyen, Android-függőség nélkül tesztelhető.
- Egyetlen APK, offline működés, minimális jogosultság; internetjogosultságot ne kérj, ha nincs szükség rá.
- Ne építs túl korán backend, account, szinkronizáció, fizetés, AI-chat vagy felhő funkciókat.

## 3. Tudástár és bővíthetőség
A kiadott tudástár `assets/knowledge/manifest.json` és `assets/knowledge/techniques.json` fájlokban, az illusztrációk lokális drawable/assets erőforrásként legyenek. A JSON séma verziózott és dokumentált. Importáláskor validálj: kötelező mezők, egyedi stabil ID, létező képhivatkozások, kategóriaazonosítók, duplikált/azonos jelentésű kifejezések, üres szöveg, unsupported schemaVersion. Hibás rekordot ne használj kvízben; jeleníts meg fejlesztői diagnosztikát, ne omoljon össze az app.

Példa, **csak sémaillusztráció, nem hitelesített karate-adat**:
```json
{
  "schemaVersion": 1,
  "contentVersion": "0.1.0",
  "techniques": [
    {
      "id": "technique-example-001",
      "categoryId": "uke",
      "romaji": "PÉLDA-ROMAJI",
      "hungarian": "PÉLDA MAGYAR NÉV",
      "japaneseScript": null,
      "aliases": [],
      "illustration": "technique-example-001.webp",
      "descriptionHu": null,
      "verified": false,
      "source": null,
      "quizEligible": false
    }
  ]
}
```

A tényleges adatmodell különítse el a **fogalmat/technikát** és az esetleges több névváltozatot. A helyes válaszok összehasonlítása technikaazonosítóval történjen, ne szabad szöveges egyezéssel. Egy rekord csak akkor kvízképes, ha a szükséges mezők hitelesítettek és a feladat egyértelmű. A felhasználói előrehaladás ne vesszen el tudástárfrissítéskor; azonosítók stabilak, törlések és átnevezések migrációja dokumentált. A felhasználói próbálkozások Room-ban legyenek, ne a csomagolt JSON-ban.

**Tartalmi integritás:** Ne állíts elő kitalált japán terminológiát vagy nem ellenőrzött karate-rajzot hiteles tananyagként. Ha nincs a repositoryban jogtisztán felhasználható, ellenőrzött tartalom, készíts elkülönített DEMO adatkészletet és egyértelmű „Demó / ellenőrzésre vár” jelzést. Ne használj harmadik fél alkalmazásából kinyert képeket. Dokumentáld a képek licencét és forrását. Az első éles adatkészletet szakmai ellenőrzésre előkészített CSV/JSON importtal lehessen feltölteni. A rajzok egyértelműen mutassák a testhelyzetet és a mozgásirányt; többfázisú mozdulatokhoz a séma később képsorozatot is támogathasson.

## 4. Kvízmotor
- Tiszta Kotlin `QuizGenerator` bemenet: validált technikák, engedélyezett kérdéstípusok, kategóriaszűrés, kérdésszám, seedelt RNG. Kimenet: kérdések helyes technikaazonosítóval és három különböző, téves technikaazonosítóval.
- Ne szerepeljen ugyanaz a helyes technika indokolatlanul többször egy rövid tesztben; opciók keverése reprodukálható legyen seed alapján.
- Distraktorok ugyanabból a kategóriából előnyben, de soha ne legyenek elfogadható szinonimák vagy vizuálisan azonos illusztrációk. Ha nincs elegendő jelölt, az adott kérdéstípus kihagyása és világos UI-üzenet szükséges.
- A kérdés és opciók jelenjenek meg megfelelő felirattal, az illusztrációknak legyen hozzáférhető leírásuk. A helyes válasz után a felhasználó lássa a romaji, magyar név és rajz hármasát.
- Kérdés megválaszolása egyszer történhet; dupla kattintás nem számolhat dupla eredményt. Kvízállapot túlélje a konfigurációváltást; megszakítás kezelése.
- Statisztika csak ténylegesen megválaszolt kérdésekből számolódjon; a tanulókártya „Tudtam” jelölés ne számítson objektív kvíztalálatnak.

## 5. UX és képernyők
Bottom navigation: `Tudástár`, `Tanulás`, `Kvíz`, `Haladás`.
- Tudástár: keresősáv, kategóriaszűrő chip-ek, listatételek romaji+magyar névvel és kis képpel; részletező képernyő nagyobb rajzzal, opcionális magyarázattal.
- Tanulás: kategóriaválasztás, kártya, felfedés, előző/következő, „Tudtam / Gyakorolnom kell”.
- Kvíz: konfiguráció, egy kérdés/képernyő, 4 válasz, előrehaladásjelző, eredmény és hibák ismétlése.
- Haladás: összes megválaszolt kérdés, sikerarány, kategóriánkénti bontás, gyakorlást igénylő elemek.
- Világos/sötét téma, nagy érintési célpontok, TalkBack és megfelelő kontraszt. Kerüld a harcművészeti klisékkel túlterhelt dizájnt; tiszta, gyors, funkcionális UI.

## 6. Adatmodell-javaslat
`Technique(id, categoryId, romaji, hungarian, japaneseScript?, aliases, descriptionHu?, illustrationRef?, verified, quizEligible, source?, contentVersion)`
`Category(id, nameHu, sortOrder)`
`QuizAttempt(id, techniqueId, questionType, selectedTechniqueId, correct, answeredAt, sessionId)`
`StudyReview(id, techniqueId, selfRating, reviewedAt)`
`QuizSession(id, startedAt, finishedAt?, requestedCount, answeredCount)`

A tartalom és a felhasználói adatok külön életciklust kövessenek. A statisztikákat lehetőleg a próbálkozásokból számold; ne tarts fenn ellentmondó redundáns számlálókat. Room migráció és biztonságos tartalomfrissítés legyen tervezve.

## 7. Tesztek és minőség
Kötelező automatizált unit tesztek:
1. JSON-validáció: hibás séma, hiányzó név, duplikált ID, hiányzó kép.
2. Kvízgenerálás: pontosan egy helyes válasz; három különböző hibás; nincs helyes szinonima a tévesek között; azonos seed → azonos sorrend; kevés adat → graceful fallback.
3. Kategóriaszűrés és kérdéstípusok jogosultsága.
4. Eredményszámítás: 0%, 100%, félbehagyott teszt, dupla válasz.
5. Tudástárfrissítés nem törli a korábbi előrehaladást.
6. UI tesztek a fő navigációra, tanulókártyára és egy teljes kvízre, ha az emulátor rendelkezésre áll.

Építési cél: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, szükség esetén `connectedDebugAndroidTest`. Kódformázás/lint és README telepítési utasítások. Ha build nem futtatható, pontos környezeti okot írj a végső jelentésbe.

## 8. Átadandók
- Teljes, Android Studio-ban megnyitható forrásprojekt.
- `README.md`: előfeltételek, futtatás, build, használat, ismert korlátok.
- `docs/ARCHITECTURE.md`: komponensek, adatfolyam, döntések.
- `docs/KNOWLEDGE_BASE.md`: JSON séma, új technika és kép hozzáadása, hitelesítési folyamat, licencelés, migráció.
- `docs/QUIZ_RULES.md`: kérdéstípusok, distraktorválasztás, szinonimák, fallback.
- `docs/TEST_REPORT.md`: ténylegesen futtatott parancsok és eredmények.
- Példaadatok és jogtisztán felhasználható demo illusztrációk, vagy explicit képhelyőrzők (ilyenkor kép-alapú kvíz nem aktiválható).

## 9. Elfogadási kritériumok
- Az alkalmazás internet nélkül elindul és a tudástár böngészhető.
- Minden megjelenített éles technika tartalmazza a japán romaji nevet, magyar nevet és mozdulatábrát; hiányos tételek egyértelműen demó/ellenőrzésre vár státuszúak.
- A tanulókártyák felfedik a választ és rögzítik az önértékelést.
- A kvíz azonos tudástárból generálódik, nem kézzel kódolt kérdéslistából.
- Egyetlen kérdésnek pontosan egy egyértelmű helyes válasza van; elégtelen adatok esetén nincs hibásan összeállított kvíz.
- Az eredmények app-újraindítás után megmaradnak.
- A tesztek és a debug build sikeresek az elérhető buildkörnyezetben, vagy a blokkoló tényezők dokumentáltak.
- Új, ellenőrzött technika hozzáadása nem igényel UI- vagy kvízmotor-kódmódosítást.

## 10. Későbbi bővítésre előkészítés, de MOST ne implementáld
- Több képkockás mozdulatábra, animáció, videó, hangos japán kiejtés.
- Kyu/dan tanterv és övvizsgatesztek, több dojo/szövetség szerinti tananyag.
- Spaced repetition (SM-2/FSRS), tanulási tervek, értesítések.
- Központi tudástár-szerkesztő, felhős tartalomszinkron, többnyelvűség.
- Felhasználói profilok, felhős backup, oktatói adminfelület.

## 11. Végrehajtási sorrend
1. Repository/környezet felmérése és implementációs terv.
2. Gradle projekt, Compose navigáció és DI.
3. Tudástár séma, import, validáció, helyi illusztrációk és böngésző UI.
4. Tanulókártyák és önértékelés.
5. Tiszta Kotlin kvízmotor, majd kvíz UI.
6. Room perzisztencia és haladás képernyő.
7. Tesztek, build, hibajavítás, dokumentáció.

**A munka végén** foglald össze: mi készült el, mely tesztek futottak ténylegesen, hol található a debug APK (ha elkészült), milyen demóadatok vannak, és melyek a következő három legfontosabb teendő az éles Kyokushin-tudástárhoz.
