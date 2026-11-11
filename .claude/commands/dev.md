---
description: Kiveszi a következő fejlesztési feladatot a docs/backlog mappából, lefejleszti, teszteli, és sikeres esetben átteszi a docs/done mappába.
allowed-tools: Bash, PowerShell, Read, Write, Edit, Grep, Glob, Skill
---

Egy fejlesztési feladat végigvitele a `docs/backlog/` mappából a `docs/done/` mappába.

**Argumentum (opcionális):** `$ARGUMENTS` – ha konkrét task-azonosító (pl. `T-105`), azt a
feladatot vedd elő a kiválasztási szabály helyett. Egyébként hagyd üresen.

**Egy hívás = egy feladat.** Ne fuss előre a következőre, még ha gyorsan is ment.

---

## 1. A feladat kiválasztása

Ha `$ARGUMENTS` megad egy task-azonosítót, azt a fájlt vedd a `docs/backlog/`-ból, és ha
a függőségei nem teljesülnek, **jelezd és kérdezz rá**, mielőtt belekezdesz.

Egyébként determinista szabály:

1. Listázd a `docs/backlog/` `T-*.md` fájljait, **növekvő task-azonosító szerint**.
2. Olvasd ki mindegyik fejlécéből a `| Függ |` mezőt.
3. A soron következő az **első olyan feladat, amelynek minden függősége már a
   `docs/done/` mappában van** (vagy nincs függősége).
4. Ha nincs ilyen, **állj meg**: írd ki, melyik feladat melyik hiányzó függőségre vár.
   Ne kezdj bele egy feladatba a függősége nélkül.

Írd ki, melyik feladatot választottad, és miért éppen azt.

## 2. A feladat beolvasása és a kontextus

1. Olvasd be a kiválasztott task-fájlt **teljes egészében**: Cél, Teendők, Kész ha,
   Figyelj rá, Definition of Done.
2. Olvasd be a hivatkozott követelményeket a `docs/REQUIREMENTS.md`-ből (a fejléc
   `Követelmény` mezője), és a kapcsolódó user story elfogadási kritériumait a
   `docs/BACKLOG.md`-ből.
3. Olvasd be a feladathoz tartozó `docs/FUNCTIONAL_SPEC.md` szakaszt, ha UI-ról szól.
4. Töltsd be a releváns skillt:
   - tudástár, séma, import, validáció → `kyo-knowledge-base`
   - kvízmotor, distraktor, seed, scoring → `kyo-quiz-engine`
   - Compose, ViewModel, Room, Hilt, Gradle → `kyo-android-module`
   - karate tartalom, hitelesítés, illusztráció → `kyokushin-content`
   - build- vagy tesztkörnyezeti hiba → `kyo-build-env`
5. Nézd meg a fejlécben hivatkozott ADR-eket a `docs/adr/` alatt.

## 3. Fejlesztés

- A „Teendők" lista a munkadarab, nem szó szerinti recept: ha a kód valósága eltér, a
  specifikáció és az ADR-ek győznek, az eltérést pedig jelezd.
- Tartsd be a rétegszabályt: `core/model` és `core/quiz` Android-mentes; `feature/*` nem
  lát Room entitást, DAO-t, DTO-t; a repository domain modellt ad vissza.
- `Clock` és RNG injektálva – közvetlen `System.currentTimeMillis()`, `Instant.now()`,
  `Random.Default` üzleti kódban tilos.
- Minden felhasználói szöveg `strings.xml`-ben, magyarul.
- Tartalmat érintő munkánál: a hitelesítési státuszt kódból **soha nem emeljük**.
- Ne implementálj olyat, ami nincs a feladatban. Ha menet közben más hibát találsz,
  **jegyezd fel** (új task-javaslat), de ne javítsd ebben a körben.

## 4. Tesztek

1. Írj tesztet a feladathoz; a teszt neve hivatkozzon a lefedett `TR-nn` vagy `FR-nn`
   azonosítóra (pl. `quizGenerator_sameSeed_producesIdenticalPlan_TR02`).
2. Futtasd:
   ```
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```
   (Windows PowerShellben: `.\gradlew.bat …`, és állítsd be a `JAVA_HOME`-ot, ha kell:
   `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`.)
3. Bukó teszt esetén a **kódot** javítsd, ne a tesztet – kivéve, ha a teszt bizonyítottan
   hibás elvárást fogalmaz meg; akkor írd le, miért.
4. Ha a környezet nem engedi a futtatást (hiányzó JDK/SDK/emulátor): **ne állítsd, hogy
   zöld**. Rögzítsd a `docs/ENVIRONMENT.md`-ben és a `docs/TEST_REPORT.md`-ben
   „NEM FUTOTT – ok" sorként.

## 5. Ellenőrzés a Definition of Done ellen

Menj végig a task-fájl **„Kész, ha"** és **„Definition of Done"** listáján, pontonként.
Minden pontnál mondd meg, teljesül-e, és mivel igazolható.

**Ha bármelyik pont nem teljesül:** a feladat **marad a `docs/backlog/` mappában.**
Írd le, mi hiányzik és miért, majd állj meg. Ne mozgasd át félkészen.

## 6. Lezárás és áthelyezés

Csak akkor, ha minden DoD pont teljesül:

1. Egészítsd ki a task-fájlt egy lezáró szakasszal a végén:

   ```markdown
   ---

   ## Elkészült

   | | |
   |---|---|
   | Dátum | ÉÉÉÉ-HH-NN |
   | Commit | <rövid hash> |

   **Futtatott parancsok:**

   | Parancs | Exit | Eredmény |
   |---|---|---|
   | `./gradlew testDebugUnitTest` | 0 | <teszt/hiba szám> |
   | `./gradlew assembleDebug` | 0 | <APK vagy BUILD SUCCESSFUL> |

   **Megjegyzés:** <amit a következő fejlesztőnek tudnia kell: eltérés a tervtől,
   felfedezett probléma, javasolt új task – vagy „nincs">
   ```

2. Mozgasd a fájlt: `git mv docs/backlog/<fájl> docs/done/<fájl>`
   (a `git mv` megőrzi az előzményt).
3. Frissítsd a `docs/backlog/README.md` feladattáblázatát: vedd ki az elkészült sort.
4. Frissítsd a `docs/IMPLEMENTATION_PLAN.md` megfelelő fázissorát és a változásnaplót.
5. Írd be a `docs/TEST_REPORT.md`-be a tényleges futtatásokat.
6. Commitolj:
   ```
   feat(<modul>): <rövid angol leírás> [T-nnn]
   ```
   `test(...)`, `fix(...)`, `docs(...)` előtaggal, ha az a pontosabb.

## 7. Zárójelentés

Rövid, tényszerű összefoglaló magyarul:

- **Melyik feladat** készült el, és mi lett a tartalma.
- **Mely parancsok futottak** ténylegesen, milyen exit kóddal és eredménnyel.
- **Mi NEM futott**, és miért.
- **Mi következik**: a `docs/backlog/` következő elérhető feladata.
- Ha találtál menet közben problémát, amit nem javítottál: mit javasolsz új taskként.

Soha ne jelents zöld buildet vagy tesztet tényleges, nullás exit kódú futtatás nélkül.
