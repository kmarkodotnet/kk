# Kyokushin Karate Trainer

Magyar nyelvű, internet nélkül működő Android gyakorlóalkalmazás. A tudástárban
romaji vagy magyar névre kereshetsz, kategóriára szűrhetsz, tanulókártyákat nézhetsz,
kvízt indíthatsz és követheted a saját haladásodat.

## Kipróbálás Android Studióból

1. Nyisd meg a projekt gyökérmappáját, amelyben a `settings.gradle.kts` található.
2. Várd meg a Gradle szinkronizálását. Az SDK Managerben legyen telepítve az
   Android API 37 platform, a Build Tools 37.0.0 és a Platform Tools.
3. A Device Managerben indíts el egy emulátort, vagy csatlakoztass Android 8.0+
   telefont USB-hibakereséssel. A projekt tesztjei a `Medium_Phone_API_37.0` AVD-n futnak.
4. A felső futtatási listában válaszd az **app** konfigurációt és az eszközt,
   majd kattints a zöld **Run ▶** gombra.

Ha nincs `app` futtatási konfiguráció: **Run → Edit Configurations → + → Android App**,
Module: `KyokushinKarateTrainer.app` (vagy a listában szereplő `app` modul), Launch:
Default Activity. A projekt gyökerét kell megnyitni, nem önmagában az `app` mappát.

## APK és parancssoros build

A Gradle wrapper a projekt része. JDK és Android SDK szükséges; a Kotlin/Java
fordítás célja 17. Az aktuális helyi környezetet a [környezeti leírás](docs/ENVIRONMENT.md)
rögzíti. A függőségek első letöltéséhez internet kell, az alkalmazás használatához nem.

PowerShell a projekt gyökerében:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lint ktlintCheck
```

Debug APK: [`app/build/outputs/apk/debug/app-debug.apk`](app/build/outputs/apk/debug/app-debug.apk).
Az átadási build mérete és az ellenőrzések eredménye a [tesztjelentésben](docs/TEST_REPORT.md)
található. Telepítés az SDK `platform-tools` mappájában elérhető `adb` paranccsal:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ez fejlesztői aláírású debug build, nem áruházi kiadás. Telefonon az APK fájl
megnyitásával is telepíthető, ha az adott fájlkezelő számára engedélyezed a telepítést.

## Használat

- **Tudástár:** keresés, kategóriaszűrés, technika adatlapja.
- **Tanulás:** válassz kategóriát és elrejtési módot, indítsd a kártyákat. Koppints
  a felfedéshez; a „Tudtam” és „Gyakorolnom kell” menti az önértékelést.
- **Kvíz:** válassz kategóriát, kérdésszámot és típust. Válasz után visszajelzés,
  a végén eredmény és a hibák újragyakorlása érhető el. Megszakításkor az addigi
  válaszok megmaradnak.
- **Haladás:** összesítés, kategóriák, gyakorlást igénylő technikák, önértékelés.
  A Beállításokban külön megerősítéssel törölheted a saját gyakorlási adatokat.
  A tartalomdiagnosztika csak debug buildben jelenik meg.

## Ismert korlátok

- A csomagolt **161 rekord, 9 kategória szakmai lektorálásra vár**. Az alkalmazás
  ezt jelzi; a kvízeredmény nem igazolja a karateismeretek szakmai helyességét.
- 10 illusztráció van; ebből 3 feliratos, ezért képes kvízben nem használható.
  Képes kérdésekhez jelenleg csak egy kategóriában van elegendő használható kép.
  A hiányzó ábrák helyén magyarázó helyőrző látható.
- A kiválasztások és a folyamatban lévő menet mentett Android-állapotból állnak
  vissza; ez nem külön, tartós „félbehagyott kvíz folytatása” szolgáltatás.
- Nincs fiók, felhőmentés vagy szinkronizálás. Az alkalmazás eltávolítása vagy
  adatainak Android-beállításokbeli törlése a helyi eredményeket is törli.
- Az eszköztesztek egy API 37-es emulátort fednek le; minden gyártó és Android-verzió
  külön kézi ellenőrzése nem történt meg.
- A tudástár technika-adatlapján még nincs külön „eddig gyakoroltam” összegzés;
  a technikánkénti statisztika a Haladás képernyőről érhető el. Ez a korábban
  dokumentált külön hiány nem tartozott az öt lezárásra kijelölt feladathoz.

## Tartalom és átadás

Az [elfogadási ellenőrzés](docs/ACCEPTANCE.md) tételenként tartalmazza a bizonyítékokat
és korlátokat. Új tartalomhoz: [KNOWLEDGE_BASE.md](docs/KNOWLEDGE_BASE.md),
[kvízszabályok](docs/QUIZ_RULES.md).

Az éles tudástárhoz következő három teendő:

1. Kyokushin oktató ellenőrizze a japán és magyar neveket, leírásokat és kategóriákat;
   a javításokat rekordonként, forrással és lektori jóváhagyással rögzítsük.
2. Készüljenek jogtisztán használható, szakmailag ellenőrzött, felirat nélküli ábrák
   a hiányzó technikákhoz és a három feliratos kép helyett.
3. A lektorált csomagot validáljuk, majd oktatóval és tanulókkal próbáljuk ki;
   csak tényleges jóváhagyás alapján változzon a hitelesítési státusz.
