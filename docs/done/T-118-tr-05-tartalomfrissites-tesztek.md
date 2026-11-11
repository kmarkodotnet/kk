# T-118 · TR-05: tartalomfrissítés tesztek

| | |
|---|---|
| Epik | **E1** – Fundamentum: domain modell, kvízmotor, tudástár, perzisztencia |
| User story | US-04 |
| Modul | `core/data` |
| Méret | L (6–12 ó) |
| Függ | [T-117](T-117-core-data-contentimporter.md) |
| Követelmény | FR-33, DR-06, DR-07, DR-08 |
| Teszt | TR-05 |

## Cél

A projekt egyik legfontosabb garanciája: a frissítés nem viszi el az előrehaladást.

## Teendők

- [ ] In-memory Room; v1 tartalom betöltése, próbálkozások rögzítése, v2 betöltése.
- [ ] Ellenőrzés: a próbálkozások száma és a számolt statisztika változatlan.
- [ ] Átnevezett technika: az `id` marad, az előrehaladás követi, az új név jelenik meg.
- [ ] Visszavont technika: `retired = true`, a próbálkozásai feloldhatók maradnak.
- [ ] Idempotencia: ugyanaz a verzió kétszer → nincs duplikáció.

## Kész, ha

- [ ] Minden fenti eset lefedve, `TR05` jelöléssel a tesztnevekben.
- [ ] `./gradlew testDebugUnitTest` exit 0.

## Figyelj rá

- Ez a teszt Robolectricet vagy in-memory Roomot igényel, tehát lassabb, mint a tiszta JVM tesztek – külön forráskészletbe is tehető.

## Definition of Done

A task-specifikus pontokon felül a **globális DoD** is érvényes (`docs/BACKLOG.md` → „Globális Definition of Done”):

- [ ] `./gradlew assembleDebug` exit 0
- [ ] `./gradlew testDebugUnitTest` exit 0, a teszt neve hivatkozik a lefedett követelményre
- [ ] Nem sért rétegszabályt (ADR-001, L1–L6)
- [ ] Nincs közvetlen `Clock` / `Random` hívás üzleti kódban (NFR-07)
- [ ] Minden felhasználói szöveg `strings.xml`-ben, magyarul
- [ ] UI esetén: `contentDescription`, ≥ 48 dp célpont, `@Preview` világos + sötét témára
- [ ] `docs/IMPLEMENTATION_PLAN.md` állása frissítve
- [ ] Ami nem futott le, az `docs/TEST_REPORT.md`-ben „NEM FUTOTT – ok” sorként szerepel

## Kapcsolódó dokumentumok

- `docs/REQUIREMENTS.md` – a hivatkozott követelmények
- `docs/FUNCTIONAL_SPEC.md` – a képernyő és a viselkedés leírása
- `docs/ARCHITECTURE.md` + `docs/adr/` – a szerkezeti döntések
- `.claude/skills/` – `kyo-quiz-engine`, `kyo-knowledge-base`, `kyo-android-module`, `kyokushin-content`

---

## Elkészült

| | |
|---|---|
| Dátum | 2026-11-11 |
| Commit | `6694f7d` |

**Futtatott parancsok:**

| Parancs | Exit | Eredmény |
|---|---|---|
| `.\gradlew.bat :core:data:testDebugUnitTest` | 0 | 78 teszt, 0 hiba (10 új `TR05` teszt) |
| `.\gradlew.bat testDebugUnitTest` | 0 | 340 teszt, 0 hiba, 0 kihagyott |
| `.\gradlew.bat assembleDebug` | 0 | BUILD SUCCESSFUL (`:app:assembleDebug` UP-TO-DATE – csak tesztkód változott) |

**Megjegyzés:** A TR-05 négy kért esete mind lefedve, `TR05` jelölésű tesztnevekkel, a
`core/data` meglévő `test` forráskészletében (külön forráskészlet nem kellett: a
Robolectric + in-memory Room futás 8 s alatt lefut). Termelési kód **nem változott** – a
T-117 szerkezeti FR-33 garanciája a viselkedés szintjén igazolódott.

A statisztika-változatlanságot a teszt saját `ProgressSnapshot`-ja méri (nyers sorok +
levezetett számok), mert a `ProgressRepository` aggregált lekérdezései a **T-501** tárgya.
Amikor a T-501 megírja őket, érdemes egy TR-05 tesztet a *valódi* lekérdezésekre is
ráhúzni – a pillanatkép számolása ugyanazt az adatot használja, tehát az eredménynek
egyeznie kell.

A csomagolt kiadások JSON-összeállítása a `TestKnowledgeAssetSources.kt`
`KnowledgeSourceFixtures` objektumába került (két tesztosztály használja); a T-117
`ContentImporterTest` tesztjei változatlanok, csak a privát segédei delegálnak.

A `ktlintCheck` cél **nem létezik** a projektben (`Task 'ktlintCheck' not found`, exit 1) –
a ktlint beállítása a T-607 feladata; a `docs/TEST_REPORT.md` „NEM FUTOTT" sora rögzíti.
