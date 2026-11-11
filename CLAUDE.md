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
5. `docs/IMPLEMENTATION_PLAN.md` – fázisok, aktuális állás, következő lépés.
6. `docs/ENVIRONMENT.md` – a build környezet tényleges állapota, blokkolók.
7. `docs/adr/` – architekturális döntések (ADR-NNN).

## Vasszabályok
- **Nyelv:** minden felhasználói szöveg és dokumentáció magyar. A kód, azonosítók, commit
  üzenetek angolok. Technikaneveknél a romaji az eredeti forma, mellette magyar jelentés.
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
- Kis, tesztelhető lépések. Minden lépés: kód → unit teszt → build (ha a környezet engedi) →
  `docs/IMPLEMENTATION_PLAN.md` állásfrissítés → commit.
- Blokkoló esetén: pontosan dokumentáld a `docs/ENVIRONMENT.md`-ben, és folytasd azzal, ami
  elvégezhető (a kvízmotor és a validáció JVM-en, Android SDK nélkül is tesztelhető lenne,
  de Gradle futtatáshoz JDK kell – lásd ENVIRONMENT.md).
- Commit konvenció: `feat|fix|test|docs|chore(scope): rövid angol leírás`.

## Parancsok
`/kyo-status`, `/kyo-next`, `/kyo-setup-env`, `/kyo-build`, `/kyo-test`, `/kyo-validate-kb`,
`/kyo-doc-sync`. Részletek: `.claude/commands/`.

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
**Jelenlegi környezeti állapot: JDK, Android SDK és Gradle NINCS telepítve a gépen.**
Lásd `docs/ENVIRONMENT.md` – a kód írható, de build/teszt addig nem futtatható.
