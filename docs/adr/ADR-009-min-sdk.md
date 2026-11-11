# ADR-009 – `minSdk = 26` (Android 8.0)

- **Állapot:** elfogadva (2026-11-11, felhasználói döntés)
- **Kontextus:** OQ-03. A választás egyszerre eszköztámogatási és technikai kérdés.

## Döntés

`minSdk = 26`, `targetSdk` és `compileSdk` a telepített Android SDK legfrissebb stabil
platformja (a konkrét számot ADR-008 rögzíti, az `sdkmanager --list` tényleges kimenete alapján).

## Indoklás

| Szempont | 24 (Android 7.0) | **26 (Android 8.0)** | 29 (Android 10) |
|---|---|---|---|
| `java.time` (`Instant`) natívan | nem – core library desugaring kell | **igen** | igen |
| Adaptív ikonok, értesítési csatornák | nem | **igen** | igen |
| Régebbi tartalék-/edzőtermi telefon | néhány % többlet | **gyakorlatilag a teljes aktív eszközpark** | régebbi eszközökön nem indul |
| Build- és tesztkomplexitás | magasabb (desugaring hibaosztályok) | **alacsony** | alacsony |

A döntő érv a `java.time`: a `QuizAttempt.answeredAt`, a `StudyReview.reviewedAt` és az
injektált `Clock` (ADR-007) mind `Instant`-ot használ. 26-tól ez natív, így nem kell
core library desugaringra támaszkodni, ami a Room `@TypeConverter`-ek és a tesztek egy
részében extra hibalehetőség lenne.

Az app offline, nincs hálózat, nincs modern platform-API igénye – a 29-re emelés tehát
semmit nem adna cserébe az elvesztett eszköztámogatásért.

## Mérlegelt alternatívák

- **24:** a néhány százaléknyi többlet-eszközért cserébe desugaring-függőség és nehezebben
  tesztelhető időkezelés. Egy edzőtermi tanulóappnál ez nem éri meg.
- **29:** nincs olyan funkciónk, ami ezt indokolná; feleslegesen zárna ki használható
  eszközöket.

## Következmények

- A `gradle/libs.versions.toml` és az `app/build.gradle.kts` `minSdk = 26`-ot rögzít.
- Nincs `coreLibraryDesugaring` függőség – egyszerűbb build.
- Az `Instant` ↔ `Long` (epoch milli) `@TypeConverter` natív `java.time` API-ra épül.
- Ha később mégis 24-re kellene menni (pl. konkrét felhasználói igény), a változás
  lokalizált: desugaring bekapcsolása + a `java.time` használat újraellenőrzése.
