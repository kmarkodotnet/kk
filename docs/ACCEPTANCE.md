# Átadási ellenőrzés – 2026-11-13

Hatókör: a T-604, T-605, T-606, T-610 és T-611 feladat, debug Android build.
A szakmai lektorálás külön munka; a csomagolt rekordok hitelesítési státuszát
a fejlesztés nem emelte meg.

| Kritérium | Eredmény | Bizonyíték és határ |
|---|---|---|
| AC-01 – offline indítás és böngészés | **Teljesül** | `tools/verify_offline_restart.py`: az emulátor repülő módban, az app force-stop után indul; a tudástár keresője és a Haladás elérhető. A manifest permission-teszt is sikeres; az app nem kér INTERNET jogosultságot. |
| AC-02 – éles technikák nevei, ábrái; hiányok jelölése | **Teljesül a jelenlegi, lektorálatlan csomagra** | A valós assets-importteszt és validátor ellenőrzi a csomagot; a UI státuszjelzést és hiányzó ábránál helyőrzőt mutat. 161 rekord, 9 kategória, 10 ábra; szakmailag jóváhagyott éles rekordot nem állítunk. |
| AC-03 – kártyafelfedés és önértékelés mentése | **Teljesül** | `AppFlowTest.studyRevealRatingAndRotation_TR06_FR10_FR19`, nagy betűmérettel is; RoomProgressRepository önértékelési tesztjei. Felfedés előtt tiltott, utána aktív gomb; forgatás után is megmarad az állapot. |
| AC-04 – a kvíz ugyanabból a tudástárból készül | **Teljesül** | A QuizSetupViewModel a TechniqueRepository jelöltjeit adja a generátornak. A teljes eszközteszt az assets JSON neveiből azonosítja a kérdéseket és szándékosan hibás opciót választ az újragyakorlás ellenőrzéséhez. |
| AC-05 – egyértelmű helyes válasz, elégtelen adatok kezelése | **Teljesül a tesztelt szabályokra** | core/quiz: 245 sikeres teszt, generálási/distraktor/normalizálási/shortfall szabályokkal. A feliratos ábrák képes kvízből kizártak. A nyelvi és karate-szakmai tartalom helyessége továbbra is lektori feladat. |
| AC-06 – eredmények megmaradnak újraindítás után | **Teljesül** | Külön emulátoros próba: öt mentett válasz, majd kétszeri `am force-stop` és indítás. A Haladás előtte/utána szövege egyezik, köztük `0 / 5 helyes`; XML és JSON bizonyíték: `tmp/acceptance/`. Ez valódi alkalmazásfolyamat-újraindítás, nem csak Activity-recreate. |
| AC-07 – tesztek és debug build | **Teljesül** | A teljes kombinált Gradle-futtatás exit 0: 647 unit teszt, 6 eszközteszt, assembleDebug, lint, ktlintCheck. A végső, kártyaforgatással kiegészített eszközfutás szintén exit 0. Parancsok: TEST_REPORT.md. |
| AC-08 – új ellenőrzött technika felvétele kódmódosítás nélkül | **Részben igazolt** | Az adatvezérelt importot, verzióváltást, új/visszatérő rekordokat a ContentImporterTest és ContentUpdateProgressTest ellenőrzi; a UI és a kvíz repositoryból dolgozik. Új, ténylegesen lektorált rekord + kép kézi beépítése és UI-próbája **NEM FUTOTT**, mert nincs ilyen új jóváhagyott forráscsomag. A fejlesztés nem állított elő hitelesnek mondott új karateanyagot. |

Az AC-08 fennmaradó tartalmi elfogadási próbája nem elvégzettként szerepel.
A T-610 célja a kritériumok és bizonyítékaik egyenkénti, őszinte rögzítése;
a szakmai tartalom jóváhagyása nem a fejlesztő feladata.

## Átadott APK

`app/build/outputs/apk/debug/app-debug.apk` – **22 858 574 byte** (21,80 MiB).

SHA-256: `f846fb85a98a776330483906b85db3e5f37bbcd0970c9d288160d45f870cfff7`.

## További tartalmi munka

1. Oktatói lektorálás és a javítások forrással dokumentált átvétele.
2. Hiányzó, felirat nélküli, jogtisztán felhasználható mozdulatábrák pótlása.
3. Az új, jóváhagyott csomag validálása és oktatóval/tanulókkal végzett elfogadási próba,
   benne az AC-08 tényleges JSON + kép hozzáadási próbájával.

A korábban dokumentált tudástár-adatlapi gyakorlási összegzés hiánya továbbra is
külön fejlesztési ügy; a statisztika a Haladás képernyőről elérhető. A többi ismert
korlát a [README](../README.md) és a [CONVENTIONS](CONVENTIONS.md) dokumentumban található.
