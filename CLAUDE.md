# CLAUDE.md – Kyokushin Karate Trainer

Ez a projekt egy **offline-first, magyar nyelvű Android alkalmazás** (Kotlin + Jetpack Compose),
amely Kyokushin karate technikaneveket tanít és kvízekkel gyakoroltat.

A projekt egyetlen hiteles forrása a mesterprompt: `CLAUDE_KYOKUSHIN_MASTER_PROMPT.md`.
Ha bármi ütközik, a mesterprompt győz. A terveket a `docs/` könyvtár tartalmazza.

## Dokumentumhierarchia (ebben a sorrendben olvasandó)
1. `CLAUDE_KYOKUSHIN_MASTER_PROMPT.md` – a megbízás, hatókör, elfogadási kritériumok.
2. `docs/REQUIREMENTS.md` – követelményspecifikáció (FR/NFR azonosítókkal).
3. `docs/FUNCTIONAL_SPEC.md` – funkcionális specifikáció (képernyők, folyamatok, állapotok).
4. `docs/ARCHITECTURE.md` – architektúra és modulterv.
5. `docs/CONVENTIONS.md` – **felhalmozott gyakorlati tudás**: konvenciók, ismert csapdák,
   nyitott ügyek. Minden feladat előtt olvasd el; ha újat tanulsz, írd hozzá.
6. `docs/BACKLOG.md` – epikek, user story-k, a globális DoR/DoD és a mérföldkövek.
7. `docs/backlog/` – a nyitott feladatok (+ `GROUPS.md`); `docs/done/` – az elkészültek.
8. `docs/IMPLEMENTATION_PLAN.md` – fázisok, aktuális állás, következő lépés.
9. `docs/ENVIRONMENT.md` – a build környezet tényleges állapota, blokkolók.
10. `docs/adr/` – architekturális döntések (ADR-NNN).

## Vasszabályok
- **Nyelv:** a dokumentáció magyar. A UI-kezelőszövegek és a technikatartalom (név,
  leírás, kategórianév) **lokalizálhatók tetszőleges számú nyelvre** (FR-43, ADR-013,
  2026-11-14, felhasználói döntés – ez feloldja a korábbi, kizárólag magyar
  vasszabályt és OQ-07-et). Az alapnyelv és a hiteles tartalomforrás továbbra is
  **magyar**: minden technikarekord `hungarian_name`/`description_hu` mezője és minden
  `strings.xml` alapértelmezett (qualifier nélküli) bejegyzése kötelezően magyar marad,
  és ha egy adott nyelvre nincs fordítás, az app **mindig** a magyar szövegre esik
  vissza – soha nem marad üres, és soha nem jelenik meg más nyelvű (pl. angol)
  alapértelmezésként. A kód, azonosítók, commit üzenetek angolok. Technikaneveknél a
  romaji az eredeti, nyelvfüggetlen forma, mellette a lokalizált jelentés.
- **Tartalmi integritás:** SOHA ne gyárts kitalált japán terminológiát, kitalált övfokot vagy
  nem ellenőrzött karate-illusztrációt hiteles tananyagként. Minden nem hitelesített rekord
  `verified = false`, `quizEligible = false`, és a UI-ban „Demó / ellenőrzésre vár" jelzést kap.
  Harmadik fél alkalmazásából kinyert kép vagy szöveg használata tilos.
- **Nincs hamis jelentés:** ne írd azt, hogy build vagy teszt futott, ha nem futott. A tényleges
  parancsokat és kimeneteket a `docs/TEST_REPORT.md` tartalmazza.
- **Nincs korai bővítés:** backend, fiók, szinkronizáció, fizetés, AI-chat, spaced repetition,
  animáció, hang – a mesterprompt 10. pontja szerint MOST nem implementálandó.
- **INTERNET permission nem kérhető** a manifestben.
- **Rétegszabály:** UI soha nem hivatkozik Room DAO-ra, entitásra vagy DTO-ra. A kvízmotor
  tiszta Kotlin, Android-függőség nélkül.
- **Verziók:** ne találj ki verziószámokat. Minden függőségverzió a `gradle/libs.versions.toml`
  version catalogban van, és ellenőrzött, egymással kompatibilis stabil kiadás.

## Munkamód (automatikus fejlesztés)

**Könyvfeldolgozási kiegészítés (2026-11-16, felhasználói döntés, E11):** a könyv és
a feldolgozás előtti Kyo-adatbázis egyező állításai nem igényelnek új szakmai lektorálást;
kisebb eltérésnél a könyv az irányadó, szintén új lektorálás nélkül; lényegi
ellentmondásnál célzott lektorálás szükséges. Ez az E11-ben felülírja a minden
könyvanyagra kiterjedő lektorálási feltételezést. Az elfogadás nem állítható be
kitalált szakértői ellenőrzésként. Részletek és az előzmény nélküli új tartalom kezelése:
[`docs/BOOK_CONTENT_POLICY.md`](docs/BOOK_CONTENT_POLICY.md).

- Kis, tesztelhető lépések. Minden lépés: kód → unit teszt → build (ha a környezet engedi) →
  `docs/IMPLEMENTATION_PLAN.md` állásfrissítés → commit.
- Blokkoló esetén: pontosan dokumentáld a `docs/ENVIRONMENT.md`-ben, és folytasd azzal, ami
  elvégezhető (a kvízmotor és a validáció JVM-en, Android SDK nélkül is tesztelhető lenne,
  de Gradle futtatáshoz JDK kell – lásd ENVIRONMENT.md).
- Commit konvenció: `feat|fix|test|docs|chore(scope): rövid angol leírás`.

## Parancsok
**`/dev`** – a fő fejlesztési hurok: kiveszi a `docs/backlog/` következő feladatát,
lefejleszti, teszteli, és sikeres Definition of Done esetén átmozgatja a `docs/done/`
mappába. Egy hívás = egy feladat.

**`/devall`** – ugyanez sorozatban: feladatonként **egy** subagentet indít, megvárja,
ellenőrzi az eredményt, majd indítja a következőt. Egyszerre mindig **pontosan egy**
subagent fut. Hiba, holtpont vagy eltérő jelentés esetén megáll, nem próbálkozik újra.
Korlátozható: `/devall 3`. A `docs/backlog/GROUPS.md` szerint egy menetben viszi azokat a
feladatokat, amelyek ugyanazon a fájlkészleten dolgoznak, és olcsóbb modellt választ a
kis, nem helyesség-kritikus feladatokhoz.

Továbbá: `/kyo-status`, `/kyo-next`, `/kyo-setup-env`, `/kyo-build`, `/kyo-test`,
`/kyo-validate-kb`, `/kyo-doc-sync`. Részletek: `.claude/commands/`.

## Skillek
`kyokushin-content`, `kyo-knowledge-base`, `kyo-quiz-engine`, `kyo-android-module`,
`kyo-build-env`. Részletek: `.claude/skills/`.

## Build célok
```
./gradlew testDebugUnitTest      # kötelező, zöldnek kell lennie
./gradlew assembleDebug          # debug APK: app/build/outputs/apk/debug/
./gradlew connectedDebugAndroidTest   # csak ha emulátor/eszköz elérhető
./gradlew lint ktlintCheck       # formázás és lint
```
**Jelenlegi környezeti állapot: JDK, Android SDK, Gradle és működő AVD rendelkezésre áll.**
A build, unit teszt és eszközteszt futtatható; részletek: `docs/ENVIRONMENT.md` és `docs/TEST_REPORT.md`.
